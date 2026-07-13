package Display.Managers;

import Display.Backend.AwtWindowBackend;
import Display.State.DisplayMode;
import java.util.logging.Logger;

/**
 * Coordina transiciones entre modos de presentación de la ventana.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-003: DELEGACIÓN COMPLETA AL BACKEND
 *
 * Antes del HRFC-003, FullscreenManager mantenía un campo volatile
 * {@code currentMode} que actualizaba al finalizar cada solicitud de
 * transición. Ese campo era una suposición interna: el Engine asumía que
 * la operación había tenido éxito porque había sido solicitada.
 *
 * Ahora FullscreenManager ya no mantiene ningún estado de modo propio.
 * Toda consulta del modo actual se delega a AwtWindowBackend.deriveCurrentMode(),
 * que lee el estado real confirmado por AWT en el instante de la consulta.
 *
 * Responsabilidades de FullscreenManager:
 *   1. Decidir QUÉ tipo de fullscreen corresponde (exclusive vs borderless)
 *      según las capacidades del device.
 *   2. Delegar la solicitud al Backend.
 *   3. Mantener el índice del monitor activo (entero puro, no estado AWT).
 *
 * El Pipeline llama readSnapshot() después de cualquier transición para
 * conocer el estado real. FullscreenManager no interviene en esa lectura.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   Todos los métodos → EDT únicamente (delegan en Backend que es EDT-only).
 *   getActiveMonitorIndex() → thread-safe (volatile int).
 *   getCurrentMode() → hilo del llamador; delega en Backend.deriveCurrentMode()
 *                      que es EDT-only. Usar solo desde el EDT.
 */
public final class FullscreenManager {

    private static final Logger LOG =
        Logger.getLogger(FullscreenManager.class.getName());

    private final AwtWindowBackend backend;

    /**
     * Índice del monitor activo para operaciones fullscreen.
     * Volatile: puede leerse desde cualquier thread para diagnóstico.
     */
    private volatile int activeMonitorIndex;

    public FullscreenManager(AwtWindowBackend backend, int monitorIndex) {
        this.backend            = backend;
        this.activeMonitorIndex = monitorIndex;
    }

    // ── Transiciones ──────────────────────────────────────────────────────────

    /**
     * Solicita entrar en fullscreen desde el modo windowed.
     *
     * Delega en el Backend que elige exclusive vs borderless según el device.
     * El resultado real se confirma leyendo el snapshot después de la llamada.
     *
     * EDT únicamente.
     */
    public void enterFullscreen() {
        LOG.fine("FullscreenManager: requesting enterFullscreen");
        backend.requestEnterFullscreen();
    }

    /**
     * Solicita entrar en modo borderless windowed.
     *
     * EDT únicamente.
     */
    public void enterBorderless() {
        LOG.fine("FullscreenManager: requesting enterBorderless");
        backend.requestEnterBorderless();
    }

    /**
     * Solicita salir de fullscreen y restaurar el estado windowed.
     *
     * EDT únicamente.
     */
    public void exitFullscreen() {
        LOG.fine("FullscreenManager: requesting exitFullscreen");
        backend.requestExitFullscreen();
    }

    /**
     * Alterna entre WINDOWED y FULLSCREEN.
     *
     * EDT únicamente.
     */
    public void toggle() {
        LOG.fine("FullscreenManager: requesting toggleFullscreen");
        backend.requestToggleFullscreen();
    }

    // ── Cambio de monitor ─────────────────────────────────────────────────────

    /**
     * Cambia el monitor activo. El cambio toma efecto en la próxima
     * transición fullscreen.
     *
     * EDT únicamente.
     */
    public void setMonitor(int monitorIndex) {
        backend.requestSetMonitor(monitorIndex);
        this.activeMonitorIndex = monitorIndex;
        LOG.info("FullscreenManager: active monitor index set to " + monitorIndex);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    /**
     * Modo de presentación actual, derivado del estado real de AWT.
     *
     * Delega en AwtWindowBackend.deriveCurrentMode() — la única fuente
     * válida del modo confirmado. No hay campo currentMode interno.
     *
     * EDT únicamente (deriveCurrentMode() lee objetos AWT).
     */
    public DisplayMode getCurrentMode() {
        return backend.deriveCurrentMode();
    }

    /**
     * True si actualmente se está en algún modo fullscreen, según AWT.
     * EDT únicamente.
     */
    public boolean isFullscreen() {
        return backend.deriveCurrentMode().isFullscreen();
    }

    /**
     * Índice del monitor activo. Thread-safe (volatile read).
     */
    public int getActiveMonitorIndex() {
        return activeMonitorIndex;
    }
}
