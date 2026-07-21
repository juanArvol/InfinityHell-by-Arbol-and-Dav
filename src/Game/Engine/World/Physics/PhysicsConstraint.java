package Game.Engine.World.Physics;

import Game.Engine.World.Components.MaterialComponent;

/**
 * Restricción declarativa sobre una propiedad física.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicsConstraint es una restricción física expresada como dato.
 *
 * Una restricción corrige el valor de una propiedad después de que el Solver
 * ha aplicado las ecuaciones. No modela fenómenos. Modela límites físicos:
 *
 *   - Una propiedad no puede superar un máximo determinado por el material.
 *   - Una propiedad no puede ser negativa.
 *   - Una propiedad converge hacia un valor de equilibrio.
 *   - Una propiedad se disipa a una tasa que depende del material.
 *
 * El PhysicsSolver aplica las restricciones en una fase separada, posterior
 * a la resolución de ecuaciones, para garantizar consistencia física.
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * condition(quantity, material) → evalúa si la restricción aplica sobre el
 *                                 valor actual de la propiedad. Solo lectura.
 *
 * correct(quantity, material)  → corrige el valor de la propiedad. Esta es
 *                                la única operación de escritura en todo el
 *                                modelo declarativo.
 *
 * ── EJEMPLOS SIN FENÓMENOS ────────────────────────────────────────────────
 *
 *   // Restricción de disipación ambiental térmica
 *   //   Si hay temperatura residual, converge hacia 0 a la tasa del material
 *   PhysicsConstraint.of(
 *       CoreDomains.Thermal.class,
 *       (q, mat) -> !q.isZero(),
 *       (q, mat) -> q.converge(0.0, mat.getThermalDiffusivity() * 0.05)
 *   )
 *
 *   // Restricción de clamp de humedad (rango natural [0, 1])
 *   PhysicsConstraint.of(
 *       CoreDomains.Fluid.class,
 *       (q, mat) -> q.getValue() < 0.0 || q.getValue() > 1.0,
 *       (q, mat) -> q.clampTo(0.0, 1.0)
 *   )
 *
 *   // Restricción de disipación de carga eléctrica
 *   PhysicsConstraint.of(
 *       CoreDomains.Electrical.class,
 *       (q, mat) -> !q.isZero(),
 *       (q, mat) -> q.converge(0.0, mat.getElectricalConductivity() * 0.02)
 *   )
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva restricción = instanciar PhysicsConstraint.
 * El PhysicsSolver no cambia.
 *
 * @param <D> dominio de la propiedad sobre la que opera esta restricción.
 */
public final class PhysicsConstraint<D extends PhysicalDomain> {

    private final Class<D>    domain;
    private final Condition<D>  condition;
    private final Correction<D> correction;
    private final int           priority;

    // ── Interfaces funcionales ─────────────────────────────────────────────

    /**
     * Condición de aplicabilidad de la restricción.
     * Solo lectura — nunca modifica el estado.
     *
     * @param <D> dominio de la propiedad.
     */
    @FunctionalInterface
    public interface Condition<D extends PhysicalDomain> {
        /**
         * @param quantity valor actual de la propiedad.
         * @param material material del objeto.
         * @return true si la restricción debe aplicarse.
         */
        boolean test(PhysicalQuantity<D> quantity, MaterialComponent material);
    }

    /**
     * Corrección de la propiedad.
     * Es la única operación de escritura en el modelo declarativo.
     * Modifica quantity in-place.
     *
     * @param <D> dominio de la propiedad.
     */
    @FunctionalInterface
    public interface Correction<D extends PhysicalDomain> {
        /**
         * @param quantity valor de la propiedad a corregir (mutable).
         * @param material material del objeto.
         */
        void apply(PhysicalQuantity<D> quantity, MaterialComponent material);
    }

    // ── Constructor privado — usar factories ──────────────────────────────

    private PhysicsConstraint(Class<D>     domain,
                               Condition<D>  condition,
                               Correction<D> correction,
                               int           priority) {
        if (domain     == null) throw new IllegalArgumentException("domain no puede ser null");
        if (condition  == null) throw new IllegalArgumentException("condition no puede ser null");
        if (correction == null) throw new IllegalArgumentException("correction no puede ser null");
        this.domain     = domain;
        this.condition  = condition;
        this.correction = correction;
        this.priority   = priority;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Crea una restricción con prioridad por defecto (100).
     *
     * @param domain     dominio de la propiedad que restringe.
     * @param condition  condición de aplicabilidad.
     * @param correction corrección a aplicar.
     * @param <D>        dominio físico.
     * @return restricción configurada.
     */
    public static <D extends PhysicalDomain>
    PhysicsConstraint<D> of(Class<D>     domain,
                             Condition<D>  condition,
                             Correction<D> correction) {
        return new PhysicsConstraint<>(domain, condition, correction, 100);
    }

    /**
     * Crea una restricción con prioridad explícita.
     * Menor prioridad = se aplica antes.
     *
     * @param domain     dominio de la propiedad que restringe.
     * @param condition  condición de aplicabilidad.
     * @param correction corrección a aplicar.
     * @param priority   prioridad de aplicación.
     * @param <D>        dominio físico.
     * @return restricción configurada.
     */
    public static <D extends PhysicalDomain>
    PhysicsConstraint<D> of(Class<D>     domain,
                             Condition<D>  condition,
                             Correction<D> correction,
                             int           priority) {
        return new PhysicsConstraint<>(domain, condition, correction, priority);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Dominio de la propiedad que esta restricción opera. */
    public Class<D> getDomain() { return domain; }

    /** Prioridad de aplicación. Menor = antes. */
    public int getPriority() { return priority; }

    // ── Evaluación ────────────────────────────────────────────────────────

    /**
     * True si la restricción aplica sobre la propiedad con el material dado.
     *
     * @param quantity valor actual de la propiedad.
     * @param material material del objeto.
     * @return true si la corrección debe aplicarse.
     */
    public boolean applies(PhysicalQuantity<D> quantity, MaterialComponent material) {
        return condition.test(quantity, material);
    }

    /**
     * Aplica la corrección sobre la propiedad.
     * Solo llamar si applies() retornó true.
     *
     * @param quantity valor de la propiedad a corregir (mutable).
     * @param material material del objeto.
     */
    public void correct(PhysicalQuantity<D> quantity, MaterialComponent material) {
        correction.apply(quantity, material);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PhysicsConstraint[" + domain.getSimpleName() + " priority=" + priority + "]";
    }
}
