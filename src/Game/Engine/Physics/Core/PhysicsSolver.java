package Game.Engine.Physics.Core;

import Game.Engine.GameObjects;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Motor de resolución física del World Simulation Core.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 *
 * ── FLUJO DEFINITIVO ──────────────────────────────────────────────────────
 *
 *   PhysicsSolver.solve(relations, objects, deltaTime)
 *       ↓
 *   Para cada entidad: resolveState() → ResolvedState(PhysicalState, SimulationContext?)
 *       ↓
 *   Construir WorkingState + EvaluationView (con context() si disponible)
 *       ↓
 *   Para cada PhysicalRelation (ordenadas por prioridad):
 *       EvaluatorRegistry.get(relation.getRelationType()) → RelationEvaluator
 *       evaluator.evaluate(relation, views, deltaTime)
 *         Los evaluadores de dominio compuesto leen view.context() para acceder a
 *         KinematicState, MaterialState, ContactState, EnvironmentState.
 *       ↓
 *   Commit: WorkingState.commit() → PhysicalState definitivo
 *
 * ── RESOLUCIÓN DE ESTADO ──────────────────────────────────────────────────
 * FrameContext.resolveState() sigue el orden de prioridad:
 *   1. SimulationContextComponent — extrae physical() del contexto compuesto
 *                                    y expone el SimulationContext via view.context()
 *   2. PhysicsComponent           — extrae state directamente (context() = null)
 *
 * Los evaluadores que no necesitan el contexto compuesto funcionan igual
 * que antes: leen via view.has() / view.get() / view.add() sobre PhysicalState.
 * Los evaluadores cinemáticos y de dominio compuesto acceden al contexto:
 *   if (view.context() == null || !view.context().hasKinematic()) continue;
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * El Solver no conoce ningún concepto físico.
 * No distingue tipos de relaciones.
 * No conoce ninguna propiedad concreta.
 * No contiene ningún if, switch ni instanceof sobre la naturaleza de las relaciones.
 *
 * ── WORKINGSTATE + COMMIT ─────────────────────────────────────────────────
 * Durante evaluate(), los evaluadores acumulan deltas vía EvaluationView.add().
 * Esos deltas viven en el WorkingState de cada entidad.
 * Al finalizar TODAS las relaciones del frame, el Solver ejecuta Commit.
 *
 * Esto garantiza:
 *   - Todas las lecturas reflejan el estado al inicio del frame.
 *   - El PhysicalState definitivo solo se modifica en la fase de Commit.
 *   - No existen efectos colaterales derivados del orden de evaluación.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicsSolver {

    private final List<PhysicalRelation> relations = new ArrayList<>();
    private final EvaluatorRegistry      evaluators;
    private       boolean                dirty     = false;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Crea un PhysicsSolver con un registro de evaluadores personalizado.
     *
     * @param evaluators registro de evaluadores. No puede ser null.
     */
    public PhysicsSolver(EvaluatorRegistry evaluators) {
        if (evaluators == null)
            throw new IllegalArgumentException("evaluators no puede ser null");
        this.evaluators = evaluators;
    }

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra una PhysicalRelation.
     *
     * @param relation la relación. Ignorada si null.
     */
    public void addRelation(PhysicalRelation relation) {
        if (relation == null) return;
        relations.add(relation);
        dirty = true;
    }

    /**
     * Registra todas las relaciones de un RelationRegistry.
     *
     * @param registry el registro. Ignorado si null o vacío.
     */
    public void registerAll(RelationRegistry registry) {
        if (registry == null || registry.isEmpty()) return;
        for (PhysicalRelation r : registry.relations()) addRelation(r);
    }

    /** Elimina todas las relaciones registradas. */
    public void clear() {
        relations.clear();
        dirty = false;
    }

    /** Número de relaciones registradas. */
    public int relationCount() { return relations.size(); }

    /** True si no hay relaciones registradas. */
    public boolean isEmpty() { return relations.isEmpty(); }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Resuelve un frame completo de simulación física.
     *
     * Flujo:
     *   1. Crear WorkingState por entidad (snapshot del PhysicalState actual).
     *   2. Construir EvaluationView sobre esos WorkingStates.
     *   3. Ordenar relaciones por prioridad (lazy sort).
     *   4. Por cada relación: obtener evaluador y delegar.
     *   5. Commit: consolidar todos los WorkingStates al PhysicalState real.
     *
     * @param objects   objetos activos en el mundo este frame.
     * @param deltaTime tiempo transcurrido desde el último frame, en segundos.
     */
    public void solve(List<GameObjects> objects, double deltaTime) {
        if (objects == null || objects.isEmpty() || relations.isEmpty()) return;

        if (dirty) {
            relations.sort(Comparator.comparingInt(PhysicalRelation::getPriority));
            dirty = false;
        }

        // ── Paso 1-2: construir WorkingStates y EvaluationViews ───────────
        FrameContext ctx = new FrameContext(objects);
        if (ctx.isEmpty()) return;

        List<RelationEvaluator.EvaluationView> views = ctx.views();

        // ── Paso 3: evaluar cada relación mediante su evaluador ───────────
        for (PhysicalRelation relation : relations) {
            RelationEvaluator evaluator = evaluators.get(relation.getRelationType());
            if (evaluator == null) continue; // sin evaluador registrado = sin efecto
            evaluator.evaluate(relation, views, deltaTime);
        }

        // ── Paso 4: Commit — consolidar WorkingStates → PhysicalState ─────
        ctx.commit();
    }

    // ═════════════════════════════════════════════════════════════════════
    // FrameContext — gestión de WorkingStates para un frame
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Crea y gestiona los WorkingState y las EvaluationView de un frame.
     * Privado al Solver — nada externo lo conoce.
     */
    private static final class FrameContext {

        private final List<EvaluationViewImpl>                views;
        private final List<RelationEvaluator.EvaluationView>  publicViews;

        FrameContext(List<GameObjects> objects) {
            this.views       = new ArrayList<>(objects.size());
            this.publicViews = new ArrayList<>(objects.size());

            for (GameObjects obj : objects) {
                ResolvedState resolved = resolveState(obj);
                if (resolved == null) continue;
                PhysicalState state = resolved.physicalState;
                if (state == null || state.isEmpty()) continue;
                double x = obj.getTransform().getPosition().getX();
                double y = obj.getTransform().getPosition().getY();
                EvaluationViewImpl view = new EvaluationViewImpl(
                    new WorkingState(state), resolved.context, x, y);
                views.add(view);
                publicViews.add(view);
            }
        }

        boolean isEmpty() { return views.isEmpty(); }

        List<RelationEvaluator.EvaluationView> views() { return publicViews; }

        void commit() {
            for (EvaluationViewImpl v : views) v.commit();
        }

        /**
         * Resuelve el PhysicalState y el SimulationContext de un objeto,
         * en orden de prioridad:
         *
         *   1. SimulationContextComponent — contexto compuesto (HRFC-031)
         *   2. PhysicsComponent           — física pura (HRFC-021)
         *
         * Retorna null si el objeto no tiene ninguno de los dos.
         */
        private static ResolvedState resolveState(GameObjects obj) {
            if (obj == null) return null;

            // Prioridad 1: SimulationContextComponent — HRFC-031
            SimulationContextComponent ctxComp =
                obj.getComponent(SimulationContextComponent.class);
            if (ctxComp != null) {
                SimulationContext ctx = ctxComp.getContext();
                return new ResolvedState(ctx.physical(), ctx);
            }

            // Prioridad 2: PhysicsComponent canónico — HRFC-021
            PhysicsComponent pc = obj.getComponent(PhysicsComponent.class);
            if (pc != null) {
                return new ResolvedState(pc.getState(), null);
            }

            return null;
        }
    }

    // ── Par (PhysicalState, SimulationContext) ────────────────────────────

    /** Resultado de la resolución de estado de un objeto. */
    private static final class ResolvedState {
        final PhysicalState    physicalState;
        final SimulationContext context;        // null si no hay contexto compuesto

        ResolvedState(PhysicalState physicalState, SimulationContext context) {
            this.physicalState = physicalState;
            this.context       = context;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // EvaluationViewImpl — implementa EvaluationView sobre WorkingState
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Implementación de EvaluationView delegando en WorkingState.
     *
     * has() y get() leen del snapshot del WorkingState (estado de inicio de frame).
     * add() acumula deltas en el buffer pending del WorkingState.
     * context() expone el SimulationContext si la entidad tiene uno (HRFC-031).
     * commit() consolida el WorkingState al PhysicalState definitivo.
     *
     * No contiene ningún buffer propio. Toda la lógica de staging y commit
     * vive en WorkingState.
     */
    private static final class EvaluationViewImpl
            implements RelationEvaluator.EvaluationView {

        private final WorkingState      workingState;
        private final SimulationContext simulationContext;  // null si no aplica
        private final FrameState        frameState;
        private final double            posX;
        private final double            posY;

        EvaluationViewImpl(WorkingState      workingState,
                           SimulationContext  simulationContext,
                           double            x,
                           double            y) {
            this.workingState      = workingState;
            this.simulationContext = simulationContext;
            this.frameState        = new FrameState();
            this.posX              = x;
            this.posY              = y;
        }

        @Override
        public boolean has(PropertyDescriptor descriptor) {
            return workingState.has(descriptor);
        }

        @Override
        public double get(PropertyDescriptor descriptor) {
            return workingState.get(descriptor);
        }

        @Override
        public void add(PropertyDescriptor descriptor, double delta) {
            workingState.add(descriptor, delta);
        }

        @Override
        public double x() { return posX; }

        @Override
        public double y() { return posY; }

        @Override
        public FrameState frameState() { return frameState; }

        /**
         * El SimulationContext de la entidad, si tiene SimulationContextComponent.
         * Null para entidades con solo PhysicsComponent.
         */
        @Override
        public SimulationContext context() { return simulationContext; }

        void commit() {
            workingState.commit();
            // FrameState es transitorio — se destruye implícitamente con este objeto.
            // El clear() explícito es defensivo para liberar referencias antes del GC.
            frameState.clear();
        }
    }
}
