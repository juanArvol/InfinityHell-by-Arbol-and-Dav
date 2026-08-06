package Game.World.Core;

import Game.Engine.Camera.CameraController;
import Game.Engine.Camera.GameCamera;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Scene.SceneRenderer;
import Game.Engine.Systems.DebugSettings;
import Game.World.Generator.WorldGenerator;
import Game.World.Spawn.SpawnSystem;
import java.awt.Graphics2D;

/**
 * Orquestador del módulo World.
 *
 * ── HRFC: Separación de responsabilidades ────────────────────────────────
 *
 * WorldManager ANTES era responsable de:
 *   1. Gestión del mundo activo y caché
 *   2. Cámara (GameCamera, CameraController, snap, lerp)
 *   3. Render (SceneRenderer)
 *   4. Prewarming de vecinos (ExecutorService)
 *   5. Transiciones entre sectores
 *   6. SpawnSystem
 *
 * WorldManager AHORA es únicamente el ORQUESTADOR que coordina:
 *   - WorldCache          → caché de mundos generados
 *   - WorldGenerator      → generación de mundos
 *   - CameraSystem        → toda la lógica de cámara (extraída)
 *   - WorldPrewarmService → pre-generación de vecinos (extraída)
 *   - WorldTransitionService → transiciones entre sectores (ya existía)
 *   - SpawnSystem         → spawn de entidades
 *   - SceneRenderer       → render de la escena
 *
 * WorldManager no contiene lógica propia más allá de la coordinación del
 * ciclo update/draw y el mantenimiento de la coordenada activa.
 *
 * ── API PÚBLICA SIN CAMBIOS ───────────────────────────────────────────────
 * Toda la API existente (getCamera, setTrackedObject, draw, onVirtualResize,
 * setCameraController, getCurrentWorld, resize, shutdown) sigue funcionando.
 * GameState no necesita cambios.
 *
 * ── CAMERA_DELTA_TIME ─────────────────────────────────────────────────────
 * Ya no es una constante hardcodeada. Se calcula a partir del targetFps
 * inyectado en el constructor. Valor por defecto: 1/30 = 0.0333s.
 *
 * ── TRANSICIÓN DE SECTOR ─────────────────────────────────────────────────
 * Cuando WorldTransitionService reporta un cambio de sector activo,
 * WorldManager:
 *   1. Actualiza currentCoord.
 *   2. Registra el trackedObject en el nuevo mundo.
 *   3. Notifica a CameraSystem para hacer snap.
 *   4. Actualiza los WorldBounds de la cámara.
 *   5. Limpia el historial de colisiones del nuevo mundo.
 *
 * ── SPAWNSYSTEM ───────────────────────────────────────────────────────────
 * WorldManager posee el SpawnSystem y lo actualiza en cada tick.
 * El SpawnSystem usa currentWorldSupplier para acceder siempre al mundo
 * activo, eliminando el bug de "enemigos solo en pantalla inicial".
 */
public class WorldManager {

    /** Frames por segundo objetivo. Configurable en el constructor. */
    private final double targetFps;

    /** Delta de tiempo calculado: 1.0 / targetFps. */
    private final double cameraDeltaTime;

    // ── Colaboradores ─────────────────────────────────────────────────────

    private final WorldCache               cache;
    private final WorldGenerator           generator;
    private final WorldTransitionService   transitionService;
    private final SceneRenderer            renderer;
    private final CameraSystem             cameraSystem;
    private final WorldPrewarmService      prewarmService;
    private final SpawnSystem              spawnSystem;

    // ── Estado activo ─────────────────────────────────────────────────────

    private WorldCoordinator currentCoord;
    private int              logicalWidth;
    private int              logicalHeight;

    /** El objeto rastreado para prewarming y cámara. */
    private GameObjects trackedObject;

    // ── Construcción ──────────────────────────────────────────────────────

    /**
     * Constructor principal — todos los colaboradores inyectados.
     *
     * @param width         ancho lógico de cada sector
     * @param height        alto lógico de cada sector
     * @param virtualWidth  ancho virtual del juego (para GameCamera)
     * @param virtualHeight alto virtual del juego (para GameCamera)
     * @param generator     generador de mundos (inyectable para tests)
     * @param settings      interfaz DebugSettings del Engine
     * @param targetFps     frames por segundo objetivo del game loop
     */
    public WorldManager(int width, int height,
                        int virtualWidth, int virtualHeight,
                        WorldGenerator generator,
                        DebugSettings settings,
                        double targetFps) {
        this.logicalWidth    = width;
        this.logicalHeight   = height;
        this.generator       = generator;
        this.targetFps       = targetFps;
        this.cameraDeltaTime = 1.0 / targetFps;
        this.currentCoord    = new WorldCoordinator(0, 0);

        // Inicializar colaboradores
        this.cache      = new WorldCache();
        this.renderer   = new SceneRenderer(settings);

        // CameraSystem posee GameCamera — separado de WorldManager
        this.cameraSystem = new CameraSystem(virtualWidth, virtualHeight);
        this.cameraSystem.setWorldBounds(width, height);

        // WorldTransitionService: conectar con el mundo dinámico
        this.transitionService = new WorldTransitionService(
            cache, generator,
            Game.Engine.Events.GameEventBus.GLOBAL,
            this::getCurrentWorld
        );

        // WorldPrewarmService: proveedor de coordenada activa dinámica
        this.prewarmService = new WorldPrewarmService(
            cache, generator, () -> currentCoord
        );

        // SpawnSystem: usa el mundo activo dinámicamente (elimina bug de primer mundo)
        this.spawnSystem = new SpawnSystem(this::getCurrentWorld, cache);

        regenerateInitialWorld();
    }

    /**
     * Constructor de conveniencia con targetFps=30 y generador por defecto.
     */
    public WorldManager(int width, int height, DebugSettings settings) {
        this(width, height, width, height, new WorldGenerator(), settings, 30.0);
    }

    /**
     * Constructor de conveniencia con generador custom y targetFps=30.
     */
    public WorldManager(int width, int height,
                        int virtualWidth, int virtualHeight,
                        WorldGenerator generator,
                        DebugSettings settings) {
        this(width, height, virtualWidth, virtualHeight, generator, settings, 30.0);
    }

    // ── Acceso al mundo activo ────────────────────────────────────────────

    /**
     * Retorna el mundo del sector activo actual.
     * Si no existe en caché, lo genera y lo añade.
     */
    public World getCurrentWorld() {
        synchronized (cache) {
            if (!cache.contains(currentCoord)) {
                cache.put(generator.generate(logicalWidth, logicalHeight, currentCoord));
            }
            return cache.get(currentCoord);
        }
    }

    // ── SpawnSystem ───────────────────────────────────────────────────────

    /**
     * El SpawnSystem activo.
     * Registrar SpawnRequests aquí para spawn automático o manual.
     */
    public SpawnSystem getSpawnSystem() {
        return spawnSystem;
    }

    // ── TransitionService ─────────────────────────────────────────────────

    /**
     * El WorldTransitionService activo.
     * Permite configurar el WorldController predicate y registrar gates.
     *
     * Uso desde la capa de composición (GameWorldBootstrap):
     *   worldManager.getTransitionService()
     *       .setWorldControllerPredicate(obj -> obj instanceof Player);
     */
    public WorldTransitionService getTransitionService() {
        return transitionService;
    }

    // ── Cámara ────────────────────────────────────────────────────────────

    /**
     * La GameCamera del Engine.
     * UIBootstrap, CrossHairHUD y cualquier sistema que necesite la vista
     * acceden a ella a través de este método.
     */
    public GameCamera getCamera() {
        return cameraSystem.getCamera();
    }

    /**
     * El CameraSystem completo.
     * Usar para acceder a getTargets(), addTarget(), getModifiers(), etc.
     */
    public CameraSystem getCameraSystem() {
        return cameraSystem;
    }

    /**
     * Reemplaza el CameraController activo.
     * null desactiva el controlador (cámara estática).
     */
    public void setCameraController(CameraController controller) {
        cameraSystem.setCameraController(controller);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza todos los subsistemas del mundo en el orden correcto:
     *   1. Mundo activo (entidades, física, colisiones)
     *   2. SpawnSystem (evalúa requests, ejecuta spawns pendientes)
     *   3. CameraSystem (CameraController + commitFrame)
     *   4. WorldPrewarmService (pre-genera sectores vecinos en background)
     *   5. WorldTransitionService (detecta cruces, transfiere entidades)
     *   6. Post-transición (snap de cámara, actualizar bounds, limpiar historial)
     */
    public void update() {
        World world = getCurrentWorld();
        world.update();

        // SpawnSystem: usa getCurrentWorld() dinámicamente — no el primer mundo
        spawnSystem.update();

        // CameraSystem: actualiza el controller y consolida el frame
        cameraSystem.update(cameraDeltaTime);

        // Prewarming de sectores vecinos en background
        if (trackedObject != null) {
            prewarmService.update(trackedObject, logicalWidth, logicalHeight);
        }

        // Procesar transiciones entre sectores
        WorldCoordinator nextCoord = transitionService.processTransitions(
            world, currentCoord, logicalWidth, logicalHeight
        );

        // Post-transición: si el sector activo cambió
        if (nextCoord != null) {
            currentCoord = nextCoord;
            World nextWorld = getCurrentWorld();

            if (trackedObject != null) {
                // Registrar el tracked object en el nuevo mundo
                nextWorld.setTrackTarget(trackedObject);

                // Snap de cámara: evitar lerp largo al cruzar sector
                var pos = trackedObject.getTransform().getPosition();
                cameraSystem.onSectorChanged(new Game.Engine.GameMath.Logic2D.Vector2D(
                    pos.getX(), pos.getY()
                ));
                cameraSystem.setWorldBounds(logicalWidth, logicalHeight);
            }

            // Limpiar historial de colisiones: evitar enter/exit espurios
            nextWorld.getObjectsContainer().clearCollisionContactHistory();
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    /**
     * Dibuja el sector activo usando la cámara del Engine.
     */
    public void draw(Graphics2D g) {
        GameCamera camera = cameraSystem.getCamera();
        renderer.draw(getCurrentWorld(), camera, g,
                      camera.getVirtualWidth(), camera.getVirtualHeight());
    }

    // ── Tracking ──────────────────────────────────────────────────────────

    /**
     * Registra el objeto a rastrear para prewarming, cámara y tracking de mundo.
     *
     * Configura automáticamente el sistema de cámara para seguir el objeto.
     * Si ya existe un CameraController personalizado, no se reemplaza.
     *
     * @param obj el objeto a seguir (generalmente el player); puede ser null.
     */
    public void setTrackedObject(GameObjects obj) {
        this.trackedObject = obj;
        getCurrentWorld().setTrackTarget(obj);

        if (obj != null) {
            cameraSystem.setTrackedObject(obj);
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        this.logicalWidth  = newWidth;
        this.logicalHeight = newHeight;
        cameraSystem.setWorldBounds(newWidth, newHeight);
    }

    /**
     * Alias para compatibilidad con llamadas existentes desde GameState.
     */
    public void onVirtualResize(int newVirtualWidth, int newVirtualHeight) {
        resize(newVirtualWidth, newVirtualHeight);
        cameraSystem.onVirtualResize(newVirtualWidth, newVirtualHeight);
    }

    /**
     * Apaga el prewarm service y libera recursos.
     * Llamar al cerrar la aplicación.
     */
    public void shutdown() {
        prewarmService.shutdown();
    }

    // ── Privado ───────────────────────────────────────────────────────────

    private void regenerateInitialWorld() {
        synchronized (cache) {
            cache.clear();
            cache.put(generator.generate(logicalWidth, logicalHeight, currentCoord));
        }
    }
}
