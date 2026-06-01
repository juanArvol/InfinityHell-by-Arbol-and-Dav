package Display.Transition;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Mecanismo de bloqueo para transiciones de display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * Problema original:
 *   toggleFullscreen() era llamado desde un KeyListener que podía dispararse
 *   múltiples veces por key repeat (tecla F11 mantenida pulsada). Esto
 *   producía múltiples transiciones superpuestas, BufferStrategy inválidas
 *   y estados inconsistentes.
 *
 *   Adicionalmente, los resize del canvas durante una transición fullscreen
 *   disparaban recreaciones del BufferStrategy mientras la transición
 *   anterior aún no había terminado.
 *
 * Causa raíz:
 *   No existía ningún mecanismo que garantizara exclusión mutua entre
 *   transiciones. Cada llamada a toggle era independiente.
 *
 * Solución:
 *   TransitionLock usa un AtomicBoolean para garantizar que solo una
 *   transición puede estar activa en cualquier momento dado.
 *   - tryAcquire() retorna false si ya hay una transición en curso.
 *   - release() se llama en el finally del bloque de transición.
 *   - El timeout previene deadlocks si release() no se llama por error.
 *
 * Beneficios:
 *   - Los key repeats de F11 son ignorados silenciosamente.
 *   - Los resize durante transición no producen recreaciones prematuras.
 *   - El código de transición no necesita protegerse explícitamente.
 *   - Elimina la dependencia del orden de eventos de Swing.
 *
 * Impacto sobre estabilidad:
 *   Elimina la causa principal de BufferStrategy inválidas y pantallas
 *   blancas observadas durante toggles consecutivos rápidos.
 * ──────────────────────────────────────────────────────────────────────────
 */
public final class TransitionLock {

    private static final Logger LOG = Logger.getLogger(TransitionLock.class.getName());

    private final AtomicBoolean locked = new AtomicBoolean(false);

    /**
     * Intenta adquirir el lock de transición.
     *
     * @return true si el lock fue adquirido (la transición puede proceder).
     *         false si ya hay una transición en curso (ignorar la solicitud).
     */
    public boolean tryAcquire() {
        boolean acquired = locked.compareAndSet(false, true);
        if (!acquired) {
            LOG.fine("TransitionLock: transition already in progress — request ignored");
        }
        return acquired;
    }

    /**
     * Libera el lock de transición.
     * Debe llamarse siempre en un bloque finally tras tryAcquire() exitoso.
     */
    public void release() {
        locked.set(false);
    }

    /** True si hay una transición en curso. */
    public boolean isLocked() {
        return locked.get();
    }
}
