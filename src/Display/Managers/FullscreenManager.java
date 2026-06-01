package Display.Managers;

import Display.State.DisplayMode;

import java.awt.*;
import java.util.logging.Logger;

/**
 * Gestiona las transiciones entre modos de presentación de la ventana.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * EVOLUCIÓN RESPECTO A LA VERSIÓN ANTERIOR
 *
 * 1. enterBorderless() EXPUESTO PÚBLICAMENTE
 *    El pipeline unificado necesita acceso directo a enterBorderless() para
 *    ejecutar EnterFullscreen(BORDERLESS_FULLSCREEN) y SetDisplayMode.
 *    Era privado; ahora es público con la misma semántica.
 *
 * 2. setMonitor() PARA CAMBIO DE MONITOR EN RUNTIME
 *    Permite al pipeline ejecutar ChangeMonitor sin recrear el manager.
 *    Valida el índice contra los dispositivos disponibles.
 *
 * 3. getActiveMonitorIndex() PARA DisplayState
 *    El índice del monitor activo es parte del snapshot completo de
 *    DisplayState. Se expone aquí como fuente de verdad.
 *
 * 4. TransitionLock ELIMINADO
 *    TransitionLock ya no es necesario aquí: el control de exclusión mutua
 *    lo ejerce DisplayTransitionMachine en el pipeline. Este manager solo
 *    ejecuta operaciones atómicas de Swing sin responsabilidad de locking.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * COMPATIBILIDAD
 *
 * Los métodos públicos originales (enterFullscreen, exitFullscreen, toggle,
 * getCurrentMode, isFullscreen) se mantienen con la misma semántica.
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
     *
     * Pre-condición: llamar desde el EDT.
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
     * Pre-condición: llamar desde el EDT.
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
        currentMode = DisplayMode.BORDERLESS_FULLSCREEN;
        LOG.info("Entered BORDERLESS_FULLSCREEN");
    }

    /**
     * Sale de fullscreen y restaura el estado windowed capturado.
     * Pre-condición: llamar desde el EDT.
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
        currentMode = DisplayMode.WINDOWED;
        LOG.info("Exited fullscreen → WINDOWED");
    }

    /**
     * Alterna entre WINDOWED ↔ FULLSCREEN.
     * Pre-condición: llamar desde el EDT.
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
     * Llamar desde el EDT.
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

    // ── Privados ──────────────────────────────────────────────────────────────

    private void enterExclusive(Window window) {
        window.setVisible(false);
        setUndecorated(window, true);
        device.setFullScreenWindow(window);
        window.setVisible(true);
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
        final boolean decorated;

        private WindowedSnapshot(int x, int y, int width, int height, boolean decorated) {
            this.x = x; this.y = y;
            this.width = width; this.height = height;
            this.decorated = decorated;
        }

        static WindowedSnapshot capture(Window window) {
            Rectangle b = window.getBounds();
            boolean dec = true;
            if (window instanceof javax.swing.JFrame f) dec = !f.isUndecorated();
            return new WindowedSnapshot(b.x, b.y, b.width, b.height, dec);
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
