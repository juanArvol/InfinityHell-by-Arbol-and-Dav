package Display.Managers;

import Display.Backend.AwtWindowBackend;
import Display.Settings.DisplaySettings;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * Gestiona los listeners de eventos AWT de la ventana.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-003: RESPONSABILIDADES ACOTADAS
 *
 * Después del HRFC-003, WindowManager ya no construye ni posee el JFrame
 * ni el Canvas. Esa responsabilidad pertenece a AwtWindowBackend.
 *
 * WindowManager retiene exactamente una responsabilidad:
 *   Registrar listeners AWT sobre los objetos que el Backend expone
 *   y convertir los eventos en notificaciones a los listeners del Engine.
 *
 * Mapa de eventos → acciones:
 *
 *   componentResized (Canvas) → CanvasResizeListener → ResizeCanvas cmd
 *   windowIconified           → suppressResize=true + onWindowSuspended()
 *   windowDeiconified         → suppressResize=false + onWindowResumed(true)
 *   windowDeactivated         → onWindowSuspended()
 *   windowActivated           → onWindowResumed(false)
 *   windowGainedFocus         → backend.requestCanvasFocus()
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   Todos los métodos de registro → EDT únicamente (se llaman desde init).
 *   suppressResize → volatile boolean; escrito y leído desde EDT.
 *   Los listeners registrados se invocan siempre desde el EDT.
 *   getFrame() / getCanvas() → delegan en backend (inmutables post-init).
 */
public final class WindowManager {

    private static final Logger LOG =
        Logger.getLogger(WindowManager.class.getName());

    private final AwtWindowBackend backend;
    private final Dimension        minimumSize;

    private final List<CanvasResizeListener>    resizeListeners    = new CopyOnWriteArrayList<>();
    private final List<WindowLifecycleListener> lifecycleListeners = new CopyOnWriteArrayList<>();

    private volatile boolean suppressResize = false;
    private boolean visible = false;

    private int lastNotifiedWidth  = -1;
    private int lastNotifiedHeight = -1;

    public WindowManager(AwtWindowBackend backend, DisplaySettings settings) {
        this.backend     = backend;
        this.minimumSize = settings.minimumWindowSize;
    }

    // ── Registro de listeners AWT ─────────────────────────────────────────────

    /**
     * Registra todos los listeners AWT sobre el JFrame y Canvas del Backend.
     *
     * Debe llamarse desde el EDT, después de backend.init().
     */
    public void registerListeners() {
        registerCanvasResizeListener();
        registerWindowLifecycleListener();
    }

    private void registerCanvasResizeListener() {
        backend.getCanvas().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!visible || suppressResize) return;

                int w = backend.getCanvas().getWidth();
                int h = backend.getCanvas().getHeight();

                if (w <= 0 || h <= 0) return;
                if (minimumSize != null
                        && (w < minimumSize.width || h < minimumSize.height)) {
                    return;
                }
                if (w == lastNotifiedWidth && h == lastNotifiedHeight) return;
                lastNotifiedWidth  = w;
                lastNotifiedHeight = h;

                for (CanvasResizeListener l : resizeListeners) {
                    l.onCanvasResized(w, h);
                }
            }
        });
    }

    private void registerWindowLifecycleListener() {
        backend.getFrame().addWindowListener(new WindowAdapter() {

            @Override
            public void windowIconified(WindowEvent e) {
                suppressResize(true);
                LOG.fine("WindowManager: windowIconified — suppressResize=true + suspending");
                notifySuspend();
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                suppressResize(false);
                LOG.fine("WindowManager: windowDeiconified — suppressResize=false + resuming (rebuild=true)");
                notifyResume(true);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                LOG.fine("WindowManager: windowDeactivated — suspending");
                notifySuspend();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                LOG.fine("WindowManager: windowActivated — resuming (rebuild=false)");
                notifyResume(false);
            }
        });

        backend.getFrame().addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                LOG.fine("WindowManager: windowGainedFocus — requesting canvas focus");
                backend.requestCanvasFocus();
            }
        });
    }

    // ── Notificaciones ────────────────────────────────────────────────────────

    private void notifySuspend() {
        for (WindowLifecycleListener l : lifecycleListeners) {
            try { l.onWindowSuspended(); }
            catch (Exception ex) {
                LOG.warning("WindowLifecycleListener.onWindowSuspended threw: " + ex);
            }
        }
    }

    private void notifyResume(boolean requiresRebuild) {
        for (WindowLifecycleListener l : lifecycleListeners) {
            try { l.onWindowResumed(requiresRebuild); }
            catch (Exception ex) {
                LOG.warning("WindowLifecycleListener.onWindowResumed threw: " + ex);
            }
        }
    }

    // ── Control de resize ─────────────────────────────────────────────────────

    public void suppressResize(boolean suppress) {
        this.suppressResize = suppress;
        if (!suppress) {
            lastNotifiedWidth  = -1;
            lastNotifiedHeight = -1;
        }
    }

    public boolean isSuppressingResize() { return suppressResize; }

    // ── Visibilidad ───────────────────────────────────────────────────────────

    /** Marca la ventana como visible para que el ComponentListener procese eventos. */
    public void onWindowShown() { this.visible = true; }

    // ── Input listeners ───────────────────────────────────────────────────────

    public void addInputListeners(KeyListener kl,
                                  MouseListener ml,
                                  MouseMotionListener mml,
                                  MouseWheelListener mwl,
                                  FocusListener fl) {
        Canvas c = backend.getCanvas();
        if (kl  != null) c.addKeyListener(kl);
        if (ml  != null) c.addMouseListener(ml);
        if (mml != null) c.addMouseMotionListener(mml);
        if (mwl != null) c.addMouseWheelListener(mwl);
        if (fl  != null) c.addFocusListener(fl);
    }

    public void addCanvasResizeListener(CanvasResizeListener l)    { resizeListeners.add(l);    }
    public void removeCanvasResizeListener(CanvasResizeListener l) { resizeListeners.remove(l); }

    public void addWindowLifecycleListener(WindowLifecycleListener l)    { lifecycleListeners.add(l);    }
    public void removeWindowLifecycleListener(WindowLifecycleListener l) { lifecycleListeners.remove(l); }

    /** Delegación a Backend para compatibilidad con el Pipeline. */
    public void requestCanvasFocus() { backend.requestCanvasFocus(); }

    /** Delegación a Backend. */
    public JFrame  getFrame()  { return backend.getFrame();  }
    /** Delegación a Backend. */
    public Canvas  getCanvas() { return backend.getCanvas(); }

    // ── Interfaces de callback ────────────────────────────────────────────────

    @FunctionalInterface
    public interface CanvasResizeListener {
        void onCanvasResized(int width, int height);
    }

    public interface WindowLifecycleListener {
        void onWindowSuspended();
        void onWindowResumed(boolean requiresRebuild);
    }
}
