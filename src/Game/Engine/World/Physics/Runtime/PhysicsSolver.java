package Game.Engine.World.Physics.Runtime;

import Game.Engine.GameObjects;
import Game.Engine.World.Physics.Core.PhysicalRelation;
import Game.Engine.World.Physics.Core.PhysicalState;
import Game.Engine.World.Physics.Core.PropertyDescriptor;
import Game.Engine.World.Physics.Core.WorkingState;
import Game.Engine.World.Physics.Core.RelationEvaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Motor de resolución física del World Simulation Core.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── FLUJO DEFINITIVO ──────────────────────────────────────────────────────
 *
 *   PhysicsSolver.solve(relations, objects, deltaTime)
 *       ↓
 *   Para cada entidad activa: crear WorkingState (snapshot del PhysicalState)
 *       ↓
 *   Construir lista de EvaluationView (una vez por frame)
 *       ↓
 *   Para cada PhysicalRelation (ordenadas por prioridad):
 *       EvaluatorRegistry.get(relation.getRelationType()) → RelationEvaluator
 *       evaluator.evaluate(relation, views, deltaTime)
 *       ↓
 *   Commit: WorkingState.commit() → PhysicalState definitivo
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * El Solver no conoce ningún concepto físico.
 * No distingue tipos de relaciones.
 * No conoce ninguna propiedad concreta.
 * No contiene ningún if, switch ni instanceof sobre la naturaleza de las relaciones.
 *
 * El único trabajo del Solver es:
 *   1. Crear un WorkingState por entidad activa.
 *   2. Construir las EvaluationView con las vistas de esos WorkingStates.
 *   3. Por cada relación, obtener su evaluador y delegarle la evaluación.
 *   4. Consolidar todos los WorkingStates mediante Commit.
 *
 * Todo el conocimiento físico vive en los evaluadores especializados y en
 * las PhysicalRelation declarativas. El Solver es agnóstico a ambos.
 *
 * ── WORKINGSTATE + COMMIT ─────────────────────────────────────────────────
 * Durante evaluate(), los evaluadores acumulan deltas vía EvaluationView.add().
 * Esos deltas viven en el WorkingState de cada entidad.
 * Al finalizar TODAS las relaciones del frame, el Solver ejecuta Commit.
 *
 * Esto garantiza:
 *   - Todas las lecturas de todos los evaluadores reflejan el estado al
 *     inicio del frame (snapshot inmutable).
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
     * Crea un PhysicsSolver con los evaluadores del Core por defecto.
     */
    public PhysicsSolver() {
        this.evaluators = EvaluatorRegistry.defaults();
    }

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
                PhysicalState state = stateOf(obj);
                if (state == null || state.isEmpty()) continue;
                double x = obj.getTransform().getPosition().getX();
                double y = obj.getTransform().getPosition().getY();
                EvaluationViewImpl view = new EvaluationViewImpl(
                    new WorkingState(state), x, y);
                views.add(view);
                publicViews.add(view);
            }
        }

        boolean isEmpty() { return views.isEmpty(); }

        List<RelationEvaluator.EvaluationView> views() { return publicViews; }

        void commit() {
            for (EvaluationViewImpl v : views) v.commit();
        }

        private static PhysicalState stateOf(GameObjects obj) {
            if (obj == null) return null;
            // PhysicsComponent es el componente canónico (HRFC-021)
            Game.Engine.World.Physics.PhysicsComponent pc =
                obj.getComponent(Game.Engine.World.Physics.PhysicsComponent.class);
            if (pc != null) return pc.getState();
            // PhysicalStateComponent como fallback de compatibilidad
            PhysicalStateComponent legacy =
                obj.getComponent(PhysicalStateComponent.class);
            return legacy != null ? legacy.getState() : null;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // EvaluationViewImpl — implementa EvaluationView sobre WorkingState
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Implementación de EvaluationView delegando completamente en WorkingState.
     *
     * has() y get() leen del snapshot del WorkingState (estado de inicio de frame).
     * add() acumula deltas en el buffer pending del WorkingState.
     * commit() consolida el WorkingState al PhysicalState definitivo.
     *
     * No contiene ningún buffer propio. Toda la lógica de staging y commit
     * vive en WorkingState.
     */
    private static final class EvaluationViewImpl
            implements RelationEvaluator.EvaluationView {

        private final WorkingState workingState;
        private final FrameState   frameState;
        private final double       posX;
        private final double       posY;

        EvaluationViewImpl(WorkingState workingState, double x, double y) {
            this.workingState = workingState;
            this.frameState   = new FrameState();
            this.posX         = x;
            this.posY         = y;
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

        void commit() {
            workingState.commit();
            // FrameState es transitorio — se destruye implícitamente con este objeto.
            // El clear() explícito es defensivo para liberar referencias antes del GC.
            frameState.clear();
        }
    }
}
