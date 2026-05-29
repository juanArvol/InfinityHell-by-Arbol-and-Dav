package Display.Managers;

import Display.ResizeListener;
import Display.Settings.DisplaySettings;
import Display.ViewportInfo;
import Entradas.KeyBoard;

import java.awt.*;
import java.awt.event.FocusListener;
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
 * ─── FLUJO CORRECTO DE F11 / toggleFullscreen ────────────────────────────────
 *
 *  GameLoop thread
 *    → keyboard.update()
 *    → edge "toggleFullscreen" detectado → fsTogglePending = true
 *    → KeyActionListener.onKeyAction("toggleFullscreen")
 *    → display.toggleFullscreen(keyboard)          ← este método
 *         invokeLater #1: toggleInProgress = true
 *                         fullscreenManager.toggle(...)
 *                         invokeLater #2: canvas.requestFocusInWindow()
 *                                         keyboard.clearFsTogglePending()
 *                                         toggleInProgress = false
 *
 *  Durante invokeLater #1 y #2, el Canvas recibe eventos componentResized
 *  del WM.  WindowManager.componentResized() comprueba toggleInProgress
 *  antes de llamar bsManager.recreate().  Si está en progreso, el recreate
 *  se encola para el invokeLater #2 (post-toggle).
 *
 *  Así se elimina el loop:
 *    resize durante toggle → recreate suprimido → BS sigue válido → no crash.
 *
 *  El recreate final (post-toggle, en invokeLater #2) garantiza que el BS
 *  queda correcto para el nuevo tamaño de canvas.
 *
 * ─── BUGS CORREGIDOS ──────────────────────────────────────────────────────────
 *
 * BUG-LOOP-F11 · componentResized() durante toggle llama bsManager.recreate()
 *   repetidamente, corrompiendo el BufferStrategy y colapsando el juego.
 *   SOLUCIÓN: flag volatile toggleInProgress; resize listener lo consulta.
 *   El recreate se hace UNA vez al finalizar el toggle, en el EDT.
 *
 * BUG-EDT-FULLSCREEN · operaciones Swing llamadas fuera del EDT.
 *   SOLUCIÓN: toggleFullscreen() usa invokeLater().
 *
 * BUG-FOCUS-LOST-AFTER-TOGGLE · Canvas pierde foco tras toggle.
 *   SOLUCIÓN: requestFocusInWindow() en invokeLater anidado post-toggle.
 *
 * BUG-F11-DOBLE-TOGGLE · F11 puede dispararse dos veces durante el toggle.
 *   SOLUCIÓN: fsTogglePending en KeyBoard + clearFsTogglePending() desde EDT.
 *
 * BUG-VIEWPORT-INICIAL · getCanvas().getWidth() puede ser 0 justo tras show().
 *   SOLUCIÓN: validar w > 0 && h > 0 antes de llamar onResize().
 */
public class DisplayManager {

    private static final Logger LOG = Logger.getLogger(DisplayManager.class.getName());

    // ─── Subsistemas ──────────────────────────────────────────────────────────
    private final DisplaySettings       settings;
    private final ViewportManager       viewportManager;
    private final WindowManager         windowManager;
    private final FullscreenManager     fullscreenManager;
    private final RenderSurfaceManager  surfaceManager;
    private final ScalingManager        scalingManager;
    private final BufferStrategyManager bsManager;

    // ─── Estado ───────────────────────────────────────────────────────────────
    /**
     * BUG-LOOP-F11 FIX: true mientras un toggle fullscreen está en curso.
     * volatile: escrito y leído desde el EDT; leído desde el ComponentListener
     * (también EDT), pero declarado volatile por claridad de intención.
     *
     * Mientras está en true, el resize listener NO llama bsManager.recreate().
     * El recreate se hace una sola vez al final del toggle.
     */
    private volatile boolean toggleInProgress = false;

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

        // 4. BUG-LOOP-F11 FIX:
        //    El resize listener recreará el BS solo cuando NO haya un toggle
        //    en progreso. Durante el toggle, el WM puede disparar 2-4 eventos
        //    componentResized (setVisible false/true, setFullScreenWindow,
        //    setSize windowed). Recrear el BS en cada uno de esos eventos
        //    deja el BS en null repetidamente, el GameLoop salta frames,
        //    y la ventana colapsa en un bucle de resize → crash.
        //
        //    Con este guard, el recreate ocurre UNA vez al finalizar el toggle
        //    (ver toggleFullscreen → invokeLater anidado).
        windowManager.addResizeListener((rw, rh, vp) -> {
            if (!toggleInProgress) {
                bsManager.recreate();
            }
        });

        LOG.info("DisplayManager inicializado. Virtual: " +
                 settings.virtualWidth + "x" + settings.virtualHeight);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /**
     * Overload de compatibilidad sin FocusListener.
     */
    public void init(java.awt.event.KeyListener keyListener,
                     java.awt.event.MouseListener mouseListener,
                     java.awt.event.MouseMotionListener motionListener,
                     java.awt.event.MouseWheelListener wheelListener) {
        init(keyListener, mouseListener, motionListener, wheelListener, null);
    }

    /**
     * Inicializa la ventana y el BufferStrategy.
     * Llamar ANTES de iniciar el game loop.
     */
    public void init(java.awt.event.KeyListener keyListener,
                     java.awt.event.MouseListener mouseListener,
                     java.awt.event.MouseMotionListener motionListener,
                     java.awt.event.MouseWheelListener wheelListener,
                     FocusListener focusListener) {

        windowManager.addInputListeners(keyListener, mouseListener,
                                        motionListener, wheelListener,
                                        focusListener);
        windowManager.show();

        // DESPUÉS de show() para que el Canvas tenga peer nativo
        bsManager.init();

        // Arrancar en fullscreen si settings lo pide.
        // invokeAndWait: el hilo main espera a que la ventana esté lista.
        if (settings.startFullscreen) {
            try {
                javax.swing.SwingUtilities.invokeAndWait(() ->
                    fullscreenManager.enterFullscreen(windowManager.getFrame())
                );
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (java.lang.reflect.InvocationTargetException e) {
                LOG.warning("Error al entrar en fullscreen inicial: " + e.getCause());
            }
        }

        // BUG-VIEWPORT-INICIAL FIX
        Canvas c = windowManager.getCanvas();
        int w = c.getWidth();
        int h = c.getHeight();
        if (w > 0 && h > 0) {
            viewportManager.onResize(w, h);
        }

        LOG.info("Display iniciado. Canvas: " + w + "x" + h);
    }

    // ─── Frame pipeline ───────────────────────────────────────────────────────

    public Graphics2D beginFrame() {
        currentVirtualG = surfaceManager.beginFrame();
        return currentVirtualG;
    }

    public void endFrame(Graphics2D virtualG) {
        surfaceManager.endFrame(virtualG);
        currentVirtualG = null;

        Graphics2D screenG = bsManager.acquireGraphics();
        if (screenG == null) return;

        try {
            scalingManager.present(screenG, surfaceManager.getFramebuffer(),
                                   viewportManager.getViewport());
        } finally {
            bsManager.present(screenG);
        }
    }

    // ─── Fullscreen toggle ────────────────────────────────────────────────────

    /**
     * Alterna fullscreen ↔ windowed de forma segura desde cualquier thread.
     *
     * FLUJO:
     *   invokeLater #1 (EDT):
     *     · toggleInProgress = true       → suprime recreates del resize listener
     *     · fullscreenManager.toggle()    → opera Swing/AWT en el EDT (correcto)
     *     invokeLater #2 (EDT, anidado):  → garantiza que setVisible(true) terminó
     *       · canvas.requestFocusInWindow() → restaurar foco
     *       · keyboard.clearFsTogglePending() → liberar guard anti-doble-edge
     *       · bsManager.recreate()          → UN recreate limpio post-toggle
     *       · toggleInProgress = false      → resize listener vuelve a operar
     *
     * @param keyboard el KeyBoard del juego (para restaurar foco y limpiar guard).
     */
    public void toggleFullscreen(KeyBoard keyboard) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            // BUG-LOOP-F11 FIX: bloquear recreates durante el toggle
            toggleInProgress = true;

            fullscreenManager.toggle(
                windowManager.getFrame(),
                settings.windowedWidth,
                settings.windowedHeight
            );

            // Anidado para ejecutarse DESPUÉS de que setVisible(true) haya
            // devuelto el control al WM y el canvas tenga su tamaño final.
            javax.swing.SwingUtilities.invokeLater(() -> {
                windowManager.getCanvas().requestFocusInWindow();
                keyboard.clearFsTogglePending();

                // UN recreate limpio con el canvas ya en su tamaño final
                bsManager.recreate();

                // BUG-LOOP-F11 FIX: habilitar de nuevo el resize listener
                toggleInProgress = false;
            });
        });
    }

    /**
     * Overload de compatibilidad sin KeyBoard.
     * No corrige BUG-F11-DOBLE-TOGGLE ni BUG-FOCUS-LOST-AFTER-TOGGLE.
     * Solo úsalo si realmente no tienes acceso al KeyBoard.
     */
    public void toggleFullscreen() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            toggleInProgress = true;
            fullscreenManager.toggle(
                windowManager.getFrame(),
                settings.windowedWidth,
                settings.windowedHeight
            );
            javax.swing.SwingUtilities.invokeLater(() -> {
                bsManager.recreate();
                toggleInProgress = false;
            });
        });
    }

    public boolean isFullscreen() {
        return fullscreenManager.isFullscreen();
    }

    // ─── Acceso a datos del viewport ─────────────────────────────────────────

    public ViewportInfo getViewport() {
        return viewportManager.getViewport();
    }

    public int getVirtualWidth() {
        return settings.virtualWidth;
    }

    public int getVirtualHeight() {
        return settings.virtualHeight;
    }

    // ─── Registro de listeners externos ──────────────────────────────────────

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
