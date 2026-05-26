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
 * dispose() destruye el peer nativo, invalida el Canvas y el BufferStrategy,
 * causando flickering, pérdida de foco y crashes.
 *
 * La transición segura es:
 *  1. setUndecorated() — requiere que la ventana NO sea visible
 *     (necesitamos setVisible(false) brevemente solo para esto)
 *  2. setFullScreenWindow() — el GraphicsDevice gestiona el modo
 *  3. setVisible(true) — restaurar visibilidad
 *
 * Para evitar parpadeo máximo:
 *  - NO hacer dispose()
 *  - NO recrear el Canvas
 *  - NO recrear el BufferStrategy (se recrea automáticamente via onResize)
 */
public class FullscreenManager {

    private static final Logger LOG = Logger.getLogger(FullscreenManager.class.getName());

    private final GraphicsDevice device;
    private boolean fullscreen;

    /**
     * @param monitorIndex índice del monitor (0 = principal)
     */
    public FullscreenManager(int monitorIndex) {
        GraphicsDevice[] devices =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        int idx = Math.max(0, Math.min(monitorIndex, devices.length - 1));
        this.device = devices[idx];
        this.fullscreen = false;

        LOG.info(() -> String.format(
            "FullscreenManager: usando monitor %d (%s), fullscreen exclusivo: %b",
            idx, device.getIDstring(), device.isFullScreenSupported()
        ));
    }

    /**
     * Activa fullscreen real en la ventana.
     *
     * Usa GraphicsDevice.setFullScreenWindow() que es el mecanismo correcto
     * de Java para fullscreen exclusivo — no MAXIMIZED_BOTH, que es
     * simplemente "ventana maximizada sin decoraciones".
     *
     * @param window la JFrame/Window a poner en fullscreen
     */
    public void enterFullscreen(Window window) {
        if (fullscreen) return;

        LOG.fine("Entrando a fullscreen...");

        if (device.isFullScreenSupported()) {
            // Fullscreen exclusivo real
            window.setVisible(false);
            trySetUndecorated(window, true);
            window.setVisible(true);

            device.setFullScreenWindow(window);
            fullscreen = true;

            LOG.fine("Fullscreen exclusivo activado.");
        } else {
            // Fallback: maximized + undecorated (no es fullscreen exclusivo
            // pero es lo mejor disponible en entornos sin soporte)
            LOG.warning("Fullscreen exclusivo no soportado — usando MAXIMIZED_BOTH");
            fallbackMaximize(window);
            fullscreen = true;
        }
    }

    /**
     * Sale de fullscreen y vuelve a modo ventana.
     *
     * @param window     la misma ventana pasada a enterFullscreen()
     * @param windowedW  ancho de ventana al restaurar
     * @param windowedH  alto de ventana al restaurar
     */
    public void exitFullscreen(Window window, int windowedW, int windowedH) {
        if (!fullscreen) return;

        LOG.fine("Saliendo de fullscreen...");

        if (device.isFullScreenSupported() && device.getFullScreenWindow() == window) {
            // Salir del fullscreen exclusivo ANTES de cambiar decoraciones
            device.setFullScreenWindow(null);
        }

        window.setVisible(false);
        trySetUndecorated(window, false);

        // Restaurar tamaño de ventana
        window.setSize(windowedW, windowedH);
        window.setLocationRelativeTo(null);
        window.setVisible(true);

        fullscreen = false;

        LOG.fine("Modo ventana restaurado.");
    }

    /**
     * Toggle: si está en fullscreen sale, si está en ventana entra.
     *
     * @param window     ventana objetivo
     * @param windowedW  ancho para restaurar en windowed
     * @param windowedH  alto para restaurar en windowed
     */
    public void toggle(Window window, int windowedW, int windowedH) {
        if (fullscreen) {
            exitFullscreen(window, windowedW, windowedH);
        } else {
            enterFullscreen(window);
        }
    }

    public boolean isFullscreen() {
        return fullscreen;
    }

    /**
     * El GraphicsDevice actual (útil para obtener DisplayMode, etc.).
     */
    public GraphicsDevice getDevice() {
        return device;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * setUndecorated() requiere que la ventana sea no-displayable.
     * En Java, para hacer eso SIN dispose() (que destruiría el canvas),
     * llamamos setVisible(false) previamente.
     *
     * IMPORTANTE: el Canvas y BufferStrategy sobreviven a esto.
     */
    private void trySetUndecorated(Window window, boolean undecorated) {
        if (window instanceof javax.swing.JFrame frame) {
            try {
                frame.setUndecorated(undecorated);
            } catch (IllegalComponentStateException e) {
                // Puede ocurrir en algunos LookAndFeel. Log y continuar.
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
}
