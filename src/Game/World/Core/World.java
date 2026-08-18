package Game.World.Core;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.World.Chunk.Chunk;
import Game.World.Chunk.ChunkStorage;
import Game.World.Entity.DynamicEntityRegistry;
import Game.World.Index.WorldSpatialIndex;

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
 * ── ACCESO A ENTIDADES ────────────────────────────────────────────────────
 * Los sistemas acceden directamente a las estructuras de almacenamiento:
 *   - globalDynamicRegistry para entidades dinámicas (Player, Enemy, Bullet)
 *   - ChunkStorage + SpatialIndex para objetos estáticos de terreno
 *
 * La simulación de IA ocurre en AISystem (sistema explícito en WorldManager).
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
    private final WorldSpatialIndex     spatialIndex;

    /**
     * Registry global de entidades dinámicas del universo.
     * 
     * Inyectado por WorldManager. Representa la ÚNICA fuente de verdad para
     * entidades dinámicas en todo el universo. Todas las operaciones
     * add/remove/query van directamente a este registry.
     * 
     * No existen copias locales ni sincronización manual entre registries.
     */
    private DynamicEntityRegistry globalDynamicRegistry = null;

    // ── Shim de compatibilidad (eliminar en Etapa 9) ─────────────────────────

    /**
     * El shim de compatibilidad legacy fue eliminado. Todos los sistemas
     * ahora acceden directamente al globalDynamicRegistry o al ChunkStorage.
     */

    /** Objeto cuya posición se expone al sistema de cámara. */
    private GameObjects trackTarget;

    // ── Constructores ─────────────────────────────────────────────────────────

    /**
     * Constructor completo — mundo con todas las estructuras nuevas.
     * El globalDynamicRegistry debe ser inyectado por WorldManager después
     * de la construcción.
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
        this.spatialIndex    = new WorldSpatialIndex(width, height);
        // globalDynamicRegistry se inyecta después vía setGlobalDynamicRegistry()
    }

    // ── Registry global — inyectado por WorldManager ─────────────────────────

    /**
     * Inyecta el registry global de WorldManager.
     * Establecido una única vez por WorldManager después de crear o recuperar
     * un World del cache. Todas las operaciones de entidades dinámicas van
     * directamente a este registry.
     *
     * @param registry el globalDynamicRegistry del WorldManager (singleton del universo)
     */
    public void setGlobalDynamicRegistry(DynamicEntityRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("globalDynamicRegistry no puede ser null");
        }
        this.globalDynamicRegistry = registry;
    }

    // ── NUEVA API — entidades dinámicas ───────────────────────────────────────

    /**
     * Añade una entidad dinámica (Player, Enemy, Bullet, Drop, NPC…).
     * Registra directamente en el globalDynamicRegistry (singleton del universo).
     *
     * @param entity la entidad dinámica a añadir
     */
    public void addDynamic(GameObjects entity) {
        if (globalDynamicRegistry == null) {
            throw new IllegalStateException(
                "globalDynamicRegistry no configurado. Llamar setGlobalDynamicRegistry() primero.");
        }
        globalDynamicRegistry.add(entity);
    }

    /**
     * Elimina una entidad dinámica del mundo.
     * Elimina directamente del globalDynamicRegistry (singleton del universo).
     *
     * @param entity la entidad a eliminar
     */
    public void removeDynamic(GameObjects entity) {
        if (globalDynamicRegistry == null) {
            throw new IllegalStateException(
                "globalDynamicRegistry no configurado. Llamar setGlobalDynamicRegistry() primero.");
        }
        globalDynamicRegistry.remove(entity);
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
            chunkStorage.evict(coord);
        }
    }

    // ── NUEVA API — acceso a estructuras ─────────────────────────────────────

    /**
     * El registro de entidades dinámicas del mundo.
     * Devuelve directamente el globalDynamicRegistry (singleton del universo).
     * 
     * Todos los consumidores (TransitionDetector, EntityCountCondition,
     * SceneRenderer) reciben la misma vista: el registro global completo.
     *
     * @return el globalDynamicRegistry (singleton del universo)
     * @throws IllegalStateException si no fue configurado vía setGlobalDynamicRegistry()
     */
    public DynamicEntityRegistry getDynamicEntityRegistry() {
        if (globalDynamicRegistry == null) {
            throw new IllegalStateException(
                "globalDynamicRegistry no configurado. Llamar setGlobalDynamicRegistry() primero.");
        }
        return globalDynamicRegistry;
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

    // ── ELIMINADO: Legacy update() ───────────────────────────────────────────

    /**
     * El método update() fue eliminado. La simulación ocurre en
     * WorldManager.update() a través de sistemas explícitos:
     * AISystem, CollisionsSystem, StatusEffectSystem, etc.
     * 
     * World ya no tiene lógica de update propia.
     */

    // ── ELIMINADO: Legacy add/remove() ───────────────────────────────────────

    /**
     * Los métodos add(GameObjects) y remove(GameObjects) fueron eliminados.
     * 
     * Usar directamente:
     *   - addDynamic(obj) para entidades dinámicas
     *   - addChunk(chunk) para chunks con estáticos
     */

    // ── ELIMINADO: getObjectsContainer() ─────────────────────────────────────

    /**
     * El método getObjectsContainer() fue eliminado junto con
     * WorldObjectsContainer. Los sistemas usan:
     *   - getDynamicEntityRegistry() para dinámicos
     *   - getChunkStorage() + getSpatialIndex() para estáticos
     */

    // ── Tracking de cámara ────────────────────────────────────────────────────

    /**
     * Registra el objeto a rastrear para el sistema de cámara del Engine.
     * Solo mantiene la referencia al trackTarget para CameraSystem.
     *
     * @param obj objeto a seguir (generalmente el player); null para liberar.
     */
    public void setTrackTarget(GameObjects obj) {
        this.trackTarget = obj;
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

    // ── ELIMINADO: Dimension APIs ────────────────────────────────────────────

    /**
     * Los métodos resize(int, int), getWidth(), getHeight() fueron eliminados.
     * 
     * En el modelo de mundo infinito basado en chunks, World no tiene dimensiones propias.
     * 
     * Para obtener dimensiones:
     *   - Chunks individuales: chunk.getWidth() / chunk.getHeight()
     *   - Región de simulación: SimulationRegion bounds
     *   - Viewport: CameraSystem viewport
     * 
     * El campo width/height se mantiene internamente solo para compatibilidad con
     * WorldSpatialIndex constructor. Serán eliminados cuando WorldSpatialIndex
     * se refactorice para no requerir dimensiones fijas.
     */

    /**
     * Coordenada del sector (shim para compatibilidad con WorldCache).
     *
     * @deprecated Eliminar en Etapa 9 con WorldCache.
     */
    @Deprecated(forRemoval = true)
    public WorldCoordinator getCoordinate() { return coordinate;  }
}
