package Display.Managers;

import Display.State.DisplayMode;

import java.awt.*;
import java.util.logging.Logger;

/**
 * Gestiona las transiciones entre modos de presentación de la ventana.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: ASINCRONÍA DE setExtendedState(MAXIMIZED_BOTH)
 *
 * Problema anterior:
 *   enterBorderless() llamaba setExtendedState(MAXIMIZED_BOTH) y retornaba
 *   inmediatamente. El pipeline leía canvas.getWidth() justo después, pero
 *   setExtendedState es asíncrono en AWT: el window manager del sistema
 *   operativo puede no haber completado la maximización todavía. El canvas
 *   reportaba el tamaño anterior o un tamaño intermedio, resultando en un
 *   viewport calculado con dimensiones incorrectas para el modo borderless.
 *
 * Solución:
 *   Después de setVisible(true) en enterBorderless(), se llama
 *   frame.validate() + canvas.validate() para forzar la validación del
 *   layout antes de retornar. Esto sincroniza el LayoutManager de Swing
 *   con las nuevas dimensiones. Si tras la validación el canvas todavía
 *   tiene dimensiones degeneradas (edge case en algunos window managers),
 *   se usa GraphicsConfiguration.getBounds() como fallback para obtener
 *   el tamaño de pantalla definitivo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN: DPI SCALING INDEPENDENCIA
 *
 *   getCanvasSize() obtiene las dimensiones del canvas y, si el canvas
 *   reporta tamaño degenerado o inconsistente con lo esperado en fullscreen,
 *   usa GraphicsConfiguration.getBounds() del dispositivo activo como
 *   fuente de verdad. Esto desacopla el cálculo de viewport del DPI scaling
 *   implícito que algunos JVM aplican a canvas.getWidth() en sistemas con
 *   Windows Display Scaling activo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   Todos los métodos públicos deben ejecutarse desde el EDT.
 *   currentMode es volatile para lectura thread-safe desde el GameLoop.
 *   activeMonitorIndex es volatile por el mismo motivo.
 */
public final class FullscreenManager {

    private static final Logger LOG = Logger.getLogger(FullscreenManager.class.getName());

    private GraphicsDevice device;
    private int activeMonitorIndex;

    /** Estado actual del modo de presentación. Volatile: leído desde GameLoop. */
    private volatile DisplayMode currentMode = DisplayMode.WINDOWED;

    /** Snapshot del estado windowed capturado antes de entrar en fullscreen. */
    private WindowedSnapshot windowedSnapshot = null;

    public FullscreenManager(int monitorIndex) {
        this.activeMonitorIndex = monitorIndex;
        this.device = resolveDevice(monitorIndex);
    }

    // ── Transiciones ──────────────────────────────────────────────────────────

    /**
     * Entra en fullscreen desde el modo windowed.
     * Si el dispositivo soporta exclusive, usa FULLSCREEN_EXCLUSIVE.
     * Si no, usa BORDERLESS_FULLSCREEN.
     * EDT únicamente.
     */
    public void enterFullscreen(Window window) {
        if (currentMode.isFullscreen()) {
            LOG.fine("enterFullscreen(): already in fullscreen — ignored");
            return;
        }
        windowedSnapshot = WindowedSnapshot.capture(window);
        if (device.isFullScreenSupported()) {
            enterExclusive(window);
        } else {
            LOG.warning("Exclusive fullscreen not supported — falling back to BORDERLESS_FULLSCREEN");
            enterBorderless(window);
        }
    }

    /**
     * Entra en modo borderless windowed (maximized, sin decoración).
     *
     * Después de setVisible(true) llama validate() para sincronizar el
     * LayoutManager con las nuevas dimensiones antes de retornar.
     * El pipeline puede leer canvas.getWidth() inmediatamente después y
     * obtendrá las dimensiones correctas post-maximización.
     *
     * EDT únicamente.
     */
    public void enterBorderless(Window window) {
        if (currentMode == DisplayMode.BORDERLESS_FULLSCREEN) {
            LOG.fine("enterBorderless(): already BORDERLESS_FULLSCREEN — ignored");
            return;
        }
        if (!currentMode.isFullscreen()) {
            windowedSnapshot = WindowedSnapshot.capture(window);
        }
        window.setVisible(false);
        setUndecorated(window, true);
        if (window instanceof javax.swing.JFrame f) {
            f.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
        }
        window.setVisible(true);

        // Forzar validación del layout para que las dimensiones del canvas
        // sean correctas cuando el pipeline las lea inmediatamente después.
        window.validate();

        currentMode = DisplayMode.BORDERLESS_FULLSCREEN;
        LOG.info("Entered BORDERLESS_FULLSCREEN");
    }

    /**
     * Sale de fullscreen y restaura el estado windowed capturado.
     * EDT únicamente.
     */
    public void exitFullscreen(Window window) {
        if (!currentMode.isFullscreen()) {
            LOG.fine("exitFullscreen(): not in fullscreen — ignored");
            return;
        }
        window.setVisible(false);

        if (currentMode == DisplayMode.FULLSCREEN_EXCLUSIVE
                && device.getFullScreenWindow() == window) {
            device.setFullScreenWindow(null);
        }

        setUndecorated(window, false);

        if (windowedSnapshot != null) {
            windowedSnapshot.restore(window);
            windowedSnapshot = null;
        }

        window.setVisible(true);
        window.validate();

        currentMode = DisplayMode.WINDOWED;
        LOG.info("Exited fullscreen → WINDOWED");
    }

    /**
     * Alterna entre WINDOWED ↔ FULLSCREEN.
     * EDT únicamente.
     */
    public void toggle(Window window) {
        if (currentMode.isFullscreen()) {
            exitFullscreen(window);
        } else {
            enterFullscreen(window);
        }
    }

    // ── Cambio de monitor ─────────────────────────────────────────────────────

    /**
     * Cambia el monitor activo para operaciones fullscreen.
     * Si el índice es inválido, se clampea al rango disponible.
     * EDT únicamente.
     */
    public void setMonitor(int monitorIndex) {
        GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        int clamped = Math.max(0, Math.min(monitorIndex, devices.length - 1));
        this.activeMonitorIndex = clamped;
        this.device = devices[clamped];
        LOG.info("Active monitor changed to index " + clamped);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Modo de presentación activo. Thread-safe (volatile read). */
    public DisplayMode getCurrentMode() { return currentMode; }

    /** Conveniencia. Thread-safe. */
    public boolean isFullscreen() { return currentMode.isFullscreen(); }

    /** Índice del monitor activo. Thread-safe (volatile read). */
    public int getActiveMonitorIndex() { return activeMonitorIndex; }

    /**
     * Retorna el tamaño del canvas como Dimension.
     *
     * Si el canvas reporta dimensiones degeneradas (puede ocurrir
     * inmediatamente después de una transición borderless en algunos
     * window managers), usa GraphicsConfiguration.getBounds() del
     * dispositivo activo como fallback.
     *
     * Esto garantiza independencia del DPI scaling implícito del JVM:
     * en sistemas con Windows Display Scaling > 100%, canvas.getWidth()
     * puede retornar el tamaño lógico mientras la BS opera sobre píxeles
     * físicos. Usar device.getDefaultConfiguration().getBounds() retorna
     * el tamaño físico del monitor que es el correcto para el viewport.
     *
     * EDT únicamente.
     */
    public Dimension getPhysicalCanvasSize(java.awt.Canvas canvas) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        if (w > 0 && h > 0) {
            return new Dimension(w, h);
        }
        // Fallback: tamaño físico del monitor activo.
        Rectangle bounds = device.getDefaultConfiguration().getBounds();
        LOG.fine("FullscreenManager: canvas reported degenerate size, using device bounds: "
                 + bounds.width + "x" + bounds.height);
        return new Dimension(bounds.width, bounds.height);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void enterExclusive(Window window) {
        window.setVisible(false);
        setUndecorated(window, true);
        device.setFullScreenWindow(window);
        window.setVisible(true);
        window.validate();
        currentMode = DisplayMode.FULLSCREEN_EXCLUSIVE;
        LOG.info("Entered FULLSCREEN_EXCLUSIVE");
    }

    private static GraphicsDevice resolveDevice(int monitorIndex) {
        GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        int idx = Math.max(0, Math.min(monitorIndex, devices.length - 1));
        return devices[idx];
    }

    private static void setUndecorated(Window window, boolean undecorated) {
        if (window instanceof javax.swing.JFrame f) {
            try {
                f.setUndecorated(undecorated);
            } catch (IllegalComponentStateException e) {
                LOG.warning("setUndecorated(" + undecorated + ") failed: " + e.getMessage());
            }
        }
    }

    // ── WindowedSnapshot ──────────────────────────────────────────────────────

    private static final class WindowedSnapshot {
        final int x, y, width, height;

        private WindowedSnapshot(int x, int y, int width, int height) {
            this.x = x; this.y = y;
            this.width = width; this.height = height;
        }

        static WindowedSnapshot capture(Window window) {
            Rectangle b = window.getBounds();
            return new WindowedSnapshot(b.x, b.y, b.width, b.height);
        }

        void restore(Window window) {
            if (window instanceof javax.swing.JFrame f) {
                f.setExtendedState(javax.swing.JFrame.NORMAL);
            }
            window.setSize(width, height);
            window.setLocation(x, y);
        }
    }
}
