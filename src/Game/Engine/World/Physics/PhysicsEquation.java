package Game.Engine.World.Physics;

/**
 * Descripción declarativa de una ley física.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicsEquation es conocimiento físico expresado como dato, no como código.
 *
 * Una ecuación describe cómo una propiedad fuente influye sobre una propiedad
 * destino mediante un coeficiente calculado en función del material y del
 * estado actual. El PhysicsSolver la lee y la aplica uniformemente, sin
 * conocer qué fenómeno representa.
 *
 * Si la fuente y el destino son la misma propiedad, la ecuación modela
 * disipación, decaimiento o autorrefuerzo interno.
 *
 * ── POR QUÉ ESTO REEMPLAZA A LAS *Relation ───────────────────────────────
 * Las clases ThermalExpansionRelation, ChargeTransferRelation, etc., eran
 * fenómenos con nombres propios. Contenían lógica específica sobre temperatura,
 * carga, fluidos. El Engine las conocía por nombre.
 *
 * PhysicsEquation no tiene nombre de fenómeno. No contiene lógica sobre
 * ningún dominio concreto. Únicamente describe:
 *
 *   - qué propiedad produce la influencia (source)
 *   - qué propiedad recibe el efecto (target)
 *   - cómo se calcula el coeficiente de transferencia (coefficient)
 *   - si la ecuación aplica en el estado actual (condition)
 *
 * El Solver recorre todas las ecuaciones registradas de forma uniforme.
 * No distingue entre "ecuación térmica" y "ecuación eléctrica".
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * condition(ctx) → evalúa si los datos actuales hacen aplicable esta ecuación.
 *                  Solo lectura. Nunca modifica el estado.
 *
 * coefficient(ctx) → retorna el factor multiplicativo que se aplica sobre el
 *                    valor de la propiedad fuente para calcular el delta del
 *                    destino. Un coeficiente de 0 es equivalente a no aplicar.
 *
 * El Solver calcula el delta como:
 *
 *   delta = source.getValue() × coefficient(ctx)
 *
 * y lo aplica sobre la propiedad destino del mismo objeto:
 *
 *   target.add(delta)
 *
 * ── EJEMPLOS SIN FENÓMENOS ────────────────────────────────────────────────
 *
 *   // Expansión volumétrica (antes: ThermalExpansionRelation)
 *   //   source: temperatura   target: presión
 *   //   coefficient: (1 − compresibilidad) × factorEscala
 *   PhysicsEquation.of(
 *       CoreDomains.Thermal.class,
 *       CoreDomains.Pressure.class,
 *       ctx -> ctx.hasSource() && ctx.hasTarget(),
 *       ctx -> (1.0 - ctx.getMaterial().getCompressibility()) * 0.05
 *   )
 *
 *   // Disipación interna (antes: ThermalEnergyTransferRelation)
 *   //   source: temperatura   target: temperatura (misma propiedad)
 *   //   coefficient: −tasaDe disipación cuando energía > umbral
 *   PhysicsEquation.of(
 *       CoreDomains.Thermal.class,
 *       CoreDomains.Thermal.class,
 *       ctx -> ctx.hasSource()
 *           && Math.abs(ctx.source()) * ctx.getMaterial().getHeatCapacity() >= 500.0,
 *       ctx -> -0.1
 *   )
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva ley física = instanciar PhysicsEquation.
 * El PhysicsSolver no cambia.
 * El Engine no aprende ningún nuevo concepto.
 *
 * @param <S> dominio de la propiedad fuente.
 * @param <T> dominio de la propiedad destino.
 */
public final class PhysicsEquation<S extends PhysicalDomain, T extends PhysicalDomain> {

    private final Class<S> sourceDomain;
    private final Class<T> targetDomain;
    private final Condition<S, T>   condition;
    private final Coefficient<S, T> coefficient;
    private final int               priority;

    // ── Interfaces funcionales ─────────────────────────────────────────────

    /**
     * Condición de aplicabilidad.
     * Recibe el contexto del objeto y retorna true si la ecuación debe aplicarse.
     * Solo lectura — nunca modifica el estado.
     *
     * @param <S> dominio fuente.
     * @param <T> dominio destino.
     */
    @FunctionalInterface
    public interface Condition<S extends PhysicalDomain, T extends PhysicalDomain> {
        /**
         * @param ctx contexto del estado del objeto.
         * @return true si la ecuación aplica en este estado.
         */
        boolean test(EquationContext<S, T> ctx);
    }

    /**
     * Función de coeficiente.
     * Calcula el factor por el que se multiplica el valor fuente para obtener
     * el delta del destino. Solo lectura — nunca modifica el estado.
     *
     * @param <S> dominio fuente.
     * @param <T> dominio destino.
     */
    @FunctionalInterface
    public interface Coefficient<S extends PhysicalDomain, T extends PhysicalDomain> {
        /**
         * @param ctx contexto del estado del objeto.
         * @return coeficiente de transferencia. 0 = sin efecto.
         */
        double compute(EquationContext<S, T> ctx);
    }

    // ── Constructor privado — usar factories ──────────────────────────────

    private PhysicsEquation(Class<S> sourceDomain,
                             Class<T> targetDomain,
                             Condition<S, T>   condition,
                             Coefficient<S, T> coefficient,
                             int priority) {
        if (sourceDomain  == null) throw new IllegalArgumentException("sourceDomain no puede ser null");
        if (targetDomain  == null) throw new IllegalArgumentException("targetDomain no puede ser null");
        if (condition     == null) throw new IllegalArgumentException("condition no puede ser null");
        if (coefficient   == null) throw new IllegalArgumentException("coefficient no puede ser null");
        this.sourceDomain = sourceDomain;
        this.targetDomain = targetDomain;
        this.condition    = condition;
        this.coefficient  = coefficient;
        this.priority     = priority;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Crea una ecuación con prioridad por defecto (100).
     *
     * @param sourceDomain dominio de la propiedad fuente.
     * @param targetDomain dominio de la propiedad destino.
     * @param condition    condición de aplicabilidad.
     * @param coefficient  función de coeficiente.
     * @param <S>          dominio fuente.
     * @param <T>          dominio destino.
     * @return ecuación configurada.
     */
    public static <S extends PhysicalDomain, T extends PhysicalDomain>
    PhysicsEquation<S, T> of(Class<S> sourceDomain,
                              Class<T> targetDomain,
                              Condition<S, T>   condition,
                              Coefficient<S, T> coefficient) {
        return new PhysicsEquation<>(sourceDomain, targetDomain, condition, coefficient, 100);
    }

    /**
     * Crea una ecuación con prioridad explícita.
     * Menor prioridad = se evalúa antes.
     *
     * @param sourceDomain dominio de la propiedad fuente.
     * @param targetDomain dominio de la propiedad destino.
     * @param condition    condición de aplicabilidad.
     * @param coefficient  función de coeficiente.
     * @param priority     prioridad de evaluación.
     * @param <S>          dominio fuente.
     * @param <T>          dominio destino.
     * @return ecuación configurada.
     */
    public static <S extends PhysicalDomain, T extends PhysicalDomain>
    PhysicsEquation<S, T> of(Class<S> sourceDomain,
                              Class<T> targetDomain,
                              Condition<S, T>   condition,
                              Coefficient<S, T> coefficient,
                              int priority) {
        return new PhysicsEquation<>(sourceDomain, targetDomain, condition, coefficient, priority);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Dominio de la propiedad fuente. */
    public Class<S> getSourceDomain() { return sourceDomain; }

    /** Dominio de la propiedad destino. */
    public Class<T> getTargetDomain() { return targetDomain; }

    /** Prioridad de evaluación. Menor = antes. */
    public int getPriority() { return priority; }

    // ── Evaluación ────────────────────────────────────────────────────────

    /**
     * True si la ecuación aplica sobre el contexto dado.
     * Solo lectura.
     *
     * @param ctx contexto del objeto.
     * @return true si la ecuación debe resolverse.
     */
    public boolean applies(EquationContext<S, T> ctx) {
        return condition.test(ctx);
    }

    /**
     * Calcula el coeficiente de esta ecuación sobre el contexto dado.
     * Solo lectura.
     *
     * @param ctx contexto del objeto.
     * @return coeficiente de transferencia.
     */
    public double computeCoefficient(EquationContext<S, T> ctx) {
        return coefficient.compute(ctx);
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PhysicsEquation["
            + sourceDomain.getSimpleName()
            + " → "
            + targetDomain.getSimpleName()
            + " priority=" + priority + "]";
    }
}
