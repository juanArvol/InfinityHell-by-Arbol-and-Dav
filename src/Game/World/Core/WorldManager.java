package Game.World.Core;

import Game.Engine.GameObjects;
import Game.World.Generator.WorldGenerator;
import Main.Debug.DebugGameSettings;

import java.awt.Graphics2D;

/**
 * Gestiona los mundos del juego: cache, generación y transiciones.
 *
 * REFACTORIZACIONES:
 *
 * 1. ELIMINADO SINGLETON:
 *    WorldManager ya no tiene instance estático ni init()/getInstance().
 *    Se instancia normalmente y se inyecta donde se necesita (GameState).
 *    Justificación: el singleton impedía testear GameState de forma aislada
 *    y bloqueaba el camino hacia multiplayer (múltiples instancias de juego).
 *
 * 2. ELIMINADO instanceof Player:
 *    La detección de qué objeto es el "jugador" es responsabilidad del llamador
 *    (WorldTransitionService). WorldManager no debe conocer tipos de gameplay.
 *    Se introduce el concepto de "tracked object" para la cámara/prewarming.
 *
 * 3. ELIMINADO draw(Graphics2D):
 *    WorldManager ya no sabe dibujar. Quien necesite dibujar el mundo usa
 *    WorldRenderer directamente sobre getCurrentWorld().
 *    Esto separa coordinación (WorldManager) de presentación (WorldRenderer).
 *
 * 4. DELEGADO WorldTransitionService:
 *    La lógica de transferencia de objetos entre mundos vive en WorldTransitionService.
 *    WorldManager solo coordina: detecta vecinos a precargar y delega transferencias.
 */
public class WorldManager {

    private static final int PREWARM_THRESHOLD = 300;

    private final WorldCache              cache;
    private final WorldGenerator          generator;
    private final WorldTransitionService  transitionService;
    private final WorldRenderer           renderer;

    private WorldCoordinator currentCoord;
    private int logicalWidth;
    private int logicalHeight;

    // El objeto rastreado para prewarming de vecinos (generalmente el player).
    // WorldManager NO sabe que es un Player; solo sabe que tiene un Transform.
    private GameObjects trackedObject;

    private final java.util.concurrent.ExecutorService bgExecutor =
        java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "WorldPrewarm");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });

    /**
     * Constructor principal — todos los colaboradores inyectados.
     *
     * @param width      ancho lógico de cada mundo
     * @param height     alto lógico de cada mundo
     * @param generator  generador de mundos (inyectable para tests o custom config)
     * @param settings   configuración del juego (para DebugRenderSystem via WorldRenderer)
     */
    public WorldManager(int width, int height, WorldGenerator generator, DebugGameSettings settings) {
        this.logicalWidth  = width;
        this.logicalHeight = height;
        this.generator     = generator;
        this.cache         = new WorldCache();
        this.renderer      = new WorldRenderer(settings);
        this.transitionService = new WorldTransitionService(cache, generator);
        this.currentCoord  = new WorldCoordinator(0, 0);

        regenerateAll();
    }

    /** Constructor de conveniencia con generador por defecto. */
    public WorldManager(int width, int height, DebugGameSettings settings) {
        this(width, height, new WorldGenerator(), settings);
    }

    // ── Acceso al mundo actual ─────────────────────────────────────────────────

    public World getCurrentWorld() {
        synchronized (cache) {
            if (!cache.contains(currentCoord)) {
                cache.put(generator.generate(logicalWidth, logicalHeight, currentCoord));
            }
            return cache.get(currentCoord);
        }
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Actualiza el mundo actual, pre-genera vecinos y procesa transferencias.
     *
     * La coordinación de qué mundo es "actual" ocurre aquí cuando
     * WorldTransitionService reporta que el objeto rastreado cruzó un borde.
     */
    public void update(int virtualWidth, int virtualHeight) {
        World world = getCurrentWorld();
        world.update();

        if (trackedObject != null) {
            prewarmNeighbors(trackedObject);
        }

        WorldCoordinator nextCoord = transitionService.processTransitions(
            world, currentCoord, logicalWidth, logicalHeight
        );

        if (nextCoord != null) {
            currentCoord = nextCoord;
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Dibuja el mundo actual.
     * Delegado a WorldRenderer para separar coordinación de presentación.
     */
    public void draw(Graphics2D g) {
        renderer.draw(getCurrentWorld(), g);
    }

    // ── Seguimiento de objeto (para prewarming) ────────────────────────────────

    /**
     * Registra el objeto a rastrear para prewarming de vecinos (típicamente el player).
     * WorldManager NO sabe que es un Player — solo usa su Transform.
     */
    public void setTrackedObject(GameObjects obj) {
        this.trackedObject = obj;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        this.logicalWidth  = newWidth;
        this.logicalHeight = newHeight;
    }

    /** Alias para compatibilidad con llamadas existentes desde GameState. */
    public void onVirtualResize(int newVirtualWidth, int newVirtualHeight) {
        resize(newVirtualWidth, newVirtualHeight);
    }

    public void shutdown() {
        bgExecutor.shutdown();
    }

    // ── Prewarming de vecinos ──────────────────────────────────────────────────

    private void prewarmNeighbors(GameObjects tracked) {
        var pos = tracked.getTransform().getPosition();
        double px = pos.getX();
        double py = pos.getY();

        if (px < PREWARM_THRESHOLD)                 scheduleNeighbor(-1,  0);
        if (px > logicalWidth  - PREWARM_THRESHOLD) scheduleNeighbor( 1,  0);
        if (py < PREWARM_THRESHOLD)                 scheduleNeighbor( 0, -1);
        if (py > logicalHeight - PREWARM_THRESHOLD) scheduleNeighbor( 0,  1);
    }

    private void scheduleNeighbor(int dx, int dy) {
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

    private void regenerateAll() {
        synchronized (cache) {
            cache.clear();
            cache.put(generator.generate(logicalWidth, logicalHeight, currentCoord));
        }
    }
}
