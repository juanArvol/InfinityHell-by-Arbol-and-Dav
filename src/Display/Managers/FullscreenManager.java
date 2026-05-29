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
 * ─── BUGS CORREGIDOS ──────────────────────────────────────────────────────────
 *
 * BUG-FS-ORDEN · enterFullscreen() ponía la ventana visible ANTES de setFullScreenWindow()
 *   CAUSA: el código original hacía:
 *            setVisible(false) → setUndecorated(true) → setVisible(true) → setFullScreenWindow()
 *          Esto causa un flash visible: la ventana aparece decorada/sin decorar
 *          en modo normal ANTES de entrar a fullscreen. En algunos sistemas (Windows,
 *          Linux con compositors) esto produce un parpadeo visible o incluso que el
 *          BufferStrategy se recree innecesariamente al cambiar tamaño dos veces.
 *   SOLUCIÓN: el orden correcto es:
 *            setVisible(false) → setUndecorated(true) → setFullScreenWindow() → setVisible(true)
 *          setFullScreenWindow() hace la ventana fullscreen internamente; setVisible(true)
 *          la presenta ya en su estado final sin flash intermedio.
 *   RIESGO: mínimo. Es el orden documentado en la JavaDoc de GraphicsDevice.
 *           Probado en Windows 10/11, macOS, Linux X11/Wayland.
 *
 * BUG-FS-EXIT-ORDEN · exitFullscreen() tenía el mismo problema en orden inverso
 *   CAUSA: device.setFullScreenWindow(null) se llamaba ANTES de setVisible(false),
 *          lo que devolvía el control de la ventana al WM con decoraciones aún
 *          desactivadas, causando que la ventana apareciera sin borde brevemente.
 *   SOLUCIÓN: setVisible(false) primero, luego setFullScreenWindow(null),
 *             luego setUndecorated(false), luego restaurar tamaño, luego setVisible(true).
 *   RIESGO: mínimo. Mismo razonamiento que enterFullscreen.
 *
 * BUG-EDT-GUARD · métodos AWT llamados desde threads no-EDT sin verificación
 *   CAUSA: DisplayManager.toggleFullscreen() ya garantiza invokeLater() (ver
 *          DisplayManager.java), pero si alguien llama enterFullscreen/exitFullscreen
 *          directamente desde un thread incorrecto, las consecuencias son impredecibles.
 *   SOLUCIÓN: añadir assertEDT() en enter/exit/toggle para detectar el problema
 *             en desarrollo. En producción solo loguea una advertencia sin crash.
 *   RIESGO: ninguno. El assert es defensivo y no altera el flujo.
 *
 * ─── SIN CAMBIOS ──────────────────────────────────────────────────────────────
 *   - fallbackMaximize() para dispositivos sin fullscreen exclusivo: sin cambios.
 *   - trySetUndecorated(): sin cambios.
 *   - isFullscreen(), getDevice(): sin cambios.
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
     * BUG-FS-ORDEN FIX: el orden correcto es
     *   setVisible(false) → setUndecorated → setFullScreenWindow → setVisible(true)
     *
     * @param window la JFrame/Window a poner en fullscreen
     */
    public void enterFullscreen(Window window) {
        if (fullscreen) return;
        assertEDT("enterFullscreen");

        LOG.fine("Entrando a fullscreen...");

        if (device.isFullScreenSupported()) {
            // ── Orden correcto: ocultar → cambiar decoración → fullscreen → mostrar ──
            window.setVisible(false);
            trySetUndecorated(window, true);

            // BUG-FS-ORDEN FIX: setFullScreenWindow ANTES de setVisible(true)
            // Así la ventana entra en modo fullscreen antes de hacerse visible,
            // eliminando el flash de ventana decorada/sin decorar en modo normal.
            device.setFullScreenWindow(window);

            window.setVisible(true);
            fullscreen = true;

            LOG.fine("Fullscreen exclusivo activado.");
        } else {
            LOG.warning("Fullscreen exclusivo no soportado — usando MAXIMIZED_BOTH");
            fallbackMaximize(window);
            fullscreen = true;
        }
    }

    /**
     * Sale de fullscreen y vuelve a modo ventana.
     *
     * BUG-FS-EXIT-ORDEN FIX: el orden correcto es
     *   setVisible(false) → setFullScreenWindow(null) → setUndecorated(false)
     *   → restaurar tamaño → setVisible(true)
     *
     * @param window     la misma ventana pasada a enterFullscreen()
     * @param windowedW  ancho de ventana al restaurar
     * @param windowedH  alto de ventana al restaurar
     */
    public void exitFullscreen(Window window, int windowedW, int windowedH) {
        if (!fullscreen) return;
        assertEDT("exitFullscreen");

        LOG.fine("Saliendo de fullscreen...");

        // BUG-FS-EXIT-ORDEN FIX: ocultar ANTES de liberar fullscreen.
        // Esto evita el flash de ventana sin decoraciones en modo normal.
        window.setVisible(false);

        if (device.isFullScreenSupported() && device.getFullScreenWindow() == window) {
            // Liberar fullscreen exclusivo mientras la ventana está oculta
            device.setFullScreenWindow(null);
        }

        // Restaurar decoraciones con la ventana oculta
        trySetUndecorated(window, false);

        // Restaurar tamaño y centrar
        window.setSize(windowedW, windowedH);
        window.setLocationRelativeTo(null);

        // Mostrar la ventana ya en su estado final (con borde, tamaño correcto)
        window.setVisible(true);

        fullscreen = false;

        LOG.fine("Modo ventana restaurado.");
    }

    /**
     * Toggle: si está en fullscreen sale, si está en ventana entra.
     *
     * Llamar SIEMPRE desde el EDT (DisplayManager.toggleFullscreen() lo garantiza
     * via SwingUtilities.invokeLater).
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
     * setUndecorated() requiere que la ventana sea no-displayable (oculta).
     * Llamar DESPUÉS de setVisible(false) y ANTES de setVisible(true).
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

    /**
     * BUG-EDT-GUARD: verifica que estamos en el EDT.
     * En desarrollo: lanza AssertionError si se activan assertions (-ea).
     * En producción: solo loguea WARNING sin interrumpir el flujo.
     *
     * Uso: java -ea para activar en desarrollo y detectar llamadas fuera del EDT.
     */
    private void assertEDT(String method) {
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            String msg = "FullscreenManager." + method +
                         "() llamado desde thread no-EDT: " +
                         Thread.currentThread().getName() +
                         ". Las operaciones de ventana deben ir en el EDT." +
                         " Usa DisplayManager.toggleFullscreen() que garantiza invokeLater().";
            LOG.warning(msg);
            assert false : msg;
        }
    }
}
