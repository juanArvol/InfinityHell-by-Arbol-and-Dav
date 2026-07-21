package Game.Engine.World.Physics;

import Game.Engine.World.Components.MaterialComponent;

/**
 * Vista del estado físico de un objeto durante la resolución del Solver.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * SolverContext es la única ventana que PhysicsSolver y las funciones de
 * PhysicsEquation / PhysicsConstraint tienen sobre el estado de un objeto.
 *
 * Reemplaza PropertyContext del modelo anterior. La diferencia fundamental:
 *
 *   PropertyContext → conocía ThermalComponent, ElectricalComponent, etc.
 *                     Resolvía componentes concretos por nombre de dominio.
 *                     Solo funcionaba con los dominios de CoreDomains.
 *
 *   SolverContext   → solo conoce PhysicalState.
 *                     Accede a cualquier propiedad por su descriptor.
 *                     Completamente agnóstico al dominio.
 *                     Funciona con cualquier propiedad registrada.
 *
 * ── ACCESO ────────────────────────────────────────────────────────────────
 * SolverContext provee dos niveles de acceso:
 *
 *   1. Por PhysicalProperty (API principal, type-safe)
 *      ctx.has(PhysicalProperties.TEMPERATURE)
 *      ctx.get(PhysicalProperties.TEMPERATURE)
 *      ctx.quantity(PhysicalProperties.TEMPERATURE)
 *
 *   2. Por par de propiedades (para PhysicsEquation — fuente/destino)
 *      EquationContext<S,T> = ctx.equationContext(sourceProperty, targetProperty)
 *
 * ── SOLO LECTURA vs. ESCRITURA ───────────────────────────────────────────
 * La mayoría de accesos son de solo lectura.
 * La única escritura válida ocurre a través de PhysicsConstraint.correct(),
 * que recibe directamente la PhysicalQuantity mutable.
 * El PhysicsSolver aplica los deltas de ecuaciones directamente sobre las
 * PhysicalQuantity obtenidas mediante quantity().
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 * SolverContext es una vista ligera sobre PhysicalState. No copia ni cachea
 * el estado. Es una referencia directa al PhysicalState del objeto.
 *
 * Se crea por objeto por frame. Su coste es mínimo: un solo campo (PhysicalState).
 */
public final class SolverContext {

    private final PhysicalState state;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un contexto sobre el estado dado.
     *
     * @param state estado físico del objeto. No puede ser null.
     */
    public SolverContext(PhysicalState state) {
        if (state == null) throw new IllegalArgumentException("state no puede ser null");
        this.state = state;
    }

    /**
     * Factory estática.
     *
     * @param state estado físico del objeto. No puede ser null.
     * @return nuevo SolverContext.
     */
    public static SolverContext of(PhysicalState state) {
        return new SolverContext(state);
    }

    // ── Acceso al estado ──────────────────────────────────────────────────

    /**
     * True si el objeto tiene la propiedad registrada.
     *
     * @param property descriptor de la propiedad.
     * @return true si existe una magnitud para esta propiedad.
     */
    public boolean has(PhysicalProperty<?> property) {
        return state.has(property);
    }

    /**
     * Valor numérico actual de la propiedad, o 0.0 si no existe.
     *
     * @param property descriptor de la propiedad.
     * @return valor actual.
     */
    public double get(PhysicalProperty<?> property) {
        return state.get(property);
    }

    /**
     * La PhysicalQuantity mutable de la propiedad.
     * El Solver la usa para aplicar deltas. Retorna null si no existe.
     *
     * @param property descriptor de la propiedad.
     * @param <D>      dominio físico.
     * @return magnitud mutable, o null.
     */
    public <D extends PhysicalDomain> PhysicalQuantity<D> quantity(
            PhysicalProperty<D> property) {
        return state.getQuantity(property);
    }

    // ── Acceso al material ────────────────────────────────────────────────

    /**
     * El material del objeto. Nunca null.
     *
     * @return material del objeto.
     */
    public MaterialComponent getMaterial() {
        return state.getMaterial();
    }

    // ── Construcción de EquationContext ───────────────────────────────────

    /**
     * Construye un EquationContext<S,T> tipado para una PhysicsEquation.
     *
     * El PhysicsSolver crea un EquationContext por cada par (ecuación, objeto)
     * durante el recorrido de resolución. Esto permite que las funciones
     * Condition y Coefficient de PhysicsEquation reciban un contexto tipado
     * con acceso directo a las magnitudes fuente y destino.
     *
     * @param sourceProperty propiedad fuente de la ecuación.
     * @param targetProperty propiedad destino de la ecuación.
     * @param <S>            dominio fuente.
     * @param <T>            dominio destino.
     * @return contexto tipado para la evaluación de la ecuación.
     */
    public <S extends PhysicalDomain, T extends PhysicalDomain>
    EquationContext<S, T> equationContext(
            PhysicalProperty<S> sourceProperty,
            PhysicalProperty<T> targetProperty) {
        PhysicalQuantity<S> source = state.getQuantity(sourceProperty);
        PhysicalQuantity<T> target = state.getQuantity(targetProperty);
        return new EquationContext<>(source, target, state.getMaterial());
    }

    // ── Acceso al PhysicalState completo ──────────────────────────────────

    /**
     * El PhysicalState subyacente.
     * El Solver usa este acceso para iterar sobre las propiedades registradas.
     *
     * @return estado físico completo del objeto.
     */
    public PhysicalState getState() { return state; }
}
