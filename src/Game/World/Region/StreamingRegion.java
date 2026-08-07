package Game.World.Region;

import Game.World.Chunk.GlobalChunkResolver;
import Game.World.Core.WorldCoordinator;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Región de streaming — determina qué chunks deben estar cargados en memoria.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * StreamingRegion calcula el conjunto de WorldCoordinators que deben tener
 * su Chunk disponible en ChunkStorage para soportar la simulación y el render.
 *
 * NO decide qué objetos se simulan (eso es SimulationRegion).
 * NO decide qué objetos se renderizan (eso es RenderRegion / GameCamera).
 * NO carga chunks por sí misma (eso es WorldPrewarmService).
 *
 * ── TRES CONTRIBUYENTES ───────────────────────────────────────────────────
 * La StreamingRegion agrupa tres fuentes de demanda de datos:
 *
 *   1. Simulation area  → chunks necesarios para física/IA/colisiones
 *   2. Camera/render area → chunks visibles en la cámara
 *   3. Prewarm margin  → chunks para precarga anticipada
 *
 * La unión de estos tres conjuntos define los chunks requeridos.
 * Un chunk puede ser descargado solo si NO aparece en ninguno de los tres.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 * SimulationRegion.radius <= StreamingRegion.streamingRadius
 * RenderRegion <= StreamingRegion.streamingRadius
 *
 * Si un chunk es necesario para simular o renderizar, ya debe estar cargado.
 * StreamingRegion garantiza esto al ser siempre mayor.
 *
 * ── INDEPENDENCIA DEL PLAYER ──────────────────────────────────────────────
 * La región se actualiza con dos posiciones: la del player (centro de
 * simulación) y la de la cámara (centro de render). Son independientes
 * y pueden diferir (cinemáticas, pantalla dividida futura).
 */
public final class StreamingRegion {

    // ── Radios ────────────────────────────────────────────────────────────

    /**
     * Radio total de streaming en píxeles globales.
     * Debe ser >= max(simulationRadius, renderRadius).
     * El margen extra es el prewarm buffer.
     */
    private double streamingRadius;

    // ── Estado ────────────────────────────────────────────────────────────

    /** Conjunto de chunks requeridos en el último tick. */
    private final Set<WorldCoordinator> requiredChunks = new HashSet<>();

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * @param streamingRadius radio total de streaming en píxeles globales.
     *                        Valor típico: 2.5 × chunkWidth.
     */
    public StreamingRegion(double streamingRadius) {
        this.streamingRadius = streamingRadius;
    }

    // ── Configuración en runtime ──────────────────────────────────────────

    /**
     * Actualiza el radio de streaming.
     *
     * @param radius nuevo radio en píxeles globales
     */
    public void setStreamingRadius(double radius) {
        this.streamingRadius = Math.max(1, radius);
    }

    public double getStreamingRadius() {
        return streamingRadius;
    }

    // ── Actualización ─────────────────────────────────────────────────────

    /**
     * Recalcula el conjunto de chunks requeridos considerando la posición
     * del player y de la cámara.
     *
     * La unión de los chunks cubiertos por la StreamingRegion centrada en
     * ambos puntos define el conjunto final requerido.
     *
     * @param playerX     posición X global del player
     * @param playerY     posición Y global del player
     * @param cameraX     posición X global del centro de la cámara
     * @param cameraY     posición Y global del centro de la cámara
     * @param chunkWidth  ancho de cada chunk en píxeles
     * @param chunkHeight alto de cada chunk en píxeles
     */
    public void update(double playerX, double playerY,
                       double cameraX, double cameraY,
                       int chunkWidth, int chunkHeight) {
        requiredChunks.clear();

        // Contribución del player
        addChunksInRadius(playerX, playerY, chunkWidth, chunkHeight);

        // Contribución de la cámara (puede diferir si hay cinemática activa)
        if (Math.abs(cameraX - playerX) > 1 || Math.abs(cameraY - playerY) > 1) {
            addChunksInRadius(cameraX, cameraY, chunkWidth, chunkHeight);
        }
    }

    /**
     * Versión simplificada cuando la cámara está centrada en el player.
     * Equivalente a update(pX, pY, pX, pY, ...).
     *
     * @param centerX     posición X global del centro
     * @param centerY     posición Y global del centro
     * @param chunkWidth  ancho de cada chunk
     * @param chunkHeight alto de cada chunk
     */
    public void update(double centerX, double centerY,
                       int chunkWidth, int chunkHeight) {
        requiredChunks.clear();
        addChunksInRadius(centerX, centerY, chunkWidth, chunkHeight);
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    /**
     * Conjunto de WorldCoordinators que deben tener su Chunk cargado
     * en ChunkStorage en este frame.
     *
     * WorldManager compara este conjunto con ChunkStorage.loadedCoords()
     * para determinar qué cargar y qué puede descargarse.
     *
     * @return set inmutable de coordenadas requeridas
     */
    public Set<WorldCoordinator> getRequiredChunks() {
        return Collections.unmodifiableSet(requiredChunks);
    }

    /**
     * True si el chunk indicado es necesario en el frame actual.
     *
     * @param coord coordenada del chunk a comprobar
     * @return true si debe estar cargado
     */
    public boolean requires(WorldCoordinator coord) {
        return requiredChunks.contains(coord);
    }

    /**
     * Calcula los chunks que están cargados pero ya no son necesarios.
     * Estos son candidatos para eviction.
     *
     * @param loadedCoords conjunto de coords actualmente en ChunkStorage
     * @return set de coords que pueden descargarse
     */
    public Set<WorldCoordinator> findEvictable(Set<WorldCoordinator> loadedCoords) {
        Set<WorldCoordinator> evictable = new HashSet<>(loadedCoords);
        evictable.removeAll(requiredChunks);
        return evictable;
    }

    // ── Privado ───────────────────────────────────────────────────────────

    /**
     * Añade al conjunto requiredChunks todos los chunks dentro del radio
     * de streaming centrado en (centerX, centerY).
     */
    private void addChunksInRadius(double centerX, double centerY,
                                    int chunkWidth, int chunkHeight) {
        double left   = centerX - streamingRadius;
        double top    = centerY - streamingRadius;
        double right  = centerX + streamingRadius;
        double bottom = centerY + streamingRadius;

        int cx0 = GlobalChunkResolver.firstChunkIndex(left,   chunkWidth);
        int cy0 = GlobalChunkResolver.firstChunkIndex(top,    chunkHeight);
        int cx1 = GlobalChunkResolver.lastChunkIndex(right,  chunkWidth);
        int cy1 = GlobalChunkResolver.lastChunkIndex(bottom, chunkHeight);

        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cy = cy0; cy <= cy1; cy++) {
                requiredChunks.add(new WorldCoordinator(cx, cy));
            }
        }
    }
}
