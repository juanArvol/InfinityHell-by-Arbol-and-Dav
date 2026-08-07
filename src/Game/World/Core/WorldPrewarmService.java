package Game.World.Core;

import Game.Engine.GameObjects;
import Game.World.Generator.WorldGenerator;
import Game.World.Region.StreamingRegion;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Servicio de pre-generación anticipada de chunks vecinos.
 *
 * ── ETAPA 5: Migración a coordenadas globales y StreamingRegion ───────────
 *
 * ANTES: detectaba proximidad al borde con coordenadas locales al sector:
 *   if (px < prewarmThreshold) scheduleNeighbor(-1, 0, ...)
 *   if (px > logicalWidth - prewarmThreshold) scheduleNeighbor(1, 0, ...)
 *   ... asumía que el player siempre estaba en [0, logicalWidth]
 *
 * AHORA: usa StreamingRegion para calcular qué chunks son necesarios
 *   según la posición GLOBAL del player. StreamingRegion devuelve el
 *   conjunto completo de coords requeridas (no solo los 4 vecinos inmediatos).
 *   Para cada coord requerida que no esté en WorldCache, programa su generación.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 * StreamingRegion.streamingRadius >= SimulationRegion.simulationRadius
 * → Los chunks necesarios para simular siempre se generan antes de ser usados.
 *
 * ── WorldCache (legacy) ───────────────────────────────────────────────────
 * Sigue usando WorldCache durante esta etapa por compatibilidad con
 * WorldManager y TransitionSystem. En Etapa 9, WorldCache se reemplaza
 * por World.getChunkStorage() y este servicio se actualiza para usar Chunk
 * directamente a través de ChunkStorage.
 */
public final class WorldPrewarmService {

    private static final Logger LOG = Logger.getLogger(WorldPrewarmService.class.getName());

    private final WorldCache       cache;
    private final WorldGenerator   generator;
    private final ExecutorService  bgExecutor;
    private final StreamingRegion  streamingRegion;

    /**
     * @param cache          caché legacy de mundos
     * @param generator      generador de mundos
     * @param chunkWidth     ancho de cada chunk en píxeles globales
     * @param chunkHeight    alto de cada chunk en píxeles globales
     * @param streamingRadius radio de streaming en píxeles globales
     *                        (típico: 2.5 × chunkWidth)
     */
    public WorldPrewarmService(WorldCache cache,
                                WorldGenerator generator,
                                int chunkWidth,
                                int chunkHeight,
                                double streamingRadius) {
        this.cache           = cache;
        this.generator       = generator;
        this.streamingRegion = new StreamingRegion(streamingRadius);
        this.bgExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WorldPrewarm");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    /**
     * Constructor legacy para compatibilidad durante la transición.
     * Usa streamingRadius = 2.5 × max(chunkWidth, chunkHeight).
     *
     * @deprecated Usar el constructor con streamingRadius explícito.
     */
    @Deprecated(forRemoval = true)
    public WorldPrewarmService(WorldCache cache,
                                WorldGenerator generator,
                                java.util.function.Supplier<WorldCoordinator> ignored) {
        // El Supplier<WorldCoordinator> ya no es necesario — usamos coords globales
        this(cache, generator, 1280, 720, 1280 * 2.5);
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Evalúa qué chunks son necesarios según la posición global del player
     * y programa su generación en background si no están disponibles.
     *
     * Usa StreamingRegion para calcular el conjunto de coords requeridas.
     * Los chunks ya disponibles se ignoran (idempotente).
     *
     * @param tracked       el objeto rastreado (player u otro)
     * @param logicalWidth  ancho de cada chunk en píxeles
     * @param logicalHeight alto de cada chunk en píxeles
     */
    public void update(GameObjects tracked, int logicalWidth, int logicalHeight) {
        if (tracked == null) return;

        var pos = tracked.getTransform().getPosition();
        double px = pos.getX();
        double py = pos.getY();

        // Calcular qué chunks son necesarios según la posición global
        streamingRegion.update(px, py, logicalWidth, logicalHeight);
        Set<WorldCoordinator> required = streamingRegion.getRequiredChunks();

        for (WorldCoordinator coord : required) {
            synchronized (cache) {
                if (cache.contains(coord)) continue;
            }
            scheduleChunk(coord, logicalWidth, logicalHeight);
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public void shutdown() {
        bgExecutor.shutdown();
        try {
            if (!bgExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                bgExecutor.shutdownNow();
                LOG.warning("WorldPrewarmService: bgExecutor did not terminate in 2s — forced shutdown.");
            }
        } catch (InterruptedException e) {
            bgExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ── Configuración ─────────────────────────────────────────────────────

    public void setStreamingRadius(double radius) {
        streamingRegion.setStreamingRadius(radius);
    }

    public double getStreamingRadius() {
        return streamingRegion.getStreamingRadius();
    }

    // ── Privado ───────────────────────────────────────────────────────────

    private void scheduleChunk(WorldCoordinator coord, int w, int h) {
        bgExecutor.submit(() -> {
            // generateLegacyWorld: genera Chunk y envuelve en World para WorldCache.
            // Eliminar cuando WorldCache → ChunkStorage.
            World generated = generateLegacyWorld(w, h, coord);
            synchronized (cache) {
                if (!cache.contains(coord)) {
                    cache.put(generated);
                }
            }
        });
    }

    /**
     * Wrapper que aísla el warning de deprecación durante la transición.
     *
     * @SuppressWarnings justificado: llamada interna de migración.
     */
    @SuppressWarnings({"deprecation", "removal"})
    private World generateLegacyWorld(int w, int h, WorldCoordinator coord) {
        return generator.generate(w, h, coord);
    }
}
