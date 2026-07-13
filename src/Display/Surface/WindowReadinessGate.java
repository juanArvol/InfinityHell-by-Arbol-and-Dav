package Display.Surface;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Barrera explícita de readiness para el GameLoop.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * PROPÓSITO
 *
 * Antes del HRFC-002, la única barrera que el GameLoop tenía para saber si
 * podía renderizar era publishedRef != null en SurfacePublisher. Esto era
 * implícito: durante una transición el pipeline llamaba unpublish() (null)
 * y el GameLoop hacía drop silencioso. Al finalizar la transición, publish()
 * restauraba la referencia.
 *
 * El mecanismo implícito tiene dos debilidades:
 *
 *   1. No distingue entre "no hay surface todavía" y "la surface está siendo
 *      reconstruida" y "la ventana está suspendida". Para SUSPENDED (Alt+Tab)
 *      no queremos destruir la surface, pero sí queremos que el GameLoop
 *      descarte frames. publishedRef != null no puede expresar esto sin
 *      destruir la BS.
 *
 *   2. Si buildAndPublish() falla (build retorna null), publishedRef queda
 *      null permanentemente. El GameLoop descarta frames para siempre sin
 *      que haya nadie que lo corrija: el mecanismo de recuperación
 *      (notifyContentLost) nunca dispara porque endPresent() nunca se llama.
 *
 * WindowReadinessGate resuelve ambos problemas:
 *
 *   - La gate es un AtomicReference<GateState> con dos valores: OPEN / CLOSED.
 *   - acquireFrame() solo construye un frame si la gate está OPEN.
 *   - open() / close() los llama el pipeline en el EDT.
 *   - Para SUSPENDED: la pipeline cierra la gate sin tocar publishedRef.
 *     La surface sigue publicada; si la BS sigue válida al reabrir, no hay
 *     coste de reconstrucción.
 *   - Para FAILED/LOST: la pipeline cierra la gate Y vacía publishedRef.
 *     El mecanismo de recuperación (onRecoveryNeeded) dispara desde aquí.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 *   open() / close()           → EDT únicamente (desde el pipeline).
 *   isOpen()                   → cualquier thread (AtomicReference, lock-free).
 *   acquireFrame() usa isOpen() → GameLoop thread (lock-free).
 *
 * La escritura (EDT) y la lectura (GameLoop) son atómicas por AtomicReference.
 * No hay bloqueo ni espera en el GameLoop: si la gate está cerrada, retorna
 * null inmediatamente (drop silencioso).
 */
public final class WindowReadinessGate {

    private static final Logger LOG = Logger.getLogger(WindowReadinessGate.class.getName());

    private enum GateState { OPEN, CLOSED }

    /**
     * Estado actual de la gate.
     *
     * AtomicReference garantiza visibilidad cross-thread sin locks.
     * La gate comienza CLOSED: se abre solo cuando el pipeline publica
     * el primer estado READY en DisplayManager.init().
     */
    private final AtomicReference<GateState> state =
        new AtomicReference<>(GateState.CLOSED);

    // ── API para el pipeline (EDT) ────────────────────────────────────────────

    /**
     * Abre la gate. El GameLoop puede adquirir frames en el próximo ciclo.
     *
     * Llamar desde el pipeline después de publicar una surface READY.
     * EDT únicamente.
     */
    public void open() {
        GateState prev = state.getAndSet(GateState.OPEN);
        if (prev != GateState.OPEN) {
            LOG.fine("WindowReadinessGate: opened");
        }
    }

    /**
     * Cierra la gate. El GameLoop descartará frames desde el próximo ciclo.
     *
     * Llamar desde el pipeline antes de iniciar cualquier transición,
     * al suspender (Alt+Tab, iconify) o al detectar un estado inválido.
     * EDT únicamente.
     */
    public void close() {
        GateState prev = state.getAndSet(GateState.CLOSED);
        if (prev != GateState.CLOSED) {
            LOG.fine("WindowReadinessGate: closed");
        }
    }

    // ── API para el GameLoop (cualquier thread) ───────────────────────────────

    /**
     * True si el GameLoop puede adquirir un frame en este ciclo.
     *
     * Lock-free. Llamar desde el GameLoop thread antes de acquireFrame().
     * Si retorna false, el GameLoop debe hacer drop silencioso inmediatamente
     * sin intentar acceder a ningún recurso gráfico.
     */
    public boolean isOpen() {
        return state.get() == GateState.OPEN;
    }

    @Override
    public String toString() {
        return "WindowReadinessGate[" + state.get() + "]";
    }
}
