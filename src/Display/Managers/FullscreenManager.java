package Display.Managers;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Window;
import java.util.logging.Logger;

/**
 * Gestiona fullscreen REAL usando GraphicsDevice.setFullScreenWindow().
 *
 * REGLA CRÍTICA: NUNCA llamar frame.dispose() durante el toggle.
 * dispose() destruye el peer nativo, invalida el Canvas y el BufferStrategy.
 *
 * REGLA DE THREADING: todos los métodos públicos DEBEN llamarse desde el EDT.
 * DisplayManager.toggleFullscreen() lo garantiza via invokeLater().
 * assertEDT() lo verifica en desarrollo (-ea).
 *
 * ─── ORDEN CORRECTO DE OPERACIONES ───────────────────────────────────────────
 *
 *  enterFullscreen:
 *    setVisible(false) → setUndecorated(true) → setFullScreenWindow(frame)
 *    → setVisible(true)
 *
 *    setFullScreenWindow ANTES de setVisible(true) elimina el flash de ventana
 *    decorada/sin decorar en modo normal antes de entrar a fullscreen.
 *
 *  exitFullscreen:
 *    setVisible(false) → setFullScreenWindow(null) → setUndecorated(false)
 *    → setSize(windowed) → setLocationRelativeTo(null) → setVisible(true)
 *
 *    setVisible(false) primero evita que el WM vea la ventana sin borde
 *    brevemente antes de restaurar las decoraciones.
 *
 * ─── POR QUÉ NO HAY MÁS LÓGICA AQUÍ ─────────────────────────────────────────
 *
 *  La supresión de componentResized durante el toggle y el recreate del
 *  BufferStrategy post-toggle están en DisplayManager, que es quien coordina
 *  todos los subsistemas. FullscreenManager solo sabe operar la ventana.
 */
public class FullscreenManager {

    private static final Logger LOG = Logger.getLogger(FullscreenManager.class.getName());

    private final GraphicsDevice device;

    /**
     * volatile: el GameLoop puede consultar isFullscreen() desde su thread.
     * EDT lo escribe al final de enter/exit.
     */
    private volatile boolean fullscreen;

    public FullscreenManager(int monitorIndex) {
        GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        int idx = Math.max(0, Math.min(monitorIndex, devices.length - 1));
        this.device     = devices[idx];
        this.fullscreen = false;

        LOG.info(() -> String.format(
            "FullscreenManager: monitor %d (%s), fullscreen exclusivo: %b",
            idx, device.getIDstring(), device.isFullScreenSupported()
        ));
    }

    /**
     * Activa fullscreen real en la ventana dada.
     * Llamar DESDE EL EDT.
     */
    public void enterFullscreen(Window window) {
        if (fullscreen) return;
        assertEDT("enterFullscreen");

        LOG.fine("Entrando a fullscreen...");

        if (device.isFullScreenSupported()) {
            window.setVisible(false);
            trySetUndecorated(window, true);
            device.setFullScreenWindow(window);  // ANTES de setVisible(true)
            window.setVisible(true);
        } else {
            LOG.warning("Fullscreen exclusivo no soportado — usando MAXIMIZED_BOTH");
            fallbackMaximize(window);
        }

        fullscreen = true;
        LOG.fine("Fullscreen activado.");
    }

    /**
     * Sale de fullscreen y vuelve a modo ventana.
     * Llamar DESDE EL EDT.
     */
    public void exitFullscreen(Window window, int windowedW, int windowedH) {
        if (!fullscreen) return;
        assertEDT("exitFullscreen");

        LOG.fine("Saliendo de fullscreen...");

        window.setVisible(false);

        if (device.isFullScreenSupported() && device.getFullScreenWindow() == window) {
            device.setFullScreenWindow(null);
        }

        trySetUndecorated(window, false);
        window.setSize(windowedW, windowedH);
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        fullscreen = false;
        LOG.fine("Modo ventana restaurado.");
    }

    /**
     * Toggle: entra o sale según estado actual.
     * Llamar DESDE EL EDT (DisplayManager.toggleFullscreen() lo garantiza).
     */
    public void toggle(Window window, int windowedW, int windowedH) {
        if (fullscreen) {
            exitFullscreen(window, windowedW, windowedH);
        } else {
            enterFullscreen(window);
        }
    }

    /** @return true si actualmente está en fullscreen. */
    public boolean isFullscreen() {
        return fullscreen;
    }

    /** El GraphicsDevice actual. */
    public GraphicsDevice getDevice() {
        return device;
    }

    // ─── Helpers privados ─────────────────────────────────────────────────────

    private void trySetUndecorated(Window window, boolean undecorated) {
        if (window instanceof javax.swing.JFrame frame) {
            try {
                frame.setUndecorated(undecorated);
            } catch (IllegalComponentStateException e) {
                LOG.warning("No se pudo cambiar undecorated: " + e.getMessage());
            }
        }
    }

    private void fallbackMaximize(Window window) {
        if (window instanceof javax.swing.JFrame frame) {
            window.setVisible(false);
            frame.setUndecorated(true);
            frame.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
            window.setVisible(true);
        }
    }

    /**
     * Verifica que estamos en el EDT.
     * Con -ea: lanza AssertionError. Sin -ea: solo loguea WARNING.
     */
    private void assertEDT(String method) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            String msg = "FullscreenManager." + method +
                         "() llamado desde thread no-EDT: " +
                         Thread.currentThread().getName() +
                         ". Usa DisplayManager.toggleFullscreen() que garantiza invokeLater().";
            LOG.warning(msg);
            assert false : msg;
        }
    }
}
