package Display.Transition;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Máquina de estados formal para las transiciones del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * El mecanismo anterior (TransitionLock) usaba un boolean atómico que
 * garantizaba exclusión mutua pero no sabía QUÉ transición estaba en curso.
 * DisplayTransitionMachine reemplaza ese boolean por un enum atómico
 * ({@link DisplayTransitionState}), obteniendo:
 *
 *   1. Conocer exactamente qué operación está ejecutándose.
 *   2. Impedir transiciones incompatibles simultáneas con semántica clara.
 *   3. Trazas de diagnóstico con información real del estado.
 *   4. Extensibilidad: agregar un nuevo tipo requiere solo un nuevo enum value.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * INVARIANTE
 *
 *   - Solo puede haber un estado activo a la vez.
 *   - Toda transición DEBE comenzar con tryBegin() y terminar con end().
 *   - end() debe llamarse en un bloque finally para garantizar retorno a IDLE.
 *   - IDLE es el único estado desde el que puede iniciarse cualquier transición.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREADING
 *
 * AtomicReference garantiza que tryBegin() es libre de carreras.
 * tryBegin() y end() son seguros desde cualquier thread.
 * El estado puede leerse desde cualquier thread sin sincronización adicional.
 */
public final class DisplayTransitionMachine {

    private static final Logger LOG = Logger.getLogger(DisplayTransitionMachine.class.getName());

    /**
     * Estado actual de transición. AtomicReference garantiza CAS atómico
     * para la adquisición sin necesidad de synchronized.
     */
    private final AtomicReference<DisplayTransitionState> state =
        new AtomicReference<>(DisplayTransitionState.IDLE);

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Intenta iniciar una transición del tipo dado.
     *
     * Solo tiene éxito si el estado actual es IDLE.
     * Si ya hay una transición activa, retorna false y la solicitud se descarta.
     *
     * Llamar desde cualquier thread. CAS atómico: no hay carrera.
     *
     * @param transition  tipo de transición que se desea iniciar
     * @return true si la transición fue adquirida; false si ya hay una activa
     */
    public boolean tryBegin(DisplayTransitionState transition) {
        if (transition == DisplayTransitionState.IDLE) {
            throw new IllegalArgumentException("Cannot begin an IDLE transition");
        }
        boolean acquired = state.compareAndSet(DisplayTransitionState.IDLE, transition);
        if (!acquired) {
            LOG.fine("DisplayTransitionMachine: transition " + transition
                     + " rejected — " + state.get() + " already in progress");
        } else {
            LOG.fine("DisplayTransitionMachine: began " + transition);
        }
        return acquired;
    }

    /**
     * Finaliza la transición activa y vuelve a IDLE.
     *
     * DEBE llamarse en un bloque finally tras tryBegin() exitoso.
     * No lanza excepción si ya está en IDLE (idempotente por seguridad).
     */
    public void end() {
        DisplayTransitionState previous = state.getAndSet(DisplayTransitionState.IDLE);
        if (previous != DisplayTransitionState.IDLE) {
            LOG.fine("DisplayTransitionMachine: ended " + previous + " → IDLE");
        }
    }

    /**
     * Finaliza la transición activa si coincide con la esperada.
     * Para diagnóstico más preciso cuando se conoce el tipo en el finally.
     */
    public void end(DisplayTransitionState expected) {
        DisplayTransitionState current = state.get();
        if (current != expected && current != DisplayTransitionState.IDLE) {
            LOG.warning("DisplayTransitionMachine.end(): expected " + expected
                        + " but current state is " + current + " — forcing IDLE");
        }
        end();
    }

    // ── Consultas ────────────────────────────────────────────────────────────

    /** Estado de transición actual. Thread-safe. */
    public DisplayTransitionState getState() {
        return state.get();
    }

    /** True si hay alguna transición activa. Thread-safe. */
    public boolean isTransitionActive() {
        return state.get().isActive();
    }

    /** True si la transición activa es del tipo dado. Thread-safe. */
    public boolean isInTransition(DisplayTransitionState type) {
        return state.get() == type;
    }

    @Override
    public String toString() {
        return "DisplayTransitionMachine[" + state.get() + "]";
    }
}
