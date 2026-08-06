package Game.World.Core;

import Game.Engine.GameObjects;
import Game.World.Generator.WorldGenerator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.function.Supplier;

/**
 * Servicio de pre-generación anticipada de sectores vecinos.
 *
 * ── EXTRACCIÓN DESDE WorldManager ─────────────────────────────────────────
 * La lógica de prewarming fue extraída de WorldManager para cumplir SRP.
 * WorldManager era responsable de: world state, camera, render, transitions,
 * Y prewarming. WorldPrewarmService recibe exclusivamente esta responsabilidad.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Monitorizar la posición del objeto rastreado y generar en background los
 * sectores vecinos antes de que el jugador llegue a ellos, eliminando el
 * hitching al cruzar bordes.
 *
 * ── CÓMO FUNCIONA ─────────────────────────────────────────────────────────
 * En cada tick, evalúa si el objeto rastreado está dentro del umbral de
 * proximidad a un borde. Si es así, envía al ExecutorService la generación
 * del sector vecino correspondiente.
 *
 * La generación es idempotente: si el sector ya existe en caché, el trabajo
 * es descartado sin error.
 *
 * ── UMBRAL CONFIGURABLE ───────────────────────────────────────────────────
 * prewarmThreshold: distancia al borde en píxeles para activar el prewarm.
 * Valor por defecto: 300px. Con sectores de 1280px, el prewarm se activa
 * cuando el jugador está a menos de 300px del borde (~23% del ancho).
 */
public final class WorldPrewarmService {

    private static final Logger LOG = Logger.getLogger(WorldPrewarmService.class.getName());

    private final WorldCache          cache;
    private final WorldGenerator      generator;
    private final ExecutorService     bgExecutor;
    private final Supplier<WorldCoordinator> currentCoordSupplier;

    private int prewarmThreshold = 300;

    /**
     * @param cache                 caché de mundos
     * @param generator             generador de mundos
     * @param currentCoordSupplier  proveedor de la coordenada activa actual
     */
    public WorldPrewarmService(WorldCache cache,
                                WorldGenerator generator,
                                Supplier<WorldCoordinator> currentCoordSupplier) {
        this.cache                = cache;
        this.generator            = generator;
        this.currentCoordSupplier = currentCoordSupplier;
        this.bgExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WorldPrewarm");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Configura el umbral de proximidad al borde para activar el prewarm.
     * @param pixels distancia en píxeles al borde que activa la pre-generación.
     */
    public void setPrewarmThreshold(int pixels) {
        this.prewarmThreshold = Math.max(0, pixels);
    }

    public int getPrewarmThreshold() { return prewarmThreshold; }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Evalúa si el objeto rastreado está cerca de un borde y programa
     * la generación anticipada de los sectores vecinos correspondientes.
     *
     * Llamar una vez por tick desde WorldManager.update().
     *
     * @param tracked       el objeto rastreado (jugador u otro)
     * @param logicalWidth  ancho lógico de cada sector
     * @param logicalHeight alto lógico de cada sector
     */
    public void update(GameObjects tracked, int logicalWidth, int logicalHeight) {
        if (tracked == null) return;

        var pos = tracked.getTransform().getPosition();
        double px = pos.getX();
        double py = pos.getY();

        if (px < prewarmThreshold)                   scheduleNeighbor(-1,  0, logicalWidth, logicalHeight);
        if (px > logicalWidth  - prewarmThreshold)   scheduleNeighbor( 1,  0, logicalWidth, logicalHeight);
        if (py < prewarmThreshold)                   scheduleNeighbor( 0, -1, logicalWidth, logicalHeight);
        if (py > logicalHeight - prewarmThreshold)   scheduleNeighbor( 0,  1, logicalWidth, logicalHeight);
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Apaga el ExecutorService del prewarm.
     * Llamar al cerrar la aplicación o al destruir el WorldManager.
     */
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

    // ── Privado ───────────────────────────────────────────────────────────

    private void scheduleNeighbor(int dx, int dy, int logicalWidth, int logicalHeight) {
        WorldCoordinator currentCoord = currentCoordSupplier.get();
        if (currentCoord == null) return;

        WorldCoordinator neighborCoord = new WorldCoordinator(
            currentCoord.x() + dx,
            currentCoord.y() + dy
        );

        synchronized (cache) {
            if (cache.contains(neighborCoord)) return;
        }

        final int w = logicalWidth;
        final int h = logicalHeight;

        bgExecutor.submit(() -> {
            World generated = generator.generate(w, h, neighborCoord);
            synchronized (cache) {
                if (!cache.contains(neighborCoord)) {
                    cache.put(generated);
                }
            }
        });
    }
}
