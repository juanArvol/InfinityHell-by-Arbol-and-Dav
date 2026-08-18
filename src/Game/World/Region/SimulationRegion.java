package Game.World.Region;

import Game.Engine.GameObjects;
import Game.World.Chunk.Chunk;
import Game.World.Chunk.ChunkStorage;
import Game.World.Chunk.GlobalChunkResolver;
import Game.World.Core.WorldCoordinator;
import Game.World.Entity.DynamicEntityRegistry;
import Game.World.Index.WorldSpatialIndex;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Región de simulación activa — determina qué objetos se simulan cada frame.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * SimulationRegion define la región del espacio global en la que los objetos
 * (estáticos y dinámicos) reciben update de física, colisiones, IA y efectos
 * de estado.
 *
 * Los objetos fuera de SimulationRegion se suspenden — no se actualizan,
 * pero conservan su estado (posición, velocidad, AI state, etc.).
 *
 * ── INDEPENDENCIA DE CHUNKS ───────────────────────────────────────────────
 * SimulationRegion NO es una colección de chunks.
 * Es un rectángulo espacial en coordenadas globales. Puede abarcar partes
 * de múltiples chunks o ninguno completo.
 *
 *   +---------+---------+
 *   |  Chunk  |  Chunk  |
 *   |  (0,0)  |  (1,0)  |
 *   |    +----|----+     |
 *   |    | SimReg  |     |
 *   |    |         |     |
 *   +---------+---------+
 *
 * ── COMPOSICIÓN DE LA LISTA ACTIVA ────────────────────────────────────────
 * La lista activeObjects contiene:
 *   1. Objetos estáticos de los chunks que intersectan con la región
 *      (consultados vía WorldSpatialIndex)
 *   2. Todas las entidades dinámicas (DynamicEntityRegistry) — las entidades
 *      dinámicas se filtran opcionalmente por proximidad
 *
 * ── RADIO CONFIGURABLE ────────────────────────────────────────────────────
 * simulationRadius determina el tamaño de la región (en píxeles globales).
 * Valor recomendado: 1.5 × chunkWidth para capturar el chunk del player
 * y la mitad de los 4 vecinos inmediatos.
 *
 * ── USO EN WorldManager ───────────────────────────────────────────────────
 *   // Al inicio de cada tick:
 *   simulationRegion.rebuild(playerX, playerY, spatialIndex, dynamicRegistry);
 *
 *   // Pasar a sistemas de simulación:
 *   collisionsSystem.update(simulationRegion.getActiveObjects());
 *   aiSystem.update(simulationRegion.getActiveObjects(), player, deltaTime);
 */
public final class SimulationRegion {

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Radio de simulación en píxeles globales.
     * La región activa es un rectángulo de (2*radius × 2*radius) centrado
     * en la posición del sujeto de seguimiento.
     */
    private double simulationRadius;

    // ── Estado del frame actual ───────────────────────────────────────────

    /** Bounds actuales de la región en coords globales. */
    private Rectangle currentBounds = new Rectangle(0, 0, 0, 0);

    /** Lista unificada de objetos activos para este frame. */
    private final List<GameObjects> activeObjects = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param simulationRadius radio de simulación en píxeles globales.
     *                         Valor típico: 1.5 × chunkWidth.
     */
    public SimulationRegion(double simulationRadius) {
        this.simulationRadius = simulationRadius;
    }

    // ── Configuración en runtime ──────────────────────────────────────────

    /**
     * Actualiza el radio de simulación.
     *
     * @param radius nuevo radio en píxeles globales
     */
    public void setSimulationRadius(double radius) {
        this.simulationRadius = Math.max(1, radius);
    }

    public double getSimulationRadius() {
        return simulationRadius;
    }

    // ── Reconstrucción por frame ──────────────────────────────────────────

    /**
     * Reconstruye la lista de objetos activos para el frame actual.
     *
     * Debe llamarse al inicio de cada tick, antes de que CollisionsSystem
     * y otros sistemas consuman getActiveObjects().
     *
     * @param centerX     posición X del centro de la región (player o cámara)
     * @param centerY     posición Y del centro de la región
     * @param spatialIndex índice espacial del mundo (para objetos estáticos)
     * @param dynamics    registro de entidades dinámicas
     */
    public void rebuild(double centerX, double centerY,
                        WorldSpatialIndex spatialIndex,
                        DynamicEntityRegistry dynamics) {
        activeObjects.clear();

        // Calcular bounds de la región
        int left   = (int)(centerX - simulationRadius);
        int top    = (int)(centerY - simulationRadius);
        int width  = (int)(simulationRadius * 2);
        int height = (int)(simulationRadius * 2);
        currentBounds = new Rectangle(left, top, width, height);

        // 1. Objetos estáticos de los chunks dentro de la región
        List<GameObjects> staticCandidates = spatialIndex.query(currentBounds);
        activeObjects.addAll(staticCandidates);

        // 2. Entidades dinámicas — todas están activas mientras estén vivas
        // (las entidades dinámicas no se filtran por posición en esta etapa;
        // el filtrado fino por posición puede añadirse posteriormente para
        // suspender entidades muy lejanas)
        activeObjects.addAll(dynamics.getAll());
    }

    /**
     * Versión alternativa que toma los objetos estáticos directamente de
     * ChunkStorage (sin WorldSpatialIndex). Útil durante la fase de transición
     * cuando el índice espacial aún no está completamente integrado.
     *
     * @param centerX     posición X del centro de la región
     * @param centerY     posición Y del centro de la región
     * @param chunkWidth  ancho de cada chunk
     * @param chunkHeight alto de cada chunk
     * @param storage     almacén de chunks cargados
     * @param dynamics    registro de entidades dinámicas
     */
    public void rebuildFromStorage(double centerX, double centerY,
                                   int chunkWidth, int chunkHeight,
                                   ChunkStorage storage,
                                   DynamicEntityRegistry dynamics) {
        activeObjects.clear();

        int left   = (int)(centerX - simulationRadius);
        int top    = (int)(centerY - simulationRadius);
        int width  = (int)(simulationRadius * 2);
        int height = (int)(simulationRadius * 2);
        currentBounds = new Rectangle(left, top, width, height);

        // Calcular qué chunks intersectan con la región
        int cx0 = GlobalChunkResolver.firstChunkIndex(currentBounds.getMinX(), chunkWidth);
        int cy0 = GlobalChunkResolver.firstChunkIndex(currentBounds.getMinY(), chunkHeight);
        int cx1 = GlobalChunkResolver.lastChunkIndex(currentBounds.getMaxX(), chunkWidth);
        int cy1 = GlobalChunkResolver.lastChunkIndex(currentBounds.getMaxY(), chunkHeight);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cy = cy0; cy <= cy1; cy++) {
                Chunk chunk = storage.get(new WorldCoordinator(cx, cy));
                if (chunk != null && chunk.isLoaded()) {
                    activeObjects.addAll(chunk.getObjects());
                }
            }
        }

        // Entidades dinámicas
        activeObjects.addAll(dynamics.getAll());
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    /**
     * Lista unificada de todos los objetos activos para el frame actual.
     * Incluye objetos estáticos de chunks + todas las entidades dinámicas.
     *
     * Esta es la lista que reciben CollisionsSystem, AISystem, etc.
     *
     * @return lista inmutable de objetos activos
     */
    public List<GameObjects> getActiveObjects() {
        return Collections.unmodifiableList(activeObjects);
    }

    /**
     * Bounds actuales de la región de simulación en coords globales.
     *
     * @return Rectangle de la región activa
     */
    public Rectangle getCurrentBounds() {
        return new Rectangle(currentBounds);
    }

    /**
     * True si la posición global (gx, gy) está dentro de la región activa.
     *
     * @param gx posición X global
     * @param gy posición Y global
     * @return true si el punto está en la región de simulación
     */
    public boolean contains(double gx, double gy) {
        return currentBounds.contains(gx, gy);
    }
}
