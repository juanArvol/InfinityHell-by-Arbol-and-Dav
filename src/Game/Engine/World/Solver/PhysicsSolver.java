package Game.Engine.World.Solver;

import Game.Engine.GameObjects;
import Game.Engine.World.Physics.PhysicalState;
import Game.Engine.World.Physics.PhysicsLaw;
import Game.Engine.World.Physics.WorldContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de resolución física del World Simulation Core.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── FLUJO DEFINITIVO ──────────────────────────────────────────────────────
 *
 *     PhysicsSolver.solve(objects, deltaTime)
 *         ↓
 *     Construir WorldContext (una sola vez por frame)
 *         ↓
 *     for (PhysicsLaw law : laws)
 *         law.solve(ctx)
 *         volcar deltas → PhysicalState
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * El Solver no conoce ningún concepto físico.
 * No distingue tipos de leyes.
 * No conoce aridad, contactRadius, dominios, ni fenómenos.
 * No prepara contextos distintos para distintas leyes.
 * No contiene ningún if, switch ni instanceof sobre la naturaleza de las leyes.
 *
 * El único trabajo del Solver es:
 *   1. Construir un WorldContext con las entidades activas.
 *   2. Ejecutar cada ley sobre ese contexto, en orden de prioridad.
 *   3. Aplicar los deltas acumulados al PhysicalState real.
 *
 * Eso es todo. El Solver permanece completamente inmutable ante cualquier
 * adición de nuevas leyes, propiedades, dominios o fenómenos físicos.
 *
 * ── ESCRITURA DIFERIDA ────────────────────────────────────────────────────
 * Durante law.solve(ctx), las leyes acumulan deltas sobre EntityView.add().
 * Esos deltas viven en un buffer local por entidad.
 *
 * Al finalizar la ejecución de una ley, el Solver vuelca todos los buffers
 * al PhysicalState real antes de pasar a la siguiente ley.
 *
 * Esto garantiza:
 *   - Dentro de una ley, todos los get() reflejan el estado del inicio de esa ley.
 *   - Entre leyes, los cambios de la ley anterior son visibles.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicsSolver {

    private final List<PhysicsLaw> laws  = new ArrayList<>();
    private       boolean          dirty = false;

    // ── Registro ──────────────────────────────────────────────────────────

    /**
     * Registra una PhysicsLaw.
     *
     * @param law la ley. Ignorado si null.
     */
    public void addLaw(PhysicsLaw law) {
        if (law == null) return;
        laws.add(law);
        dirty = true;
    }

    /**
     * Registra todas las leyes de un LawRegistry.
     *
     * @param registry el registro. Ignorado si null o vacío.
     */
    public void registerAll(LawRegistry registry) {
        if (registry == null || registry.isEmpty()) return;
        for (PhysicsLaw law : registry.laws()) addLaw(law);
    }

    /** Elimina todas las leyes registradas. */
    public void clear() {
        laws.clear();
        dirty = false;
    }

    /** Número de leyes registradas. */
    public int lawCount() { return laws.size(); }

    /** True si no hay leyes registradas. */
    public boolean isEmpty() { return laws.isEmpty(); }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Resuelve un frame completo de simulación física.
     *
     * Construye el WorldContext una sola vez y lo entrega a cada ley en
     * orden de prioridad. Tras cada ley, vuelca los deltas acumulados.
     *
     * @param objects   objetos activos en el mundo este frame.
     * @param deltaTime tiempo transcurrido desde el último frame, en segundos.
     */
    public void solve(List<GameObjects> objects, double deltaTime) {
        if (objects == null || objects.isEmpty() || laws.isEmpty()) return;

        if (dirty) {
            laws.sort(Comparator.comparingInt(PhysicsLaw::getPriority));
            dirty = false;
        }

        // Construir las EntityView una sola vez — todas las leyes comparten
        // el mismo contexto. Cada EntityView acumula sus propios deltas.
        FrameContext ctx = new FrameContext(objects, deltaTime);

        for (PhysicsLaw law : laws) {
            int iterations = law.getIterations();
            for (int i = 0; i < iterations; i++) {
                ctx.resetDeltas();
                law.solve(ctx);
                ctx.applyDeltas();
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Implementación interna de WorldContext
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Implementación concreta de WorldContext para un frame de simulación.
     *
     * Construye una EntityViewImpl por cada objeto con PhysicalState.
     * Las leyes acceden a ellas via entities().
     *
     * Este tipo vive exclusivamente dentro del Solver. Nada externo lo conoce.
     */
    private static final class FrameContext implements WorldContext {

        private final List<EntityViewImpl> views;
        private final List<WorldContext.EntityView> publicViews;
        private final double deltaTime;

        FrameContext(List<GameObjects> objects, double deltaTime) {
            this.deltaTime   = deltaTime;
            this.views       = new ArrayList<>(objects.size());
            this.publicViews = new ArrayList<>(objects.size());

            for (GameObjects obj : objects) {
                PhysicalState state = stateOf(obj);
                if (state == null || state.isEmpty()) continue;
                double x = obj.getTransform().getPosition().getX();
                double y = obj.getTransform().getPosition().getY();
                EntityViewImpl view = new EntityViewImpl(state, x, y);
                views.add(view);
                publicViews.add(view);
            }
        }

        @Override
        public List<WorldContext.EntityView> entities() {
            return publicViews;
        }

        @Override
        public double deltaTime() {
            return deltaTime;
        }

        @Override
        public double distance(WorldContext.EntityView a, WorldContext.EntityView b) {
            if (!(a instanceof EntityViewImpl va)) return 0.0;
            if (!(b instanceof EntityViewImpl vb)) return 0.0;
            double dx = va.x - vb.x;
            double dy = va.y - vb.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        void resetDeltas() {
            for (EntityViewImpl v : views) v.resetDeltas();
        }

        void applyDeltas() {
            for (EntityViewImpl v : views) v.applyDeltas();
        }

        private static PhysicalState stateOf(GameObjects obj) {
            if (obj == null) return null;
            PhysicalStateComponent comp = obj.getComponent(PhysicalStateComponent.class);
            return comp != null ? comp.getState() : null;
        }
    }

    /**
     * Implementación de EntityView con escritura diferida.
     *
     * Lee directamente del PhysicalState.
     * Acumula deltas en un mapa local durante la ejecución de una ley.
     * Vuelca los deltas al PhysicalState solo cuando applyDeltas() es llamado,
     * al finalizar la ejecución de esa ley.
     */
    private static final class EntityViewImpl implements WorldContext.EntityView {

        private final PhysicalState       state;
        final         double              x;
        final         double              y;
        private final Map<String, Double> deltas = new HashMap<>();

        EntityViewImpl(PhysicalState state, double x, double y) {
            this.state = state;
            this.x     = x;
            this.y     = y;
        }

        @Override
        public boolean has(String propertyId) {
            return state.has(propertyId);
        }

        @Override
        public double get(String propertyId) {
            return state.get(propertyId);
        }

        @Override
        public void add(String propertyId, double delta) {
            if (!state.has(propertyId)) return;
            deltas.merge(propertyId, delta, Double::sum);
        }

        void resetDeltas() {
            deltas.clear();
        }

        void applyDeltas() {
            for (Map.Entry<String, Double> entry : deltas.entrySet()) {
                state.add(entry.getKey(), entry.getValue());
            }
            deltas.clear();
        }
    }
}
