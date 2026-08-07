package Game.World.Core;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Player.Player;
import Game.World.Chunk.Chunk;
import Game.World.Chunk.ChunkStorage;
import Game.World.Entity.DynamicEntityRegistry;
import Game.World.Index.WorldSpatialIndex;
import Game.World.WorldObjects.WorldObjectsContainer;

/**
 * Mundo del juego — espacio global continuo.
 *
 * ── ETAPA 3: World como agregador global ──────────────────────────────────
 *
 * World ya no representa "el sector donde está el jugador".
 * World representa el UNIVERSO COMPLETO del juego:
 *
 *   World
 *   ├── ChunkStorage          → chunks cargados (contenido estático/persistente)
 *   ├── DynamicEntityRegistry → entidades dinámicas (Player, Enemy, Bullet…)
 *   └── WorldSpatialIndex     → índice espacial para consultas eficientes
 *
 * ── INVARIANTE FUNDAMENTAL ────────────────────────────────────────────────
 * Las entidades dinámicas NO pertenecen a ningún chunk.
 * Un Enemy puede moverse de Chunk(0,0) a Chunk(1,0) sin ser "transferido".
 * Un Bullet puede recorrer Chunk(0,0) → Chunk(1,0) → Chunk(2,0) sin congelarse.
 * La posición de cualquier entidad es siempre GLOBAL.
 *
 * ── RETROCOMPATIBILIDAD (SHIM) ────────────────────────────────────────────
 * La API vieja (add, remove, getObjectsContainer, update, getWidth, getHeight)
 * se mantiene para que WorldManager, TransitionSystem, SpawnSystem y
 * GameWorldBootstrap sigan compilando durante la migración.
 *
 * El shim de WorldObjectsContainer hace que getObjectsContainer() devuelva
 * una vista combinada de estáticos + dinámicos para que SceneRenderer,
 * TransitionDetector y EntityCountCondition sigan funcionando.
 *
 * ── ELIMINACIÓN PROGRESIVA ────────────────────────────────────────────────
 * En Etapas 4–9, cada llamada a la API deprecated se irá eliminando.
 * El WorldObjectsContainer embebido se eliminará en Etapa 9.
 *
 * ── COORDENADAS ───────────────────────────────────────────────────────────
 * World no tiene width/height propios en el nuevo modelo (el mundo es continuo).
 * Los campos width/height se mantienen como shim para compatibilidad con
 * WorldManager que los usa como "tamaño de chunk". Se eliminan en Etapa 9.
 */
public class World {

    // ── Dimensiones de chunk (shim — eliminar en Etapa 9) ────────────────────
    private int width;
    private int height;

    /**
     * Coordenada del sector inicial. En el nuevo modelo, World no está ligado
     * a un sector, pero se mantiene por compatibilidad con WorldCache.
     * Eliminar en Etapa 9 cuando WorldCache se reemplace por ChunkStorage.
     */
    private final WorldCoordinator coordinate;

    // ── Nuevas estructuras del mundo global ───────────────────────────────────

    private final ChunkStorage          chunkStorage;
    private final DynamicEntityRegistry dynamicRegistry;
    private final WorldSpatialIndex     spatialIndex;

    // ── Shim de compatibilidad (eliminar en Etapa 9) ─────────────────────────

    /**
     * Contenedor legacy. Recibe todas las llamadas add/remove antiguas
     * y las delega en dynamicRegistry para mantener el comportamiento actual.
     *
     * IMPORTANTE: SceneRenderer, TransitionDetector y EntityCountCondition
     * acceden a este contenedor vía getObjectsContainer(). El shim hace que
     * getObjects() devuelva la lista del DynamicEntityRegistry, de modo que
     * esos sistemas siguen leyendo las entidades correctas.
     *
     * WorldObjectsContainer.update() se mantiene activo — es la frontera de
     * simulación hasta que Etapa 4 la reemplace por SimulationRegion.
     */
    private final WorldObjectsContainer legacyContainer;

    /** Objeto cuya posición se expone al sistema de cámara. */
    private GameObjects trackTarget;

    // ── Constructores ─────────────────────────────────────────────────────────

    /**
     * Constructor completo — mundo con todas las estructuras nuevas.
     *
     * @param width      ancho del chunk (shim — eliminar en Etapa 9)
     * @param height     alto del chunk (shim — eliminar en Etapa 9)
     * @param coordinate coordenada del sector inicial (shim — eliminar en Etapa 9)
     */
    public World(int width, int height, WorldCoordinator coordinate) {
        this.width           = width;
        this.height          = height;
        this.coordinate      = coordinate;
        this.chunkStorage    = new ChunkStorage();
        this.dynamicRegistry = new DynamicEntityRegistry();
        this.spatialIndex    = new WorldSpatialIndex(width, height);
        this.legacyContainer = new WorldObjectsContainer();
    }

    // ── Registry externo inyectable (para el globalDynamicRegistry de WorldManager) ─

    /**
     * Registry externo inyectado por WorldManager.
     * Cuando está configurado, addDynamic() también registra la entidad aquí,
     * garantizando que el globalDynamicRegistry del WorldManager siempre
     * contenga las entidades añadidas a través de cualquier World.
     *
     * Esto resuelve el caso en que SpawnSystem o EnemySpawner llaman
     * world.addDynamic() pero el worldManager.globalDynamicRegistry no recibe
     * la entidad.
     */
    private DynamicEntityRegistry externalRegistry = null;

    /**
     * Inyecta el registry global de WorldManager.
     * Llamar desde WorldManager inmediatamente después de crear o recuperar
     * un World del cache.
     *
     * @param registry el globalDynamicRegistry del WorldManager
     */
    public void setExternalDynamicRegistry(DynamicEntityRegistry registry) {
        this.externalRegistry = registry;
    }

    // ── NUEVA API — entidades dinámicas ───────────────────────────────────────

    /**
     * Añade una entidad dinámica (Player, Enemy, Bullet, Drop, NPC…).
     * Registra en el DynamicEntityRegistry local Y en el registry externo
     * global si está configurado.
     *
     * @param entity la entidad dinámica a añadir
     */
    public void addDynamic(GameObjects entity) {
        dynamicRegistry.add(entity);
        // Notificar al globalDynamicRegistry de WorldManager si está inyectado
        if (externalRegistry != null) {
            externalRegistry.add(entity);
        }
        // El legacyContainer también la recibe para compatibilidad residual
        legacyContainer.add(entity);
    }

    /**
     * Elimina una entidad dinámica del mundo.
     *
     * @param entity la entidad a eliminar
     */
    public void removeDynamic(GameObjects entity) {
        dynamicRegistry.remove(entity);
        if (externalRegistry != null) {
            externalRegistry.remove(entity);
        }
        legacyContainer.remove(entity);
    }

    // ── NUEVA API — chunks estáticos ──────────────────────────────────────────

    /**
     * Añade un chunk cargado al mundo.
     * Sus objetos estáticos se indexan en WorldSpatialIndex.
     *
     * @param chunk el chunk completamente generado (loaded=true)
     */
    public void addChunk(Chunk chunk) {
        chunkStorage.put(chunk);
        spatialIndex.indexChunk(chunk);
        // Los objetos estáticos también van al legacyContainer para compatibilidad
        for (GameObjects obj : chunk.getObjects()) {
            legacyContainer.add(obj);
        }
    }

    /**
     * Descarga un chunk del mundo.
     * Sus objetos estáticos se eliminan del SpatialIndex.
     * Las entidades dinámicas NO se ven afectadas.
     *
     * @param coord coordenada del chunk a descargar
     */
    public void removeChunk(WorldCoordinator coord) {
        Chunk chunk = chunkStorage.get(coord);
        if (chunk != null) {
            spatialIndex.removeChunk(coord);
            for (GameObjects obj : chunk.getObjects()) {
                legacyContainer.remove(obj);
            }
        }
        chunkStorage.evict(coord);
    }

    // ── NUEVA API — acceso a estructuras ─────────────────────────────────────

    /**
     * El registro de entidades dinámicas del mundo.
     *
     * Si un registry externo global está configurado (inyectado por WorldManager),
     * devuelve ese registry global — garantiza que TransitionDetector y otros
     * sistemas legacy vean TODAS las entidades dinámicas del universo,
     * independientemente del sector activo.
     *
     * @return el registry global si está configurado, de lo contrario el local
     */
    public DynamicEntityRegistry getDynamicEntityRegistry() {
        return (externalRegistry != null) ? externalRegistry : dynamicRegistry;
    }

    /**
     * El almacén de chunks cargados.
     * Usar para verificar si un chunk está disponible, cargarlo o descargarlo.
     *
     * @return ChunkStorage del mundo
     */
    public ChunkStorage getChunkStorage() {
        return chunkStorage;
    }

    /**
     * El índice espacial del mundo.
     * Usar para consultas eficientes por región.
     *
     * @return WorldSpatialIndex del mundo
     */
    public WorldSpatialIndex getSpatialIndex() {
        return spatialIndex;
    }

    // ── Update (shim neutralizado — Etapa 4) ─────────────────────────────────

    /**
     * Ya no ejecuta simulación.
     *
     * WorldManager llama directamente a SimulationRegion + CollisionsSystem.
     * Este método se mantiene únicamente para que el compilador no rompa
     * código que aún tenga referencias a world.update().
     * Se eliminará en Etapa 9.
     *
     * @deprecated WorldManager ahora usa SimulationRegion directamente.
     */
    @Deprecated(forRemoval = true)
    public void update() {
        // Intencionalmente vacío — la simulación ocurre en WorldManager.update()
        // a través de SimulationRegion.
        dynamicRegistry.flush();
    }

    // ── API legacy de add/remove (shim — reemplazar en Etapas 4-9) ───────────

    /**
     * Añade un objeto al mundo.
     *
     * DISTINCIÓN IMPORTANTE:
     *   - Objetos ESTÁTICOS (terreno, obstáculos): usar addChunk(Chunk) o que las
     *     layers añadan al Chunk directamente. Este método NO debe recibir estáticos.
     *   - Objetos DINÁMICOS (Player, Enemy, Bullet): usar addDynamic() o este método.
     *
     * Durante la transición, este método delega en addDynamic() para entidades
     * dinámicas que aún llegan por la API legacy (EnemySpawner, SpawnSystem).
     * Los objetos estáticos llegan vía world.addChunk() desde WorldGenerator.
     *
     * @param obj el objeto a añadir
     */
    public void add(GameObjects obj) {
        addDynamic(obj);
    }

    /**
     * Elimina un objeto del mundo.
     *
     * @param obj el objeto a eliminar
     */
    public void remove(GameObjects obj) {
        removeDynamic(obj);
    }

    /**
     * Vista unificada de todos los objetos del mundo (estáticos + dinámicos).
     *
     * @deprecated Usar getDynamicEntityRegistry() para entidades dinámicas
     *             o getSpatialIndex().query(region) para consultas espaciales.
     *             Mantener para compatibilidad con SceneRenderer y sistemas legacy.
     *
     * @return el WorldObjectsContainer legacy
     */
    @Deprecated(forRemoval = true)
    public WorldObjectsContainer getObjectsContainer() {
        return legacyContainer;
    }

    // ── Tracking de cámara ────────────────────────────────────────────────────

    /**
     * Registra el objeto a rastrear para el sistema de cámara del Engine.
     *
     * Si el objeto es un Player, configura el objectUpdater del legacyContainer
     * para que los Enemy reciban EnemyContext correcto en cada update().
     *
     * @param obj objeto a seguir (generalmente el player); null para liberar.
     */
    public void setTrackTarget(GameObjects obj) {
        this.trackTarget = obj;

        if (obj instanceof Player player) {
            legacyContainer.setObjectUpdater(
                list -> WorldEnemyUpdater.updateAll(list, player)
            );
        } else if (obj == null) {
            legacyContainer.setObjectUpdater(list -> list.forEach(GameObjects::update));
        }
    }

    /**
     * Posición del objeto rastreado en coordenadas globales.
     *
     * @return posición del target, o null si no hay target
     */
    public Vector2D getTrackedPosition() {
        if (trackTarget == null) return null;
        var pos = trackTarget.getTransform().getPosition();
        return new Vector2D(pos.getX(), pos.getY());
    }

    /**
     * El objeto rastreado actualmente.
     *
     * @return el trackTarget, o null
     */
    public GameObjects getTrackTarget() {
        return trackTarget;
    }

    // ── Dimensiones (shim — eliminar en Etapa 9) ──────────────────────────────

    /**
     * Actualiza las dimensiones del chunk (shim para WorldManager).
     *
     * @deprecated En el nuevo modelo World no tiene dimensiones propias.
     */
    @Deprecated(forRemoval = true)
    public void resize(int newWidth, int newHeight) {
        this.width  = newWidth;
        this.height = newHeight;
    }

    /**
     * Ancho del chunk activo (shim para compatibilidad).
     *
     * @deprecated En el nuevo modelo las dimensiones pertenecen a cada Chunk.
     */
    @Deprecated(forRemoval = true)
    public int getWidth()  { return width;  }

    /**
     * Alto del chunk activo (shim para compatibilidad).
     *
     * @deprecated En el nuevo modelo las dimensiones pertenecen a cada Chunk.
     */
    @Deprecated(forRemoval = true)
    public int getHeight() { return height; }

    /**
     * Coordenada del sector (shim para compatibilidad con WorldCache).
     *
     * @deprecated Eliminar en Etapa 9 con WorldCache.
     */
    @Deprecated(forRemoval = true)
    public WorldCoordinator getCoordinate() { return coordinate; }
}
