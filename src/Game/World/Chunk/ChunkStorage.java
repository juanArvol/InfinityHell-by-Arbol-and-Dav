package Game.World.Chunk;

import Game.World.Core.WorldCoordinator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Almacén de chunks generados, indexado por WorldCoordinator.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * ChunkStorage es el repositorio de datos de chunks. Conoce qué chunks
 * están disponibles en memoria y los provee bajo demanda.
 *
 * NO decide qué chunks simular (eso es SimulationRegion).
 * NO decide qué chunks cargar (eso es StreamingRegion + WorldManager).
 * NO contiene lógica de gameplay.
 *
 * ── DIFERENCIA CON WorldCache ─────────────────────────────────────────────
 * WorldCache almacena World (chunk == escena con CollisionsSystem).
 * ChunkStorage almacena Chunk (contenedor pasivo — sin simulación).
 *
 * Durante la migración, WorldCache sigue existiendo para compatibilidad.
 * ChunkStorage es el reemplazo final.
 *
 * ── EVICTION POLICY ───────────────────────────────────────────────────────
 * Idéntica al mecanismo de WorldCache: EvictionPolicy inyectable.
 * null = sin límite (comportamiento por defecto).
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Los métodos de escritura (put, evict, clear) deben sincronizarse
 * externamente si se accede desde múltiples threads (WorldPrewarmService
 * escribe desde bgExecutor; WorldManager lee desde game loop).
 * Usar synchronized(storage) en el caller, igual que con WorldCache.
 */
public final class ChunkStorage {

    /**
     * Política de eviction inyectable.
     * null = sin eviction (ilimitado, retrocompatible).
     */
    public interface EvictionPolicy {
        /**
         * Decide qué coordenadas desalojar dado el estado actual del almacén.
         *
         * @param chunks vista de solo lectura de los chunks actuales
         * @return coordenadas a desalojar (puede ser vacío)
         */
        Collection<WorldCoordinator> selectForEviction(Map<WorldCoordinator, Chunk> chunks);
    }

    // ── Estado ────────────────────────────────────────────────────────────

    private final Map<WorldCoordinator, Chunk>  chunks     = new HashMap<>();
    private EvictionPolicy                       evictionPolicy = null;
    private BiConsumer<WorldCoordinator, Chunk>  onEvict        = null;

    // ── Configuración ─────────────────────────────────────────────────────

    /** Configura la política de eviction. null = sin eviction. */
    public void setEvictionPolicy(EvictionPolicy policy) {
        this.evictionPolicy = policy;
    }

    /**
     * Callback invocado cuando un chunk es desalojado del almacén.
     * Útil para serializar el chunk antes de descargarlo.
     *
     * @param callback recibe (coord, chunk) del chunk desalojado
     */
    public void setOnEvict(BiConsumer<WorldCoordinator, Chunk> callback) {
        this.onEvict = callback;
    }

    // ── Operaciones ───────────────────────────────────────────────────────

    /**
     * Almacena un chunk. Si ya existe uno con la misma coordenada, no lo
     * reemplaza (idempotente — la generación en background puede generar
     * el mismo chunk dos veces si hay una race condition).
     *
     * Después de añadir, evalúa la política de eviction.
     *
     * @param chunk el chunk a almacenar; debe tener su coordinator configurado
     */
    public void put(Chunk chunk) {
        chunks.putIfAbsent(chunk.getCoordinator(), chunk);

        if (evictionPolicy != null) {
            Collection<WorldCoordinator> toEvict =
                evictionPolicy.selectForEviction(Collections.unmodifiableMap(chunks));
            for (WorldCoordinator coord : toEvict) {
                evict(coord);
            }
        }
    }

    /**
     * Obtiene el chunk en la coordenada dada, o null si no está cargado.
     *
     * @param coord coordenada del chunk
     * @return el Chunk, o null si no existe en el almacén
     */
    public Chunk get(WorldCoordinator coord) {
        return chunks.get(coord);
    }

    /**
     * True si el chunk en la coordenada dada está cargado en memoria.
     *
     * @param coord coordenada a consultar
     * @return true si el chunk existe
     */
    public boolean contains(WorldCoordinator coord) {
        return chunks.containsKey(coord);
    }

    /**
     * Desaloja un chunk explícitamente, invocando el callback onEvict si existe.
     *
     * IMPORTANTE: desalojar un chunk solo afecta a su contenido estático.
     * Las entidades dinámicas (DynamicEntityRegistry) son independientes
     * y nunca se destruyen al desalojar un chunk.
     *
     * @param coord coordenada del chunk a desalojar
     */
    public void evict(WorldCoordinator coord) {
        Chunk evicted = chunks.remove(coord);
        if (evicted != null && onEvict != null) {
            onEvict.accept(coord, evicted);
        }
    }

    /**
     * Conjunto no modificable de las coordenadas actualmente cargadas.
     * Útil para StreamingRegion al calcular qué chunks cargar/descargar.
     *
     * @return set de coordenadas
     */
    public Set<WorldCoordinator> loadedCoords() {
        return Collections.unmodifiableSet(chunks.keySet());
    }

    /**
     * Colección no modificable de todos los chunks cargados.
     *
     * @return colección de Chunk
     */
    public Collection<Chunk> allChunks() {
        return Collections.unmodifiableCollection(chunks.values());
    }

    /**
     * Mapa no modificable coord → chunk.
     * Usado por EvictionPolicy para tomar decisiones.
     *
     * @return mapa inmutable
     */
    public Map<WorldCoordinator, Chunk> asMap() {
        return Collections.unmodifiableMap(chunks);
    }

    /**
     * Limpia todos los chunks, invocando onEvict para cada uno si está configurado.
     */
    public void clear() {
        if (onEvict != null) {
            chunks.forEach((coord, chunk) -> onEvict.accept(coord, chunk));
        }
        chunks.clear();
    }

    /** Número de chunks actualmente en memoria. */
    public int size() { return chunks.size(); }

    /** True si no hay ningún chunk cargado. */
    public boolean isEmpty() { return chunks.isEmpty(); }
}
