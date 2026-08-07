package Game.World.Index;

import Game.Engine.GameObjects;
import Game.World.Chunk.Chunk;
import Game.World.Chunk.ChunkStorage;
import Game.World.Chunk.GlobalChunkResolver;
import Game.World.Core.WorldCoordinator;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Índice espacial del mundo global — evita recorrer todos los chunks
 * y todos sus objetos en cada consulta.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * WorldSpatialIndex permite a SimulationRegion, RenderRegion y otros sistemas
 * consultar eficientemente qué objetos están dentro de un área espacial.
 *
 * ── IMPLEMENTACIÓN ACTUAL: Grid por chunk ─────────────────────────────────
 * La implementación inicial usa un índice por chunk:
 *   Map<WorldCoordinator, Set<GameObjects>>
 *
 * Cada objeto estático se indexa en el chunk al que pertenece (determinado
 * por su posición global en el momento de la inserción).
 *
 * Las entidades dinámicas se reindexan en cada tick desde DynamicEntityRegistry
 * porque su posición cambia frame a frame.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * La interfaz expone query(Rectangle) y queryChunk(coord). Los consumidores
 * (SimulationRegion, RenderRegion) no conocen la implementación interna.
 * Reemplazar por spatial hash o quadtree es un cambio interno sin efecto
 * en los consumidores.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Registrar el contenido de un chunk recién cargado:
 *   index.indexChunk(chunk);
 *
 *   // Consultar objetos en una región:
 *   List<GameObjects> candidates = index.query(new Rectangle(100, 100, 1500, 800));
 *
 *   // Desregistrar un chunk descargado:
 *   index.removeChunk(coord);
 */
public final class WorldSpatialIndex {

    private final int chunkWidth;
    private final int chunkHeight;

    /**
     * Índice principal: coord → set de objetos cuya posición global
     * pertenece a ese chunk.
     *
     * Objetos estáticos: indexados una vez al cargar el chunk.
     * Objetos dinámicos: reindexados cada tick.
     */
    private final Map<WorldCoordinator, Set<GameObjects>> index = new HashMap<>();

    /**
     * @param chunkWidth  ancho de cada chunk en píxeles globales
     * @param chunkHeight alto de cada chunk en píxeles globales
     */
    public WorldSpatialIndex(int chunkWidth, int chunkHeight) {
        this.chunkWidth  = chunkWidth;
        this.chunkHeight = chunkHeight;
    }

    // ── Indexación de chunks estáticos ────────────────────────────────────

    /**
     * Indexa todos los objetos de un chunk recién cargado.
     * Llamar desde WorldManager cuando un Chunk es añadido al ChunkStorage.
     *
     * @param chunk el chunk a indexar
     */
    public void indexChunk(Chunk chunk) {
        Set<GameObjects> bucket = index.computeIfAbsent(
            chunk.getCoordinator(), k -> new HashSet<>()
        );
        bucket.addAll(chunk.getObjects());
    }

    /**
     * Elimina del índice todos los objetos de un chunk descargado.
     * Llamar desde WorldManager cuando un Chunk es evicted de ChunkStorage.
     *
     * @param coord la coordenada del chunk a desindexar
     */
    public void removeChunk(WorldCoordinator coord) {
        index.remove(coord);
    }

    // ── Indexación de entidades dinámicas ─────────────────────────────────

    /**
     * Registra o actualiza la posición indexada de una entidad dinámica.
     *
     * Si la entidad cambió de chunk (oldCoord != newCoord), la mueve
     * del bucket antiguo al nuevo.
     *
     * @param entity   la entidad a indexar
     * @param oldCoord chunk anterior (null si es la primera indexación)
     * @param newCoord chunk actual según la posición global de la entidad
     */
    public void updateDynamic(GameObjects entity,
                               WorldCoordinator oldCoord,
                               WorldCoordinator newCoord) {
        if (oldCoord != null && !oldCoord.equals(newCoord)) {
            Set<GameObjects> oldBucket = index.get(oldCoord);
            if (oldBucket != null) oldBucket.remove(entity);
        }

        index.computeIfAbsent(newCoord, k -> new HashSet<>()).add(entity);
    }

    /**
     * Elimina una entidad dinámica del índice (al ser destruida).
     *
     * @param entity   la entidad a eliminar
     * @param lastCoord el último chunk conocido de la entidad
     */
    public void removeDynamic(GameObjects entity, WorldCoordinator lastCoord) {
        if (lastCoord == null) return;
        Set<GameObjects> bucket = index.get(lastCoord);
        if (bucket != null) bucket.remove(entity);
    }

    // ── Consultas espaciales ──────────────────────────────────────────────

    /**
     * Retorna todos los objetos (estáticos + dinámicos) indexados cuyos
     * chunks intersectan con el rectángulo de consulta dado.
     *
     * La consulta opera a nivel de chunk: devuelve todos los objetos de
     * cada chunk que toca el rectángulo. El culling fino por posición de
     * objeto se hace en el consumidor (SimulationRegion, RenderRegion).
     *
     * @param region rectángulo de consulta en coordenadas globales
     * @return lista de candidatos (puede incluir objetos fuera del rect exacto)
     */
    public List<GameObjects> query(Rectangle region) {
        List<GameObjects> result = new ArrayList<>();

        int cx0 = GlobalChunkResolver.firstChunkIndex(region.getMinX(), chunkWidth);
        int cy0 = GlobalChunkResolver.firstChunkIndex(region.getMinY(), chunkHeight);
        int cx1 = GlobalChunkResolver.lastChunkIndex(region.getMaxX(), chunkWidth);
        int cy1 = GlobalChunkResolver.lastChunkIndex(region.getMaxY(), chunkHeight);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cy = cy0; cy <= cy1; cy++) {
                WorldCoordinator coord = new WorldCoordinator(cx, cy);
                Set<GameObjects> bucket = index.get(coord);
                if (bucket != null) result.addAll(bucket);
            }
        }

        return result;
    }

    /**
     * Retorna todos los objetos indexados en un chunk específico.
     * Retorna lista vacía si el chunk no está indexado.
     *
     * @param coord coordenada del chunk a consultar
     * @return objetos en ese chunk (copia defensiva)
     */
    public List<GameObjects> queryChunk(WorldCoordinator coord) {
        Set<GameObjects> bucket = index.get(coord);
        if (bucket == null || bucket.isEmpty()) return List.of();
        return new ArrayList<>(bucket);
    }

    /**
     * Retorna los chunks (coordenadas) que están actualmente indexados.
     *
     * @return set inmutable de WorldCoordinators con datos en el índice
     */
    public Set<WorldCoordinator> indexedChunks() {
        return Collections.unmodifiableSet(index.keySet());
    }

    /**
     * Reconstruye el índice completo desde un ChunkStorage.
     * Útil al inicializar o tras una limpieza total.
     *
     * @param storage el almacén de chunks a re-indexar
     */
    public void rebuildFrom(ChunkStorage storage) {
        index.clear();
        for (Chunk chunk : storage.allChunks()) {
            indexChunk(chunk);
        }
    }

    /** Elimina todos los datos del índice. */
    public void clear() {
        index.clear();
    }

    /** Número de buckets (chunks) actualmente indexados. */
    public int bucketCount() { return index.size(); }

    /**
     * Número total de entradas en el índice (para diagnóstico).
     *
     * @return suma de todos los objetos en todos los buckets
     */
    public int totalIndexedObjects() {
        return index.values().stream().mapToInt(Collection::size).sum();
    }
}
