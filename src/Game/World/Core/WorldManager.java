package Game.World.Core;

import Game.Engine.Camera.CameraController;
import Game.Engine.Camera.GameCamera;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Scene.SceneRenderer;
import Game.Engine.Systems.CollisionsSystem;
import Game.Engine.Systems.DebugSettings;
import Game.Engine.Systems.StatusEffectSystem;
import Game.Player.Player;
import Game.World.Chunk.ChunkAffiliationSystem;
import Game.World.Entity.DynamicEntityRegistry;
import Game.World.Generator.WorldGenerator;
import Game.World.Region.SimulationRegion;
import Game.World.Spawn.SpawnSystem;
import Game.World.Systems.AISystem;
import java.awt.Graphics2D;
import java.util.List;

/**
 * Orquestador del módulo World — mundo continuo multi-chunk.
 *
 * ── CORRECCIÓN DEL BUG DEL PLAYER (post-refactorización) ──────────────────
 *
 * SÍNTOMA: al cruzar un chunk el Player desaparecía, perdía input, la cámara
 * dejaba de seguirlo y las balas dejaban de funcionar.
 *
 * CAUSA RAÍZ — triple:
 *
 *   1. TransitionDetector detectaba que el Player cruzó x >= worldWidth
 *      y calculaba newX = x - worldWidth (envolver al sector siguiente).
 *      executeTransfer() fue neutralizado en Etapa 8 y ya NO aplicaba esa
 *      posición. El Player seguía en x=1281 (global) — correcto arquitectónicamente.
 *      Pero el TransitionSystem devolvía nextCoord = (1,0), disparando el bug 2.
 *
 *   2. Tras detectar la transición, WorldManager cambiaba currentCoord a (1,0)
 *      y llamaba getCurrentWorld() que devolvía el World del sector (1,0).
 *      Ese World tenía su propio DynamicEntityRegistry VACÍO — el Player
 *      estaba registrado en el World del sector (0,0), que ya no era el
 *      "mundo activo". Todos los sistemas (SimulationRegion, SceneRenderer,
 *      TransitionDetector) leían el registry del nuevo World → Player ausente.
 *
 *   3. SceneRenderer.draw(getCurrentWorld(), ...) consultaba
 *      world.getDynamicEntityRegistry() del nuevo World → lista vacía.
 *      El Player no se renderizaba.
 *
 * CORRECCIÓN ARQUITECTÓNICA:
 *
 *   El DynamicEntityRegistry es un SINGLETON DEL UNIVERSO, no por-World.
 *   Vive aquí, en WorldManager. Todos los sistemas lo reciben directamente
 *   de WorldManager, no de getCurrentWorld().
 *
 *   World.getDynamicEntityRegistry() sigue existiendo como API de conveniencia
 *   pero devuelve el registry GLOBAL, no uno privado por sector.
 *
 *   TransitionDetector ahora recibe el registry global directamente para
 *   detectar entidades que cruzaron el borde de coordenadas locales.
 *
 *   Mientras el TransitionSystem legacy siga activo, el "cruce de sector"
 *   solo actualiza currentCoord (para que el prewarm sepa qué vecinos cargar)
 *   pero NO afecta qué entidades se simulan ni se renderizan — eso lo decide
 *   el globalDynamicRegistry, que es invariante ante cambios de sector.
 *
 * ── ORDEN DE UPDATE ────────────────────────────────────────────────────────
 * 
 *   1. globalDynamicRegistry.flush()   — aplicar pendingAdd/Remove
 *   2. SimulationRegion.rebuild()      — estáticos de chunks + todos los dinámicos
 *   3. AISystem.update()               — IA con contexto y deltaTime
 *   4. StatusEffectSystem.update()     — proyectar flags derivados
 *   5. Destroyable cleanup             — eliminar entidades muertas del registry
 *   6. CollisionsSystem.update()       — física, movimiento, colisiones
 *   7. ChunkAffiliationSystem.update() — bookkeeping (solo metadata)
 *   8. SpawnSystem.update()            — evaluar y ejecutar spawns
 *   9. CameraSystem.update()           — mover cámara
 *   10. WorldPrewarmService.update()   — pre-generar chunks vecinos
 *   11. TransitionService (legacy)     — solo actualiza currentCoord
 */
public class WorldManager {

    private final double targetFps;

    // ── Colaboradores ─────────────────────────────────────────────────────

    private final WorldCache               cache;
    private final WorldGenerator           generator;
    private final WorldTransitionService   transitionService;
    private final SceneRenderer            renderer;
    private final CameraSystem             cameraSystem;
    private final WorldPrewarmService      prewarmService;
    private final SpawnSystem              spawnSystem;

    // ── Sistemas de simulación ─────────────────────────────────────────────

    private final SimulationRegion         simulationRegion;
    private final AISystem                 aiSystem;
    private final CollisionsSystem         collisionsSystem;
    private final StatusEffectSystem       statusEffectSystem;
    private final ChunkAffiliationSystem   affiliationSystem;

    // ── HRFC — Profiling Infrastructure ───────────────────────────────────

    private final Game.Engine.Profiling.SubsystemTimer updateTimer = 
        new Game.Engine.Profiling.SubsystemTimer();
    private final Game.Engine.Profiling.SubsystemTimer simulationTimer = 
        new Game.Engine.Profiling.SubsystemTimer();
    private final Game.Engine.Profiling.SubsystemTimer renderTimer = 
        new Game.Engine.Profiling.SubsystemTimer();
    private long frameCounter = 0;

    /**
     * Registro global de entidades dinámicas del universo.
     *
     * SINGLETON DEL UNIVERSO — no pertenece a ningún sector.
     * Contiene Player, Enemy, Bullet y cualquier entidad dinámica viva.
     *
     * Invariante: este registry NO cambia cuando currentCoord cambia de sector.
     * Cruzar un chunk no modifica la lista de entidades dinámicas activas.
     */
    private final DynamicEntityRegistry    globalDynamicRegistry;

    // ── Bus de eventos ────────────────────────────────────────────────────
    private final Game.Engine.GameEventBus eventBus;

    // ── Estado activo ──────────────────────────────────────────────────────

    /**
     * Coordenada del sector activo — usado solo para:
     *   - WorldPrewarmService: saber qué vecinos pre-cargar
     *   - TransitionDetector: detectar cruce de borde local
     *   - WorldCache key para generar/recuperar World de vecinos
     *
     * NO determina qué entidades se simulan ni qué se renderiza.
     * Su cambio no afecta al globalDynamicRegistry.
     */
    private WorldCoordinator currentCoord;
    private int              logicalWidth;
    private int              logicalHeight;

    private Player      trackedPlayer;
    private GameObjects trackedObject;

    // ── Construcción ──────────────────────────────────────────────────────

    public WorldManager(int width, int height,
                        int virtualWidth, int virtualHeight,
                        WorldGenerator generator,
                        DebugSettings settings,
                        double targetFps) {
        this(width, height, virtualWidth, virtualHeight, generator, settings, targetFps,
             new Game.Engine.GameEventBus());
    }

    public WorldManager(int width, int height,
                        int virtualWidth, int virtualHeight,
                        WorldGenerator generator,
                        DebugSettings settings,
                        double targetFps,
                        Game.Engine.GameEventBus eventBus) {
        this.logicalWidth    = width;
        this.logicalHeight   = height;
        this.generator       = generator;
        this.targetFps       = targetFps;
        this.currentCoord    = new WorldCoordinator(0, 0);
        this.eventBus        = (eventBus != null) ? eventBus : new Game.Engine.GameEventBus();

        this.cache    = new WorldCache();
        this.renderer = new SceneRenderer(settings);

        this.cameraSystem = new CameraSystem(virtualWidth, virtualHeight);

        // Registry global — único para todo el universo, invariante ante
        // cambios de sector.
        // HRFC — World Lifecycle Integrity:
        // Debe inicializarse ANTES de TransitionService porque este lo necesita
        // para crear Worlds nuevos.
        this.globalDynamicRegistry = new DynamicEntityRegistry();

        this.transitionService = new WorldTransitionService(
            cache, generator,
            this.eventBus,
            this::getCurrentWorld,
            this.globalDynamicRegistry
        );

        double streamingRadius = Math.max(width, height) * 2.5;
        this.prewarmService = new WorldPrewarmService(
            cache, generator, width, height, streamingRadius
        );

        this.spawnSystem = new SpawnSystem(this::getCurrentWorld, cache);

        double simRadius = Math.max(width, height) * 1.5;
        this.simulationRegion   = new SimulationRegion(simRadius);
        this.aiSystem           = new AISystem();
        this.collisionsSystem   = new CollisionsSystem();
        this.statusEffectSystem = new StatusEffectSystem();
        this.affiliationSystem  = new ChunkAffiliationSystem(width, height);

        regenerateInitialWorld();
    }

    public WorldManager(int width, int height, DebugSettings settings) {
        this(width, height, width, height, new WorldGenerator(), settings, 30.0);
    }

    public WorldManager(int width, int height,
                        int virtualWidth, int virtualHeight,
                        WorldGenerator generator,
                        DebugSettings settings) {
        this(width, height, virtualWidth, virtualHeight, generator, settings, 30.0);
    }

    // ── Acceso al registry global ─────────────────────────────────────────

    /**
     * El registro global de entidades dinámicas del universo.
     * Usar para añadir Player, Enemy, Bullet, etc. al mundo.
     *
     * @return el DynamicEntityRegistry global (singleton del universo)
     */
    public DynamicEntityRegistry getGlobalDynamicRegistry() {
        return globalDynamicRegistry;
    }

    // ── Acceso al mundo activo (legacy — necesario para TransitionService) ─

    public World getCurrentWorld() {
        synchronized (cache) {
            if (!cache.contains(currentCoord)) {
                // HRFC — World Lifecycle Integrity:
                // WorldGenerator.generate() crea un World con registry temporal.
                // Inmediatamente reemplazamos el World completo con uno nuevo
                // que tiene el registry global correcto.
                World tempWorld = generateLegacyWorld(logicalWidth, logicalHeight, currentCoord);
                
                // Extraer el chunk generado del World temporal
                Game.World.Chunk.Chunk generatedChunk = null;
                for (Game.World.Chunk.Chunk chunk : tempWorld.getChunkStorage().allChunks()) {
                    generatedChunk = chunk;
                    break; // Solo hay un chunk en el World generado
                }
                
                // Crear un nuevo World con el registry global correcto
                World properWorld = new World(logicalWidth, logicalHeight, currentCoord, globalDynamicRegistry);
                if (generatedChunk != null) {
                    properWorld.addChunk(generatedChunk);
                }
                
                cache.put(properWorld);
                return properWorld;
            }
            
            // HRFC — World Lifecycle Integrity:
            // Los Worlds del cache ya tienen el registry correcto desde su creación.
            // NO intentar reinyectar — el registry es final.
            return cache.get(currentCoord);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Tick de simulación del mundo.
     *
     * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────
     *
     * DISTRIBUCIÓN TEMPORAL:
     *
     * Recibe deltaTime de GameState y lo propaga a todos los sistemas físicos:
     *   - CollisionsSystem: integración de física y colisiones
     *   - CameraSystem: movimiento de cámara
     *   - Otros sistemas temporales según se agreguen
     *
     * ── HRFC-DT-002 — Unified Temporal Context ───────────────────────────
     *
     * EVOLUCIÓN:
     *
     * El parámetro deltaTime (double) fue reemplazado por TemporalContext.
     * WorldManager extrae deltaTime del contexto y lo distribuye a sistemas
     * que aún usan double (transición gradual).
     *
     * CONTRATO:
     *   temporalContext representa el tiempo del simulation step.
     *   Todos los sistemas físicos deben usar este valor para integración temporal.
     *   Sistemas que no requieren tiempo continuo (SpawnSystem con ticks discretos,
     *   UI, etc.) pueden actualizar sin deltaTime hasta su migración.
     *
     * CADENA DE AUTORIDAD:
     *   GameLoop (calcula) → GameState (propaga) → WorldManager (distribuye)
     *     → CollisionsSystem → Physics2D (integra)
     *
     * PLAN DE MIGRACIÓN:
     *   - Fase 1 (actual): WorldManager recibe TemporalContext, extrae deltaTime
     *   - Fase 2 (futuro): Sistemas críticos reciben TemporalContext directamente
     *   - Fase 3 (ideal): Todos los sistemas temporales usan TemporalContext
     *
     * ── HRFC — Bottleneck Diagnosis ──────────────────────────────────────
     *
     * Instrumentado para capturar tiempos de subsistemas en cada frame.
     * Los tiempos se registran en ProfilingConfig si está habilitado.
     *
     * @param temporalContext contexto temporal del simulation step (autoridad única)
     */
    public void update(Main.TemporalContext temporalContext) {
        updateTimer.start();
        simulationTimer.start();

        // Extraer deltaTime del contexto temporal canónico
        double deltaTime = temporalContext.getDeltaTime();
        frameCounter++;

        // El World activo provee el externalRegistry para interoperabilidad legacy.
        World world = getCurrentWorld();

        // ── 1. Flush del registry global ──────────────────────────────────
        globalDynamicRegistry.flush();

        // ── 2. Centro de simulación ────────────────────────────────────────
        double centerX = 0, centerY = 0;
        if (trackedObject != null) {
            var pos = trackedObject.getTransform().getPosition();
            centerX = pos.getX();
            centerY = pos.getY();
        }

        // Construir el ChunkStorage compuesto UNA SOLA VEZ por tick.
        // Incluye los chunks de todos los sectores cargados en el WorldCache.
        // Esto permite que SimulationRegion y SceneRenderer accedan a estáticos
        // de chunks vecinos al borde del sector activo.
        Game.World.Chunk.ChunkStorage compositeStorage = buildCompositeChunkStorage();

        // ── 3. Reconstruir SimulationRegion ───────────────────────────────
        // Estáticos del ChunkStorage compuesto + TODOS los dinámicos globales.
        simulationRegion.rebuildFromStorage(
            centerX, centerY,
            logicalWidth, logicalHeight,
            compositeStorage,
            globalDynamicRegistry
        );

        List<GameObjects> activeObjects = simulationRegion.getActiveObjects();

        // ── 4. AISystem ────────────────────────────────────────────────────
        // Ejecuta comportamientos de IA con contexto y deltaTime
        aiSystem.update(activeObjects, trackedPlayer, deltaTime);

        // ── 5. StatusEffectSystem ──────────────────────────────────────────
        statusEffectSystem.update(activeObjects);

        // ── 6. Destroyable cleanup ─────────────────────────────────────────
        for (GameObjects obj : activeObjects) {
            if (obj instanceof Game.Engine.Destroyable d && d.isPendingDestruction()) {
                globalDynamicRegistry.remove(obj);
            }
        }
        globalDynamicRegistry.flush();

        // Reconstruir después del flush (mismo composite, ya calculado)
        simulationRegion.rebuildFromStorage(
            centerX, centerY,
            logicalWidth, logicalHeight,
            compositeStorage,
            globalDynamicRegistry
        );
        activeObjects = simulationRegion.getActiveObjects();

        // ── 6.5. HRFC — Off-Screen Tracking & Collision Culling Preparation ──
        // Actualizar off-screen trackers y marcar bullets para destrucción si
        // exceden su tiempo máximo fuera de cámara. Esto ocurre ANTES de
        // CollisionsSystem para que los bullets marcados como dead no generen
        // colisiones innecesarias.
        //
        // Esta fase es extremadamente rápida: solo itera bullets con tracker
        // configurado y hace un simple check de visibilidad.
        GameCamera activeCamera = cameraSystem.getCamera();
        for (GameObjects obj : activeObjects) {
            if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet bullet) {
                // Actualizar off-screen tracking si está configurado
                bullet.updateOffScreenTracking(activeCamera, deltaTime);
            }
        }

        // ── 7. CollisionsSystem ────────────────────────────────────────────
        // Mini-HRFC: Pasar deltaTime para integración temporal correcta
        // HRFC: Pasar camera para collision culling
        collisionsSystem.update(activeObjects, deltaTime, activeCamera);

        // ── 8. ChunkAffiliationSystem ──────────────────────────────────────
        affiliationSystem.update(globalDynamicRegistry.getAll(), world.getSpatialIndex());

        // ── 9. SpawnSystem ─────────────────────────────────────────────────
        spawnSystem.update(deltaTime);

        // ── 10. CameraSystem ───────────────────────────────────────────────
        cameraSystem.update(deltaTime);

        // ── 11. WorldPrewarmService ────────────────────────────────────────
        if (trackedObject != null) {
            prewarmService.update(trackedObject, logicalWidth, logicalHeight);
        }

        // ── 12. TransitionService (legacy) ─────────────────────────────────
        WorldCoordinator nextCoord = transitionService.processTransitions(
            world, currentCoord, logicalWidth, logicalHeight, deltaTime
        );

        if (nextCoord != null) {
            currentCoord = nextCoord;
            collisionsSystem.clearContactHistory();
        }

        // ── HRFC — Profiling Infrastructure ───────────────────────────────
        simulationTimer.stop();
        updateTimer.stop();

        // Registrar profile del frame si profiling está activo
        Game.Engine.Profiling.ProfilingConfig profilingConfig = 
            Game.Engine.Profiling.ProfilingConfig.getInstance();
        if (profilingConfig.isEnabled()) {
            Game.Engine.Profiling.FrameProfile profile = new Game.Engine.Profiling.FrameProfile();
            profile.frameNumber = frameCounter;
            profile.activeProjectiles = getActiveProjectileCount();
            profile.simulationMs = simulationTimer.getElapsedMs();
            profile.collisionMs = collisionsSystem.getLastCollisionTimeMs();
            // frameTimeMs se calculará sumando simulation + rendering después del draw()
            profilingConfig.recordFrame(profile);
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    /**
     * Renderiza el mundo usando el registry global y todos los chunks cargados.
     *
     * Construye un ChunkStorage compuesto con todos los chunks disponibles
     * en el WorldCache para que RenderRegion pueda mostrar chunks vecinos
     * cuando la cámara está cerca del borde.
     *
     * ── HRFC — Bottleneck Diagnosis ──────────────────────────────────────
     *
     * Instrumentado para medir tiempo de rendering en el frame profile.
     */
    public void draw(Graphics2D g) {
        renderTimer.start();

        GameCamera camera = cameraSystem.getCamera();

        // Construir ChunkStorage compuesto con todos los chunks del cache
        // para que el renderer vea estáticos de chunks vecinos al borde.
        Game.World.Chunk.ChunkStorage compositeStorage = buildCompositeChunkStorage();

        renderer.drawGlobal(
            compositeStorage,
            globalDynamicRegistry,
            camera, g,
            camera.getVirtualWidth(),
            camera.getVirtualHeight()
        );

        renderTimer.stop();

        // ── HRFC — Profiling Infrastructure ───────────────────────────────
        // Actualizar frameTimeMs en el profile más reciente
        Game.Engine.Profiling.ProfilingConfig profilingConfig = 
            Game.Engine.Profiling.ProfilingConfig.getInstance();
        if (profilingConfig.isEnabled()) {
            Game.Engine.Profiling.ProfileCollector collector = profilingConfig.getCollector();
            if (collector != null) {
                collector.updateLastFrameRenderTime(renderTimer.getElapsedMs());
            }
        }
    }

    /**
     * Construye un ChunkStorage temporal con los chunks de todos los Worlds
     * del WorldCache. Permite al renderer acceder a estáticos de todos los
     * sectores cargados simultáneamente.
     *
     * Esto es necesario porque cada World del WorldCache tiene su propio
     * ChunkStorage, pero RenderRegion necesita consultar chunks vecinos.
     */
    private Game.World.Chunk.ChunkStorage buildCompositeChunkStorage() {
        Game.World.Chunk.ChunkStorage composite = new Game.World.Chunk.ChunkStorage();
        synchronized (cache) {
            for (World w : cache.getAllWorlds()) {
                for (Game.World.Chunk.Chunk chunk : w.getChunkStorage().allChunks()) {
                    composite.put(chunk);
                }
            }
        }
        return composite;
    }

    // ── Tracking ──────────────────────────────────────────────────────────

    public void setTrackedObject(GameObjects obj) {
        this.trackedObject = obj;
        if (obj instanceof Player p) {
            this.trackedPlayer = p;
        }
        if (obj != null) {
            cameraSystem.setTrackedObject(obj);
        }
    }

    // ── API de conveniencia para bootstrap ────────────────────────────────

    /**
     * Añade una entidad dinámica al registry global del universo.
     * Equivalente a getGlobalDynamicRegistry().add(entity).
     *
     * Usar en bootstrap y en código de gameplay para registrar Player,
     * Enemy, Bullet, etc. sin importar en qué sector están.
     *
     * @param entity la entidad dinámica a añadir
     */
    public void addDynamic(GameObjects entity) {
        globalDynamicRegistry.add(entity);
    }

    // ── API de acceso ─────────────────────────────────────────────────────

    public SpawnSystem            getSpawnSystem()       { return spawnSystem;              }
    public WorldTransitionService getTransitionService() { return transitionService;        }
    public GameCamera             getCamera()            { return cameraSystem.getCamera(); }
    public CameraSystem           getCameraSystem()      { return cameraSystem;             }
    public SimulationRegion       getSimulationRegion()  { return simulationRegion;         }

    /**
     * Bus de eventos del mundo. Usar para registrar listeners y emitir eventos
     * sin necesidad de un bus global estático.
     */
    public Game.Engine.GameEventBus getEventBus() { return eventBus; }

    // ── HRFC — Profiling Infrastructure ───────────────────────────────────

    /**
     * Retorna el número de proyectiles activos actualmente en el mundo.
     * Para diagnóstico de cuellos de botella.
     */
    private int getActiveProjectileCount() {
        int count = 0;
        for (GameObjects obj : globalDynamicRegistry.getAll()) {
            if (obj instanceof Game.Items.Types.Bullets.Definition.Bullet) {
                count++;
            }
        }
        return count;
    }

    /**
     * Retorna el tiempo de rendering del último frame en milisegundos.
     */
    public double getLastRenderTimeMs() {
        return renderTimer.getElapsedMs();
    }

    /**
     * Retorna el tiempo de simulation del último frame en milisegundos.
     */
    public double getLastSimulationTimeMs() {
        return simulationTimer.getElapsedMs();
    }

    public void setCameraController(CameraController controller) {
        cameraSystem.setCameraController(controller);
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        this.logicalWidth  = newWidth;
        this.logicalHeight = newHeight;
    }

    public void onVirtualResize(int newVirtualWidth, int newVirtualHeight) {
        resize(newVirtualWidth, newVirtualHeight);
        cameraSystem.onVirtualResize(newVirtualWidth, newVirtualHeight);
    }

    public void shutdown() {
        prewarmService.shutdown();
    }

    // ── Privado ───────────────────────────────────────────────────────────

    private void regenerateInitialWorld() {
        synchronized (cache) {
            cache.clear();
            // HRFC — World Lifecycle Integrity:
            // El mundo inicial también debe crearse con el registry global correcto.
            World tempWorld = generateLegacyWorld(logicalWidth, logicalHeight, currentCoord);
            
            // Extraer el chunk generado
            Game.World.Chunk.Chunk generatedChunk = null;
            for (Game.World.Chunk.Chunk chunk : tempWorld.getChunkStorage().allChunks()) {
                generatedChunk = chunk;
                break;
            }
            
            // Crear World con el registry global correcto
            World properWorld = new World(logicalWidth, logicalHeight, currentCoord, globalDynamicRegistry);
            if (generatedChunk != null) {
                properWorld.addChunk(generatedChunk);
            }
            
            cache.put(properWorld);
        }
    }

    @SuppressWarnings({"deprecation", "removal"})
    private World generateLegacyWorld(int w, int h, WorldCoordinator coord) {
        return generator.generate(w, h, coord);
    }
}
