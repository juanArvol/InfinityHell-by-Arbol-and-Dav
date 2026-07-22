package Game.Engine.World.Physics.Core;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Estado temporal de trabajo durante la resolución física de un frame.
 *
 * ── HRFC-020 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * Ninguna ley física modifica directamente el PhysicalState definitivo.
 *
 * Durante la resolución de un frame el PhysicsSolver crea un WorkingState
 * por entidad. Las leyes operan exclusivamente sobre WorkingState.
 * Al finalizar todas las leyes, una fase explícita de Commit consolida los
 * valores calculados en el PhysicalState real.
 *
 * El flujo completo es:
 *
 *   PhysicalState (estado actual del frame anterior)
 *       ↓
 *   WorkingState  (copia mutable para la resolución)
 *       ↓
 *   ejecutar todas las leyes sobre WorkingState
 *       ↓
 *   commit()
 *       ↓
 *   PhysicalState actualizado (estado definitivo del frame actual)
 *
 * ── SEMÁNTICA DE ESCRITURA ────────────────────────────────────────────────
 * WorkingState diferencia entre dos operaciones:
 *
 *   snapshot  → copia inicial de cada valor tomada del PhysicalState al inicio
 *               del frame. Las leyes leen el snapshot para sus cálculos.
 *               Permanece inmutable durante toda la resolución.
 *
 *   pending   → deltas acumulados por las leyes durante la resolución.
 *               Se suman al snapshot al hacer commit.
 *
 * Esto garantiza que dentro de una misma iteración todas las leyes ven un
 * estado consistente (el del inicio del frame), eliminando los efectos
 * colaterales derivados del orden de ejecución entre leyes.
 *
 * ── POR QUÉ NO SE USAN STRINGS ───────────────────────────────────────────
 * Igual que PhysicalState, WorkingState utiliza IdentityHashMap<PropertyDescriptor, Double>.
 * La identidad de la referencia del descriptor es la clave. No existe ninguna
 * API de acceso por String.
 *
 * ── COMMIT ────────────────────────────────────────────────────────────────
 * La operación commit() aplica todos los deltas pendientes al PhysicalState
 * real. Es la única puerta de escritura hacia el estado definitivo.
 * Solo el PhysicsSolver llama a commit(). Las leyes nunca tienen acceso
 * directo al PhysicalState ni a commit().
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ Ningún método acepta String como identificador.
 *   ✗ Ningún Map<String, ...> interno.
 *   ✓ Todo acceso es exclusivamente mediante PropertyDescriptor.
 *   ✓ Las leyes nunca modifican PhysicalState directamente.
 *   ✓ commit() es la única transición WorkingState → PhysicalState.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class WorkingState {

    /**
     * Snapshot del estado al inicio del frame.
     * Inmutable durante la resolución: las leyes leen de aquí.
     * Indexado por identidad de descriptor.
     */
    private final IdentityHashMap<PropertyDescriptor, Double> snapshot;

    /**
     * Deltas pendientes acumulados por las leyes durante la resolución.
     * Indexado por identidad de descriptor.
     */
    private final IdentityHashMap<PropertyDescriptor, Double> pending;

    /**
     * El PhysicalState definitivo al que se consolida en commit().
     */
    private final PhysicalState target;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un WorkingState a partir del PhysicalState de una entidad.
     * Toma un snapshot de todos los valores actuales.
     *
     * @param state el estado físico de origen. No puede ser null.
     */
    public WorkingState(PhysicalState state) {
        if (state == null) throw new IllegalArgumentException("state no puede ser null");
        this.target   = state;
        this.snapshot = new IdentityHashMap<>(state.size() + 4);
        this.pending  = new IdentityHashMap<>(state.size() + 4);
        for (PropertyDescriptor desc : state.registeredDescriptors()) {
            snapshot.put(desc, state.get(desc));
        }
    }

    // ── Acceso de lectura (desde las leyes) ───────────────────────────────

    /**
     * True si la entidad tiene la propiedad registrada.
     *
     * @param descriptor descriptor de la propiedad.
     * @return true si existe.
     */
    public boolean has(PropertyDescriptor descriptor) {
        return descriptor != null && snapshot.containsKey(descriptor);
    }

    /**
     * Valor del snapshot de la propiedad para esta iteración.
     * Todas las leyes ven el mismo valor de inicio de frame independientemente
     * del orden de ejecución.
     * Retorna 0.0 si la entidad no tiene la propiedad.
     *
     * @param descriptor descriptor de la propiedad.
     * @return valor del snapshot, o 0.0 si no existe.
     */
    public double get(PropertyDescriptor descriptor) {
        if (descriptor == null) return 0.0;
        Double v = snapshot.get(descriptor);
        return v != null ? v : 0.0;
    }

    // ── Escritura diferida (desde las leyes) ──────────────────────────────

    /**
     * Acumula un delta sobre la propiedad.
     * Los deltas no se aplican al snapshot — son pendientes hasta commit().
     * No hace nada si la entidad no tiene la propiedad.
     *
     * @param descriptor descriptor de la propiedad.
     * @param delta      valor a añadir. Negativo para restar.
     */
    public void add(PropertyDescriptor descriptor, double delta) {
        if (descriptor == null || !snapshot.containsKey(descriptor)) return;
        pending.merge(descriptor, delta, Double::sum);
    }

    // ── Commit (solo el PhysicsSolver) ────────────────────────────────────

    /**
     * Consolida todos los deltas pendientes en el PhysicalState definitivo.
     *
     * Para cada propiedad con delta pendiente:
     *   valor_final = snapshot + delta_total
     *
     * El PhysicalState aplica clamp si la propiedad está acotada.
     * Tras commit(), el WorkingState queda vacío de pendientes y no debe
     * reutilizarse en otro frame.
     *
     * Solo el PhysicsSolver debe llamar a este método.
     */
    public void commit() {
        for (Map.Entry<PropertyDescriptor, Double> entry : pending.entrySet()) {
            PropertyDescriptor desc  = entry.getKey();
            double             delta = entry.getValue();
            // El valor final es el snapshot + delta acumulado total.
            // set() sobre PhysicalState aplica clamp si corresponde.
            double finalValue = snapshot.get(desc) + delta;
            target.set(desc, finalValue);
        }
        pending.clear();
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "WorkingState[" + snapshot.size() + " properties, "
            + pending.size() + " pending]";
    }
}
