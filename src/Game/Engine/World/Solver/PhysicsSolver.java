package Game.Engine.World.Solver;

import Game.Engine.GameObjects;
import Game.Engine.World.Components.MaterialComponent;
import Game.Engine.World.Physics.EquationContext;
import Game.Engine.World.Physics.PhysicalDomain;
import Game.Engine.World.Physics.PhysicalProperties;
import Game.Engine.World.Physics.PhysicalProperty;
import Game.Engine.World.Physics.PhysicalQuantity;
import Game.Engine.World.Physics.PhysicalState;
import Game.Engine.World.Physics.PhysicsConstraint;
import Game.Engine.World.Physics.PhysicsEquation;
import Game.Engine.World.Physics.SolverContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Único motor físico del World Simulation Core.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PhysicsSolver no conoce fenómenos.
 * No contiene lógica específica para calor, electricidad, fluidos, presión,
 * radiación, magnetismo, gravedad, lava, agua, hielo, fuego ni ningún otro
 * fenómeno físico concreto.
 *
 * PhysicsSolver únicamente resuelve los datos que se le han registrado:
 *   - ecuaciones físicas (PhysicsEquation)
 *   - restricciones físicas (PhysicsConstraint)
 *   - transferencias entre pares de objetos (PairEquation)
 *
 * Toda consecuencia emerge exclusivamente de esos datos.
 * El algoritmo permanece completamente inalterado ante cualquier extensión.
 *
 * ── FLUJO DEFINITIVO ──────────────────────────────────────────────────────
 *
 *   PhysicsSolver
 *       ↓
 *   lee PhysicalProperty     (qué propiedades tiene cada objeto)
 *       ↓
 *   lee MaterialProperty     (constantes físicas del material)
 *       ↓
 *   aplica PhysicsEquation   (leyes físicas intra-objeto)
 *       ↓
 *   aplica PairEquation      (transferencias entre pares de objetos)
 *       ↓
 *   aplica PhysicsConstraint (correcciones de equilibrio y disipación)
 *       ↓
 *   actualiza PhysicalState  (única fuente de verdad)
 *
 * ── TRES FASES DE RESOLUCIÓN ─────────────────────────────────────────────
 *
 * Fase 1 — Ecuaciones intra-objeto:
 *   Para cada objeto con PhysicalState:
 *     Para cada PhysicsEquation registrada (en orden de prioridad):
 *       Si equation.applies(ctx): target.add(source × coefficient)
 *
 * Fase 2 — Ecuaciones de transferencia entre pares:
 *   Para cada par (A, B) dentro del radio de contacto:
 *     Para cada PairEquation registrada:
 *       delta = conductivity × (valueA - valueB) × timeScale
 *       stateA.target.add(-delta)
 *       stateB.target.add(+delta)
 *
 * Fase 3 — Restricciones:
 *   Para cada objeto:
 *     Para cada PhysicsConstraint registrada (en orden de prioridad):
 *       Si constraint.applies(quantity, material): constraint.correct(...)
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * Ningún bloque if, switch ni instanceof sobre dominios concretos.
 * Ninguna rama de código específica para temperatura, carga ni humedad.
 * Toda la resolución es uniforme sobre los datos registrados.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva ley física:
 *   solver.addEquation(PhysicsEquation.of(...))
 *   → el algoritmo del Solver no cambia.
 *
 * Añadir una nueva restricción:
 *   solver.addConstraint(PhysicsConstraint.of(...))
 *   → el algoritmo del Solver no cambia.
 *
 * Añadir una nueva transferencia entre pares:
 *   solver.addPairEquation(PairEquation.of(...))
 *   → el algoritmo del Solver no cambia.
 *
 * ── GAMEPLAY ──────────────────────────────────────────────────────────────
 * PhysicsSolver nunca conoce:
 *   ✗ StatusEffects
 *   ✗ GameplayEvents
 *   ✗ daño
 *   ✗ buffs / debuffs
 *   ✗ efectos visuales
 *   ✗ animaciones
 *   ✗ audio
 *   ✗ ningún concepto de Gameplay
 *
 * Gameplay observa el PhysicalState resultante y decide sus consecuencias.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicsSolver {

    // ── Ecuaciones intra-objeto (ordenadas por prioridad, lazy) ───────────
    private final List<PhysicsEquation<?, ?>> equations   = new ArrayList<>();

    // ── Ecuaciones de transferencia entre pares ───────────────────────────
    private final List<PairEquation<?>>       pairEquations = new ArrayList<>();

    // ── Restricciones (ordenadas por prioridad, lazy) ─────────────────────
    private final List<PhysicsConstraint<?>>  constraints  = new ArrayList<>();

    /** True cuando las listas necesitan reordenarse por prioridad. */
    private boolean equationsDirty   = false;
    private boolean constraintsDirty = false;
    private boolean pairsDirty       = false;

    // ── Constructor ───────────────────────────────────────────────────────

    /** Construye un Solver vacío. Registrar ecuaciones y restricciones con los métodos add*(). */
    public PhysicsSolver() {}

    // ── Registro de ecuaciones intra-objeto ──────────────────────────────

    /**
     * Registra una ecuación física intra-objeto.
     *
     * La ecuación describe cómo la propiedad fuente influye sobre la
     * propiedad destino en el mismo objeto.
     *
     * @param equation la ecuación a registrar. No hace nada si null.
     */
    public void addEquation(PhysicsEquation<?, ?> equation) {
        if (equation == null) return;
        equations.add(equation);
        equationsDirty = true;
    }

    /**
     * Registra múltiples ecuaciones en un solo paso.
     *
     * @param equations ecuaciones a registrar.
     */
    public void addEquations(PhysicsEquation<?, ?>... equations) {
        if (equations == null) return;
        for (PhysicsEquation<?, ?> eq : equations) addEquation(eq);
    }

    // ── Registro de ecuaciones de par ─────────────────────────────────────

    /**
     * Registra una ecuación de transferencia entre pares de objetos.
     *
     * La ecuación de par describe cómo se transfiere una propiedad entre dos
     * objetos adyacentes en función de sus valores y conductividades.
     *
     * @param pairEquation ecuación de par a registrar. No hace nada si null.
     */
    public void addPairEquation(PairEquation<?> pairEquation) {
        if (pairEquation == null) return;
        pairEquations.add(pairEquation);
        pairsDirty = true;
    }

    /**
     * Registra múltiples ecuaciones de par en un solo paso.
     *
     * @param pairEquations ecuaciones de par a registrar.
     */
    public void addPairEquations(PairEquation<?>... pairEquations) {
        if (pairEquations == null) return;
        for (PairEquation<?> pe : pairEquations) addPairEquation(pe);
    }

    // ── Registro de restricciones ─────────────────────────────────────────

    /**
     * Registra una restricción física.
     *
     * La restricción corrige el valor de una propiedad después de que se
     * hayan aplicado todas las ecuaciones.
     *
     * @param constraint la restricción a registrar. No hace nada si null.
     */
    public void addConstraint(PhysicsConstraint<?> constraint) {
        if (constraint == null) return;
        constraints.add(constraint);
        constraintsDirty = true;
    }

    /**
     * Registra múltiples restricciones en un solo paso.
     *
     * @param constraints restricciones a registrar.
     */
    public void addConstraints(PhysicsConstraint<?>... constraints) {
        if (constraints == null) return;
        for (PhysicsConstraint<?> c : constraints) addConstraint(c);
    }

    // ── Eliminación ───────────────────────────────────────────────────────

    /** Elimina todas las ecuaciones, pares y restricciones registradas. */
    public void clear() {
        equations.clear();
        pairEquations.clear();
        constraints.clear();
        equationsDirty   = false;
        constraintsDirty = false;
        pairsDirty       = false;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** Número de ecuaciones intra-objeto registradas. */
    public int equationCount()    { return equations.size(); }

    /** Número de ecuaciones de par registradas. */
    public int pairEquationCount(){ return pairEquations.size(); }

    /** Número de restricciones registradas. */
    public int constraintCount()  { return constraints.size(); }

    /** True si no hay ecuaciones, pares ni restricciones registradas. */
    public boolean isEmpty() {
        return equations.isEmpty() && pairEquations.isEmpty() && constraints.isEmpty();
    }

    // ── Resolución — entry point ──────────────────────────────────────────

    /**
     * Resuelve un frame completo de simulación física sobre la lista de objetos.
     *
     * Orden garantizado:
     *   1. Ecuaciones intra-objeto (por prioridad ascendente)
     *   2. Ecuaciones de par       (por prioridad ascendente)
     *   3. Restricciones           (por prioridad ascendente)
     *
     * Solo los objetos que tienen un PhysicalState no nulo y no vacío
     * participan en la resolución.
     *
     * @param objects lista de objetos activos en el mundo este frame.
     */
    public void solve(List<GameObjects> objects) {
        if (objects == null || objects.isEmpty()) return;
        if (isEmpty()) return;

        ensureSorted();

        // ── Fase 1: ecuaciones intra-objeto ───────────────────────────────
        if (!equations.isEmpty()) {
            for (GameObjects obj : objects) {
                PhysicalState state = stateOf(obj);
                if (state == null || state.isEmpty()) continue;
                resolveEquations(state);
            }
        }

        // ── Fase 2: ecuaciones de par ─────────────────────────────────────
        if (!pairEquations.isEmpty()) {
            int n = objects.size();
            for (int i = 0; i < n - 1; i++) {
                GameObjects objA  = objects.get(i);
                PhysicalState stateA = stateOf(objA);
                if (stateA == null || stateA.isEmpty()) continue;

                for (int j = i + 1; j < n; j++) {
                    GameObjects objB  = objects.get(j);
                    PhysicalState stateB = stateOf(objB);
                    if (stateB == null || stateB.isEmpty()) continue;

                    double dist = distance(objA, objB);
                    resolvePairs(stateA, stateB, dist);
                }
            }
        }

        // ── Fase 3: restricciones ─────────────────────────────────────────
        if (!constraints.isEmpty()) {
            for (GameObjects obj : objects) {
                PhysicalState state = stateOf(obj);
                if (state == null || state.isEmpty()) continue;
                resolveConstraints(state);
            }
        }
    }

    // ── Fase 1: resolución de ecuaciones intra-objeto ─────────────────────

    /**
     * Aplica todas las ecuaciones intra-objeto sobre el estado del objeto.
     * El algoritmo es completamente uniforme — no distingue dominios.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void resolveEquations(PhysicalState state) {
        MaterialComponent material = state.getMaterial();

        for (PhysicsEquation<?, ?> equation : equations) {
            // Obtener las propiedades fuente y destino del catálogo de la ecuación
            PhysicalProperty sourceProperty = findProperty(state, equation.getSourceDomain());
            PhysicalProperty targetProperty = findProperty(state, equation.getTargetDomain());

            if (sourceProperty == null || targetProperty == null) continue;

            PhysicalQuantity sourceQ = state.getQuantity(sourceProperty);
            PhysicalQuantity targetQ = state.getQuantity(targetProperty);

            if (sourceQ == null || targetQ == null) continue;

            EquationContext ctx = new EquationContext<>(sourceQ, targetQ, material);

            if (!((PhysicsEquation) equation).applies(ctx)) continue;

            double coeff = ((PhysicsEquation) equation).computeCoefficient(ctx);
            if (Math.abs(coeff) < 1e-12) continue;

            double delta = sourceQ.getValue() * coeff;
            targetQ.add(delta);
        }
    }

    // ── Fase 2: resolución de ecuaciones de par ───────────────────────────

    /**
     * Aplica las ecuaciones de transferencia entre el par (stateA, stateB).
     * El algoritmo es completamente uniforme — no distingue dominios.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void resolvePairs(PhysicalState stateA,
                               PhysicalState stateB,
                               double distance) {
        for (PairEquation<?> pairEq : pairEquations) {
            if (distance > pairEq.getContactRadius()) continue;

            PhysicalProperty property = findProperty(stateA, pairEq.getDomain());
            if (property == null) continue;

            PhysicalQuantity qA = stateA.getQuantity(property);
            PhysicalQuantity qB = stateB.getQuantity(property);

            if (qA == null || qB == null) continue;

            double conductivity = ((PairEquation) pairEq)
                .conductivity(stateA.getMaterial(), stateB.getMaterial(), qA, qB);
            if (conductivity <= 0) continue;

            double diff = qA.getValue() - qB.getValue();
            if (Math.abs(diff) < pairEq.getEpsilon()) continue;

            double delta = diff * conductivity * pairEq.getTimeScale();

            double scaleA = ((PairEquation) pairEq).transferScale(stateA.getMaterial(), qA);
            double scaleB = ((PairEquation) pairEq).transferScale(stateB.getMaterial(), qB);

            qA.add(-delta * scaleA);
            qB.add( delta * scaleB);
        }
    }

    // ── Fase 3: resolución de restricciones ──────────────────────────────

    /**
     * Aplica todas las restricciones sobre el estado del objeto.
     * El algoritmo es completamente uniforme — no distingue dominios.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void resolveConstraints(PhysicalState state) {
        MaterialComponent material = state.getMaterial();

        Collection<PhysicalProperty<?>> props = state.registeredProperties();

        for (PhysicsConstraint constraint : constraints) {
            // Encontrar la propiedad del estado que coincide con el dominio
            PhysicalProperty matchingProperty = findPropertyByDomain(props, constraint.getDomain());
            if (matchingProperty == null) continue;

            PhysicalQuantity q = state.getQuantity(matchingProperty);
            if (q == null) continue;

            if (constraint.applies(q, material)) {
                constraint.correct(q, material);
            }
        }
    }

    // ── Helpers de búsqueda ───────────────────────────────────────────────

    /**
     * Busca en el estado la primera propiedad que pertenece al dominio dado.
     * El algoritmo no distingue dominios — simplemente busca por clase.
     */
    @SuppressWarnings("rawtypes")
    private PhysicalProperty findProperty(PhysicalState state, Class<?> domainClass) {
        for (PhysicalProperty<?> prop : state.registeredProperties()) {
            if (domainClass.equals(prop.getDomain())) {
                return prop;
            }
        }
        return null;
    }

    /**
     * Busca en una colección de propiedades la primera que pertenece al dominio dado.
     */
    @SuppressWarnings("rawtypes")
    private PhysicalProperty findPropertyByDomain(
            Collection<PhysicalProperty<?>> props,
            Class<?> domainClass) {
        for (PhysicalProperty<?> prop : props) {
            if (domainClass.equals(prop.getDomain())) {
                return prop;
            }
        }
        return null;
    }

    /** Obtiene el PhysicalState del objeto. Retorna null si no lo tiene. */
    private static PhysicalState stateOf(GameObjects obj) {
        if (obj == null) return null;
        PhysicalStateComponent comp = obj.getComponent(PhysicalStateComponent.class);
        return comp != null ? comp.getState() : null;
    }

    /** Distancia euclídea entre dos objetos. */
    private static double distance(GameObjects a, GameObjects b) {
        double dx = a.getTransform().getPosition().getX()
                  - b.getTransform().getPosition().getX();
        double dy = a.getTransform().getPosition().getY()
                  - b.getTransform().getPosition().getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ── Ordenación lazy por prioridad ─────────────────────────────────────

    private void ensureSorted() {
        if (equationsDirty) {
            equations.sort(Comparator.comparingInt(PhysicsEquation::getPriority));
            equationsDirty = false;
        }
        if (constraintsDirty) {
            constraints.sort(Comparator.comparingInt(PhysicsConstraint::getPriority));
            constraintsDirty = false;
        }
        if (pairsDirty) {
            pairEquations.sort(Comparator.comparingInt(PairEquation::getPriority));
            pairsDirty = false;
        }
    }
}
