package Game.World.Core;

import Game.Engine.Camera.CameraController;
import Game.Engine.Camera.FollowCameraController;
import Game.Engine.Camera.GameCamera;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Scene.SceneRenderer;
import Game.Engine.Systems.DebugSettings;
import Game.World.Generator.WorldGenerator;
import java.awt.Graphics2D;

/**
 * Gestiona los mundos del juego: cache, generación y transiciones.
 *
 * ── Responsabilidades ────────────────────────────────────────────────────
 *
 * WorldManager es el punto de composición entre:
 *   - El mundo actual (World) — expone getTrackedPosition().
 *   - La cámara del Engine (GameCamera) — servicio de primer nivel.
 *   - El controlador de cámara (CameraController) — comportamiento de seguimiento.
 *
 * Es la capa de composición correcta para este wiring porque:
 *   - Conoce cuál es el mundo actual (transiciones).
 *   - Conoce cuál es el objeto rastreado (trackedObject).
 *   - Sabe cuándo hay un cambio de mundo (debe reposicionar la cámara).
 *
 * GameState no necesita conocer la cámara directamente; la obtiene de
 * WorldManager cuando la necesita (por ejemplo, para UIBootstrap).
 */
public class WorldManager {

    private static final int PREWARM_THRESHOLD = 300;

    /**
     * Delta de tiempo fijo pasado al CameraController en cada tick.
     *
     * Debe coincidir con el targetFps configurado en DisplaySettings y GameLoop.
     * El valor actual (1/30) corresponde a 30 fps, que es el targetFps configurado
     * en GameOrquester.
     *
     * Si el targetFps cambia en GameOrquester, este valor debe actualizarse.
     * El lugar correcto a largo plazo es recibir targetFps como parámetro de
     * constructor e inicializar esta constante como campo: 1.0 / targetFps.
     */
    private static final double CAMERA_DELTA_TIME = 1.0 / 30.0;

    private final WorldCache              cache;
    private final WorldGenerator          generator;
    private final WorldTransitionService  transitionService;
    private final SceneRenderer           renderer;

    /** Cámara del Engine — entidad de primer nivel. */
    private final GameCamera camera;

    /** Controlador de comportamiento de cámara actual. Intercambiable en runtime. */
    private CameraController cameraController;

    private WorldCoordinator currentCoord;
    private int logicalWidth;
    private int logicalHeight;

    // El objeto rastreado para prewarming de vecinos y seguimiento de cámara.
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
     * @param width         ancho lógico de cada mundo
     * @param height        alto lógico de cada mundo
     * @param virtualWidth  ancho virtual del juego (para GameCamera)
     * @param virtualHeight alto virtual del juego (para GameCamera)
     * @param generator     generador de mundos (inyectable para tests o custom config)
     * @param settings      interfaz DebugSettings del Engine
     */
    public WorldManager(int width, int height,
                        int virtualWidth, int virtualHeight,
                        WorldGenerator generator,
                        DebugSettings settings) {
        this.logicalWidth  = width;
        this.logicalHeight = height;
        this.generator     = generator;
        this.cache         = new WorldCache();
        this.renderer      = new SceneRenderer(settings);
        this.transitionService = new WorldTransitionService(cache, generator);
        this.currentCoord  = new WorldCoordinator(0, 0);

        // Crear la cámara del Engine con los límites del mundo.
        this.camera = new GameCamera(virtualWidth, virtualHeight);
        this.camera.setWorldBounds(width, height);

        regenerateAll();
    }

    /** Constructor de conveniencia con generador por defecto. */
    public WorldManager(int width, int height, DebugSettings settings) {
        this(width, height, width, height, new WorldGenerator(), settings);
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

    // ── Cámara del Engine ─────────────────────────────────────────────────────

    /**
     * Devuelve la cámara del Engine.
     *
     * La cámara es una entidad de primer nivel del Engine. UIBootstrap,
     * CrossHairHUD y cualquier sistema que necesite la posición de la vista
     * deben usarla a través de este método.
     */
    public GameCamera getCamera() {
        return camera;
    }

    /**
     * Reemplaza el controlador de comportamiento de cámara.
     *
     * Permite cambiar de seguimiento a libre, a cinemático, etc. en runtime.
     * null desactiva el controlador (la cámara no se mueve automáticamente).
     */
    public void setCameraController(CameraController controller) {
        this.cameraController = controller;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    /**
     * Actualiza el mundo actual, la cámara y pre-genera vecinos.
     */
    public void update() {
        World world = getCurrentWorld();
        world.update();

        if (cameraController != null) {
            cameraController.update(camera, CAMERA_DELTA_TIME);
        }

        if (trackedObject != null) {
            prewarmNeighbors(trackedObject);
        }

        WorldCoordinator nextCoord = transitionService.processTransitions(
            world, currentCoord, logicalWidth, logicalHeight
        );

        if (nextCoord != null) {
            currentCoord = nextCoord;
            World nextWorld = getCurrentWorld();
            // Al cambiar de mundo, registrar el target en el nuevo mundo
            // y reposicionar la cámara con snap (sin lerp) para evitar
            // que la transición produzca un lerp visual largo.
            if (trackedObject != null) {
                nextWorld.setTrackTarget(trackedObject);
                var pos = trackedObject.getTransform().getPosition();
                camera.centerOn(pos.getX(), pos.getY());
                camera.setWorldBounds(logicalWidth, logicalHeight);
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /**
     * Dibuja el mundo actual usando la cámara del Engine.
     *
     * Las dimensiones virtuales se obtienen de GameCamera, que las recibió
     * en el constructor y las mantiene actualizadas via onVirtualResolutionChanged().
     * Son las mismas que las del Display: width×height del framebuffer virtual.
     */
    public void draw(Graphics2D g) {
        renderer.draw(getCurrentWorld(), camera, g,
                      camera.getVirtualWidth(), camera.getVirtualHeight());
    }

    // ── Seguimiento de objeto ─────────────────────────────────────────────────

    /**
     * Registra el objeto a rastrear para prewarming y cámara.
     *
     * Configura automáticamente un FollowCameraController si no hay ninguno.
     * Si ya existe un controlador, no se reemplaza — solo se actualiza el target.
     */
    public void setTrackedObject(GameObjects obj) {
        this.trackedObject = obj;
        getCurrentWorld().setTrackTarget(obj);

        // Configurar controlador de seguimiento por defecto si no hay ninguno.
        if (cameraController == null && obj != null) {
            cameraController = new FollowCameraController(
                () -> {
                    // Obtener la posición del objeto rastreado en el mundo actual.
                    GameObjects tracked = getCurrentWorld().getTrackTarget();
                    if (tracked == null) return null;
                    var pos = tracked.getTransform().getPosition();
                    return new Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D(pos.getX(), pos.getY());
                },
                0.10f   // lerp factor: seguimiento suave (10% por tick)
            );
        }

        // Snap inicial: colocar la cámara directamente sobre el objeto
        // para no empezar con un lerp largo desde (0,0).
        if (obj != null) {
            var pos = obj.getTransform().getPosition();
            camera.centerOn(pos.getX(), pos.getY());
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        this.logicalWidth  = newWidth;
        this.logicalHeight = newHeight;
        camera.setWorldBounds(newWidth, newHeight);
    }

    /** Alias para compatibilidad con llamadas existentes desde GameState. */
    public void onVirtualResize(int newVirtualWidth, int newVirtualHeight) {
        resize(newVirtualWidth, newVirtualHeight);
        camera.onVirtualResolutionChanged(newVirtualWidth, newVirtualHeight);
    }

    public void shutdown() {
        bgExecutor.shutdown();
        try {
            if (!bgExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                bgExecutor.shutdownNow();
                java.util.logging.Logger.getLogger(WorldManager.class.getName())
                    .warning("WorldManager: bgExecutor did not terminate in 2s — forced shutdown.");
            }
        } catch (InterruptedException e) {
            bgExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
