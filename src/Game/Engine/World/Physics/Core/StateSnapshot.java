package Game.Engine.World.Physics.Core;

/**
 * Contenedor genérico de dos instantáneas de un estado de dominio.
 *
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * StateSnapshot mantiene el estado actual y el estado anterior de cualquier
 * dominio de simulación. Es el reemplazante directo de todos los campos
 * históricos específicos (previousSpeed, previousMomentum, etc.) que de
 * otro modo proliferarían en los puentes y componentes del sistema.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * El historial de un estado no es responsabilidad del estado en sí.
 * El historial tampoco es responsabilidad del puente ni del intérprete.
 * Es responsabilidad de este contenedor genérico, compartido por todos los
 * dominios que necesiten acceso a la variación temporal de sus magnitudes.
 *
 * ── USO TÍPICO ────────────────────────────────────────────────────────────
 *
 *   // Construcción inicial (sin estado anterior conocido):
 *   StateSnapshot<KinematicState> snapshot = StateSnapshot.initial(firstState);
 *
 *   // Avanzar al siguiente frame:
 *   snapshot = snapshot.advance(newState);
 *
 *   // Lectura:
 *   KinematicState current  = snapshot.current();
 *   KinematicState previous = snapshot.previous();
 *
 *   // Ejemplo de magnitud derivada temporal:
 *   double deltaVelocity = snapshot.current().getVelocity()
 *                        - snapshot.previous().getVelocity();
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * StateSnapshot es completamente inmutable. advance() produce una nueva
 * instancia — nunca muta la existente.
 *
 * Esto garantiza que cualquier código que tenga una referencia al snapshot
 * anterior no ve cambios inesperados.
 *
 * ── ESTADO NULL / VACÍO ──────────────────────────────────────────────────
 * Cuando no existe estado anterior (primer frame), previous() retorna el
 * mismo estado que current(). Esto evita NPE en evaluadores que calculan
 * deltas: el delta será 0 en el primer frame, que es el comportamiento
 * correcto (sin historial real, sin cambio aparente).
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Funciona con cualquier tipo T. No requiere implementar ninguna interfaz.
 * Ejemplos futuros:
 *   StateSnapshot<KinematicState>
 *   StateSnapshot<PhysicalState>   (cuando PhysicalState sea inmutable)
 *   StateSnapshot<MaterialState>
 *   StateSnapshot<EnvironmentState>
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 *
 * @param <T> el tipo del estado del dominio.
 */
public final class StateSnapshot<T> {

    /** Estado del frame actual. Nunca null. */
    private final T current;

    /**
     * Estado del frame anterior.
     * Si no existe historial (primer frame), igual a current.
     * Nunca null.
     */
    private final T previous;

    // ── Constructor privado — usar factories ──────────────────────────────

    private StateSnapshot(T current, T previous) {
        this.current  = current;
        this.previous = previous;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Crea el snapshot inicial de un estado de dominio.
     *
     * Como no existe estado anterior, previous() retorna el mismo estado
     * que current(). Esto produce delta = 0 en todos los cálculos del primer
     * frame, que es el comportamiento semánticamente correcto.
     *
     * @param initialState el primer estado conocido. No puede ser null.
     * @param <T>          el tipo del estado.
     * @return snapshot inicial con current == previous.
     * @throws IllegalArgumentException si initialState es null.
     */
    public static <T> StateSnapshot<T> initial(T initialState) {
        if (initialState == null)
            throw new IllegalArgumentException("initialState no puede ser null");
        return new StateSnapshot<>(initialState, initialState);
    }

    // ── Operación de avance ───────────────────────────────────────────────

    /**
     * Produce un nuevo snapshot desplazando el estado actual al historial.
     *
     * Contrato:
     *   resultado.current()  == newState
     *   resultado.previous() == this.current()
     *
     * El snapshot original no se modifica.
     *
     * @param newState el nuevo estado del frame actual. No puede ser null.
     * @return nuevo snapshot con el estado anterior actualizado.
     * @throws IllegalArgumentException si newState es null.
     */
    public StateSnapshot<T> advance(T newState) {
        if (newState == null)
            throw new IllegalArgumentException("newState no puede ser null");
        return new StateSnapshot<>(newState, this.current);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /**
     * Estado del frame actual.
     *
     * @return el estado actual. Nunca null.
     */
    public T current() {
        return current;
    }

    /**
     * Estado del frame anterior.
     *
     * Si no existe historial real (primer frame tras {@link #initial}),
     * retorna el mismo estado que {@link #current()}.
     *
     * @return el estado anterior. Nunca null.
     */
    public T previous() {
        return previous;
    }

    /**
     * True si existe un estado anterior distinto al actual.
     *
     * En el primer frame tras {@link #initial}, retorna false.
     * En todos los frames siguientes, retorna true.
     *
     * Útil para evaluadores que solo deben activarse cuando hay
     * historial real (evitar falsos deltas en el primer frame).
     *
     * @return true si current != previous (por referencia).
     */
    public boolean hasPrevious() {
        return current != previous;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "StateSnapshot[current=" + current
            + (hasPrevious() ? ", previous=" + previous : ", noPrevious") + "]";
    }
}
