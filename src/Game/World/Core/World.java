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
     * Inyectado en construcción. Representa la ÚNICA fuente de verdad para
     * entidades dinámicas en todo el universo. Todas las operaciones
     * add/remove/query van directamente a este registry.
     * 
     * No existen copias locales ni sincronización manual entre registries.
     *
     * HRFC — World Lifecycle Integrity:
     * El registry es final e inmutable tras construcción.
     * Un World válido es válido inmediatamente después de new World(...).
     */
    private final DynamicEntityRegistry globalDynamicRegistry;

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
     *
     * HRFC — World Lifecycle Integrity:
     * El globalDynamicRegistry se inyecta en construcción, no posteriormente.
     * Un World válido es válido inmediatamente después de su creación.
     * No existe estado "World parcialmente inicializado".
     *
     * @param width      ancho del chunk (shim — eliminar en Etapa 9)
     * @param height     alto del chunk (shim — eliminar en Etapa 9)
     * @param coordinate coordenada del sector inicial (shim — eliminar en Etapa 9)
     * @param globalDynamicRegistry el registry global del universo (singleton)
     */
    public World(int width, int height, WorldCoordinator coordinate,
                 DynamicEntityRegistry globalDynamicRegistry) {
        if (globalDynamicRegistry == null) {
            throw new IllegalArgumentException("globalDynamicRegistry no puede ser null");
        }
        this.width           = width;
        this.height          = height;
        this.coordinate      = coordinate;
        this.chunkStorage    = new ChunkStorage();
        this.spatialIndex    = new WorldSpatialIndex(width, height);
        this.globalDynamicRegistry = globalDynamicRegistry;
    }

    // ── Registry global — DEPRECATED ──────────────────────────────────────────

    /**
     * @deprecated ELIMINADO — El registry se inyecta en el constructor.
     *             Un World válido siempre tiene su registry configurado.
     *             No existe inicialización posterior.
     *
     * Este método se mantiene temporalmente para compatibilidad de compilación
     * pero lanza UnsupportedOperationException si se llama.
     *
     * @param registry ignorado
     * @throws UnsupportedOperationException siempre
     */
    @Deprecated(forRemoval = true)
    public void setGlobalDynamicRegistry(DynamicEntityRegistry registry) {
        throw new UnsupportedOperationException(
            "setGlobalDynamicRegistry() fue eliminado. " +
            "El registry se inyecta en el constructor de World."
        );
    }

    // ── NUEVA API — entidades dinámicas ───────────────────────────────────────

    /**
     * Añade una entidad dinámica (Player, Enemy, Bullet, Drop, NPC…).
     * Registra directamente en el globalDynamicRegistry (singleton del universo).
     *
     * @param entity la entidad dinámica a añadir
     */
    public void addDynamic(GameObjects entity) {
        // HRFC — El registry es final y siempre válido tras construcción.
        // No requiere validación.
        globalDynamicRegistry.add(entity);
    }

    /**
     * Elimina una entidad dinámica del mundo.
     * Elimina directamente del globalDynamicRegistry (singleton del universo).
     *
     * @param entity la entidad a eliminar
     */
    public void removeDynamic(GameObjects entity) {
        // HRFC — El registry es final y siempre válido tras construcción.
        // No requiere validación.
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
     * HRFC — World Lifecycle Integrity:
     * El registry es final y siempre válido. No puede ser null.
     *
     * @return el globalDynamicRegistry (singleton del universo)
     */
    public DynamicEntityRegistry getDynamicEntityRegistry() {
        // HRFC — El registry es final y siempre válido tras construcción.
        // No requiere validación.
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
