package Game.World.Chunk;

import Game.Engine.GameObjects;
import Game.World.Core.WorldCoordinator;
import Game.World.Index.WorldSpatialIndex;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Sistema de bookkeeping de afiliación de chunk para entidades dinámicas.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * ChunkAffiliationSystem rastrea en qué chunk se encuentra actualmente
 * cada entidad dinámica según su posición global.
 *
 * Esta información es metadata de bookkeeping — NO afecta la simulación.
 * Se usa para:
 *   - Serialización / save-load (guardar el chunk al que pertenece un enemy)
 *   - WorldSpatialIndex (mantener el índice actualizado cuando entidades mueven)
 *   - Estadísticas / debug (distribución de entidades por chunk)
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 * - NO transfiere entidades entre Worlds.
 * - NO detiene la simulación al cruzar un chunk.
 * - NO genera chunks (eso es WorldPrewarmService).
 * - NO decide si un objeto se simula (eso es SimulationRegion).
 *
 * ── CRUZAR UN CHUNK = SOLO CAMBIO DE ÍNDICE ──────────────────────────────
 * Cuando una entidad pasa de X=1279 a X=1280 (cruce de chunk en ancho=1280):
 *
 *   entity.globalX = 1281
 *         ↓
 *   newChunk = GlobalChunkResolver.toChunk(1281, y, cW, cH) = (1, y/cH)
 *         ↓
 *   if (newChunk != oldChunk):
 *       affiliationMap.put(entity, newChunk)   ← solo esto
 *       spatialIndex.updateDynamic(entity, oldChunk, newChunk)
 *
 * La entidad sigue simulándose exactamente igual.
 * No hay ningún efecto en velocidad, IA, física o efectos activos.
 *
 * ── INTEGRACIÓN CON WorldSpatialIndex ────────────────────────────────────
 * ChunkAffiliationSystem notifica al WorldSpatialIndex cuando una entidad
 * cambia de chunk, para mantener el índice coherente con las posiciones
 * globales actuales.
 */
public final class ChunkAffiliationSystem {

    private final int chunkWidth;
    private final int chunkHeight;

    /**
     * Mapa entidad → chunk actual.
     * IdentityHashMap para comparación por referencia (no equals/hashCode).
     */
    private final IdentityHashMap<GameObjects, WorldCoordinator> affiliationMap =
        new IdentityHashMap<>();

    /**
     * @param chunkWidth  ancho de cada chunk en píxeles globales
     * @param chunkHeight alto de cada chunk en píxeles globales
     */
    public ChunkAffiliationSystem(int chunkWidth, int chunkHeight) {
        this.chunkWidth  = chunkWidth;
        this.chunkHeight = chunkHeight;
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza la afiliación de todas las entidades de la lista.
     * Notifica al índice espacial cuando alguna entidad cruza un límite de chunk.
     *
     * Debe llamarse una vez por tick, típicamente al final del paso de simulación
     * (después de que CollisionsSystem haya movido los objetos).
     *
     * @param entities     lista de entidades dinámicas activas
     * @param spatialIndex índice espacial a mantener sincronizado (puede ser null)
     */
    public void update(List<GameObjects> entities, WorldSpatialIndex spatialIndex) {
        for (GameObjects entity : entities) {
            var pos = entity.getTransform().getPosition();
            WorldCoordinator newChunk = GlobalChunkResolver.toChunk(
                pos.getX(), pos.getY(), chunkWidth, chunkHeight
            );

            WorldCoordinator oldChunk = affiliationMap.get(entity);

            if (!newChunk.equals(oldChunk)) {
                // La entidad cruzó un límite de chunk
                affiliationMap.put(entity, newChunk);

                // Notificar al índice espacial si está disponible
                if (spatialIndex != null) {
                    spatialIndex.updateDynamic(entity, oldChunk, newChunk);
                }
            }
        }
    }

    // ── Registro / desregistro explícito ──────────────────────────────────

    /**
     * Registra una entidad nueva (recién spawnada).
     * Calcula su afiliación inicial y la indexa.
     *
     * @param entity       la entidad a registrar
     * @param spatialIndex índice espacial (puede ser null)
     */
    public void register(GameObjects entity, WorldSpatialIndex spatialIndex) {
        var pos = entity.getTransform().getPosition();
        WorldCoordinator chunk = GlobalChunkResolver.toChunk(
            pos.getX(), pos.getY(), chunkWidth, chunkHeight
        );
        affiliationMap.put(entity, chunk);
        if (spatialIndex != null) {
            spatialIndex.updateDynamic(entity, null, chunk);
        }
    }

    /**
     * Desregistra una entidad destruida.
     * La elimina del mapa de afiliación y del índice espacial.
     *
     * @param entity       la entidad a desregistrar
     * @param spatialIndex índice espacial (puede ser null)
     */
    public void unregister(GameObjects entity, WorldSpatialIndex spatialIndex) {
        WorldCoordinator lastChunk = affiliationMap.remove(entity);
        if (spatialIndex != null && lastChunk != null) {
            spatialIndex.removeDynamic(entity, lastChunk);
        }
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Chunk actual de una entidad según su última posición registrada.
     *
     * @param entity la entidad a consultar
     * @return WorldCoordinator del chunk actual, o null si no está registrada
     */
    public WorldCoordinator getAffiliation(GameObjects entity) {
        return affiliationMap.get(entity);
    }

    /**
     * Número de entidades rastreadas.
     *
     * @return cantidad de entidades con afiliación registrada
     */
    public int trackedCount() {
        return affiliationMap.size();
    }

    /**
     * Limpia todas las afiliaciones. Llamar al reiniciar el mundo.
     */
    public void clear() {
        affiliationMap.clear();
    }
}
