package Display.Managers;

import Display.ResizeListener;
import Display.Managers.FullscreenManager;
import Display.Managers.BufferStrategyManager;
import Display.Managers.RenderSurfaceManager;
import Display.Managers.ScalingManager;
import Display.Settings.DisplaySettings;
import Display.ViewportInfo;
import Display.Managers.ViewportManager;
import Display.Managers.WindowManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * Fachada principal del sistema de display.
 *
 * Coordina todos los subsistemas:
 *   DisplayManager
 *    ├── WindowManager        — JFrame + Canvas + resize detection
 *    ├── FullscreenManager    — fullscreen real sin dispose()
 *    ├── ViewportManager      — cálculo de viewport/scale
 *    ├── ScalingManager       — presentación del framebuffer virtual
 *    └── RenderSurfaceManager — framebuffer virtual + BufferStrategy
 *
 * USO TÍPICO EN EL GAME LOOP:
 *
 *   // Inicio de frame:
 *   Graphics2D virtualG = displayManager.beginFrame();
 *   if (virtualG == null) return; // frame saltado (contentsLost recovery)
 *
 *   // Render del juego completo en coordenadas virtuales:
 *   gameState.draw(virtualG, displayManager.getViewport());
 *
 *   // Presentar a pantalla:
 *   displayManager.endFrame(virtualG);
 *
 * REGLA: el juego NO conoce la resolución real del monitor.
 *        Solo usa VIRTUAL_WIDTH / VIRTUAL_HEIGHT y ViewportInfo para input.
 */
public class DisplayManager {

    private static final Logger LOG = Logger.getLogger(DisplayManager.class.getName());

    // ─── Subsistemas ──────────────────────────────────────────────────────────
    private final DisplaySettings      settings;
    private final ViewportManager      viewportManager;
    private final WindowManager        windowManager;
    private final FullscreenManager    fullscreenManager;
    private final RenderSurfaceManager surfaceManager;
    private final ScalingManager       scalingManager;
    private final BufferStrategyManager bsManager;

    // ─── Estado ───────────────────────────────────────────────────────────────
    /** Graphics2D del framebuffer virtual, válido entre beginFrame/endFrame. */
    private Graphics2D currentVirtualG = null;

    // ─── Constructor ──────────────────────────────────────────────────────────

    public DisplayManager(DisplaySettings settings) {
        this.settings = settings;

        // 1. ViewportManager primero (WindowManager lo necesita para notificar)
        viewportManager   = new ViewportManager(settings);

        // 2. WindowManager (crea JFrame + Canvas, registra resize → viewportManager)
        windowManager     = new WindowManager(settings, viewportManager);

        // 3. Managers independientes
        fullscreenManager = new FullscreenManager(settings.monitorIndex);
        surfaceManager    = new RenderSurfaceManager(settings);
        scalingManager    = new ScalingManager(settings);
        bsManager         = new BufferStrategyManager(windowManager.getCanvas());

        // 4. Registrar resize listener del bsManager (recrear BS al resize)
        windowManager.addResizeListener((rw, rh, vp) -> {
            // El BufferStrategy puede quedar inválido tras resize
            bsManager.recreate();
        });

        LOG.info("DisplayManager inicializado. Virtual: " +
                 settings.virtualWidth + "x" + settings.virtualHeight);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Inicializa la ventana y el BufferStrategy.
     * Llamar ANTES de iniciar el game loop.
     *
     * @param keyListener       listener de teclado (puede ser null)
     * @param mouseListener     listener de mouse (puede ser null)
     * @param motionListener    listener de movimiento mouse (puede ser null)
     * @param wheelListener     listener de rueda mouse (puede ser null)
     */
    public void init(java.awt.event.KeyListener keyListener,
                     java.awt.event.MouseListener mouseListener,
                     java.awt.event.MouseMotionListener motionListener,
                     java.awt.event.MouseWheelListener wheelListener) {

        windowManager.addInputListeners(keyListener, mouseListener, motionListener, wheelListener);
        windowManager.show();

        // DESPUÉS de show() para que el Canvas tenga peer nativo
        bsManager.init();

        // Arrancar en fullscreen si settings lo pide
        if (settings.startFullscreen) {
            fullscreenManager.enterFullscreen(windowManager.getFrame());
        }

        // Trigger inicial de resize para que el viewport se calcule
        // con las dimensiones reales de la ventana visible
        Canvas c = windowManager.getCanvas();
        viewportManager.onResize(c.getWidth(), c.getHeight());

        LOG.info("Display iniciado. Ventana: " + c.getWidth() + "x" + c.getHeight());
    }

    // ─── Frame pipeline ───────────────────────────────────────────────────────

    /**
     * Inicia un frame de render.
     *
     * @return Graphics2D del framebuffer virtual en coordenadas virtuales,
     *         o null si hay que saltar este frame (recovery de contentsLost).
     *
     * Si devuelve no-null, SIEMPRE llamar endFrame() después.
     */
    public Graphics2D beginFrame() {
        currentVirtualG = surfaceManager.beginFrame();
        return currentVirtualG;
    }

    /**
     * Termina el frame: escala el framebuffer virtual a pantalla y presenta.
     *
     * @param virtualG el Graphics2D devuelto por beginFrame() (será disposed)
     */
    public void endFrame(Graphics2D virtualG) {
        // Cerrar el Graphics del framebuffer virtual
        surfaceManager.endFrame(virtualG);
        currentVirtualG = null;

        // Adquirir Graphics2D del canvas real (BufferStrategy)
        Graphics2D screenG = bsManager.acquireGraphics();
        if (screenG == null) return; // Frame saltado, recovery en progreso

        try {
            // Si el buffer fue restaurado, el clear ya se hizo en acquireGraphics
            // ScalingManager llena letterbox + escala el framebuffer
            scalingManager.present(screenG, surfaceManager.getFramebuffer(),
                                   viewportManager.getViewport());
        } finally {
            bsManager.present(screenG); // dispose + show()
        }
    }

    // ─── Fullscreen toggle ────────────────────────────────────────────────────

    /**
     * Alterna fullscreen ↔ windowed de forma segura.
     * NO usa dispose(). Puede llamarse desde el game loop.
     *
     * Internamente notifica onResize() vía ComponentListener del Canvas.
     */
    public void toggleFullscreen() {
        fullscreenManager.toggle(
            windowManager.getFrame(),
            settings.windowedWidth,
            settings.windowedHeight
        );
        // El ComponentListener del Canvas disparará onResize automáticamente
    }

    public boolean isFullscreen() {
        return fullscreenManager.isFullscreen();
    }

    // ─── Acceso a datos del viewport ─────────────────────────────────────────

    /**
     * Viewport actual. Usar para:
     *  - Transformar coordenadas de mouse a coordenadas virtuales
     *  - Saber los límites virtuales visibles
     *
     * Referencia inmutable — seguro cachear por frame.
     */
    public ViewportInfo getViewport() {
        return viewportManager.getViewport();
    }

    /** Ancho virtual fijo (constante de settings). */
    public int getVirtualWidth() {
        return settings.virtualWidth;
    }

    /** Alto virtual fijo (constante de settings). */
    public int getVirtualHeight() {
        return settings.virtualHeight;
    }

    // ─── Registro de listeners externos ──────────────────────────────────────

    /**
     * Registra un listener para cuando el canvas cambia de tamaño.
     * Útil para que Camera, UIManager y otros sistemas se adapten.
     */
    public void addResizeListener(ResizeListener l) {
        windowManager.addResizeListener(l);
    }

    public void removeResizeListener(ResizeListener l) {
        windowManager.removeResizeListener(l);
    }

    // ─── Acceso a subsistemas (para casos avanzados) ─────────────────────────

    public WindowManager     getWindowManager()     { return windowManager;     }
    public ViewportManager   getViewportManager()   { return viewportManager;   }
    public FullscreenManager getFullscreenManager() { return fullscreenManager; }
    public Canvas            getCanvas()            { return windowManager.getCanvas(); }
}
