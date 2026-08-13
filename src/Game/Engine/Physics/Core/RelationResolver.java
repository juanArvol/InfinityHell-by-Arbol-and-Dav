package Game.Engine.Physics.Core;

import Game.Engine.GameObjects;
import java.util.ArrayList;
import java.util.List;

/**
 * Evalúa las relaciones físicas según el plan producido por PropertyResolver.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * RelationResolver es el único sistema que evalúa relaciones físicas.
 *
 * Recibe un ResolutionPlan del PropertyResolver y ejecuta exclusivamente
 * las relaciones indicadas en ese plan — en el orden correcto y con el
 * tratamiento adecuado para ciclos físicos.
 *
 * RelationResolver NO decide qué propiedades recalcular.
 * RelationResolver NO conoce el grafo de dependencias.
 * RelationResolver NO conoce entidades concretas.
 * RelationResolver NO contiene ninguna regla específica de material.
 * RelationResolver NO contiene condiciones del tipo if (material == WATER).
 * RelationResolver NO contiene algoritmos físicos.
 *
 * Lee únicamente propiedades físicas declaradas en las relaciones.
 * Produce únicamente cambios sobre propiedades físicas.
 *
 * ── FLUJO ─────────────────────────────────────────────────────────────────
 *
 *   PropertyResolver.resolve() → ResolutionPlan
 *       ↓
 *   RelationResolver.evaluate(plan, objects, deltaTime)
 *       ↓
 *   Para cada PropagationStep del plan:
 *       Si el step tiene relación asignada → evaluar esa relación puntualmente
 *       Si no → evaluar todas las relaciones que producen esa propiedad
 *       ↓
 *   Para cada propiedad cíclica del plan:
 *       Iterar hasta convergencia (MAX_CYCLE_ITERATIONS)
 *       ↓
 *   Commit: WorkingStates → PhysicalState
 *
 * ── RELACIÓN CON PhysicsSolver ────────────────────────────────────────────
 * RelationResolver delega la mecánica de snapshot/WorkingState/commit/evaluación
 * al PhysicsSolver, que ya implementa ese patrón correctamente (HRFC-022).
 *
 * ── MODO FALLBACK — evaluación completa ──────────────────────────────────
 * Si se invoca evaluateAll(), RelationResolver ejecuta todas las relaciones
 * registradas independientemente del plan. Equivale al comportamiento
 * de PhysicsSolver.solve() directo.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene lógica física.
 *   ✗ No conoce materiales ni tipos de entidad.
 *   ✗ No contiene condiciones sobre propiedades concretas.
 *   ✗ No referencia ningún paradigma histórico de leyes ejecutables.
 *   ✓ Lee propiedades declaradas. Produce cambios sobre propiedades.
 *   ✓ El conocimiento de qué evaluar viene del ResolutionPlan.
 *   ✓ El conocimiento físico vive en los evaluadores especializados.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class RelationResolver {

    /** Solver subyacente que gestiona el patrón WorkingState/commit. */
    private final PhysicsSolver solver;

    /**
     * Registro de relaciones disponibles para evaluación selectiva.
     * RelationResolver selecciona de aquí las relaciones relevantes para cada step.
     */
    private final RelationRegistry registry;

    /**
     * Registro de evaluadores inyectado desde la capa de composición (Physics.World).
     * RelationResolver no conoce evaluadores concretos; solo los propaga al Solver.
     */
    private final EvaluatorRegistry evaluators;

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un RelationResolver con un registro de relaciones y evaluadores dados.
     *
     * @param registry   registro de relaciones. No puede ser null.
     * @param evaluators registro de evaluadores. No puede ser null.
     */
    public RelationResolver(RelationRegistry registry, EvaluatorRegistry evaluators) {
        if (registry == null)
            throw new IllegalArgumentException("registry no puede ser null");
        if (evaluators == null)
            throw new IllegalArgumentException("evaluators no puede ser null");
        this.registry   = registry;
        this.evaluators = evaluators;
        this.solver     = new PhysicsSolver(evaluators);
        for (PhysicalRelation r : registry.relations())
            solver.addRelation(r);
    }

    /** Crea un RelationResolver con un registro vacío y evaluadores dados. */
    public RelationResolver(EvaluatorRegistry evaluators) {
        if (evaluators == null)
            throw new IllegalArgumentException("evaluators no puede ser null");
        this.registry   = new RelationRegistry();
        this.evaluators = evaluators;
        this.solver     = new PhysicsSolver(evaluators);
    }

    // ── Registro de relaciones ────────────────────────────────────────────

    /**
     * Registra una relación física.
     *
     * @param relation la relación a registrar. Ignorada si null.
     */
    public void register(PhysicalRelation relation) {
        if (relation == null) return;
        registry.register(relation);
        solver.addRelation(relation);
    }

    /**
     * Registra todas las relaciones de un RelationRegistry.
     *
     * @param other registro a añadir. Ignorado si null o vacío.
     */
    public void registerAll(RelationRegistry other) {
        if (other == null || other.isEmpty()) return;
        for (PhysicalRelation r : other.relations()) register(r);
    }

    /** Número de relaciones registradas. */
    public int relationCount() { return registry.size(); }

    /** True si no hay relaciones registradas. */
    public boolean isEmpty() { return registry.isEmpty(); }

    // ── Evaluación dirigida por plan ──────────────────────────────────────

    /**
     * Evalúa las relaciones indicadas en el ResolutionPlan sobre los objetos.
     *
     * Flujo:
     *   1. Si el plan está vacío, retorna sin coste.
     *   2. Para cada PropagationStep con relación asignada: evaluar esa
     *      relación sola.
     *   3. Para cada step sin relación explícita: evaluar todas las relaciones
     *      cuyas propiedades participantes incluyen la propiedad destino.
     *   4. Para propiedades cíclicas: iterar hasta convergencia o
     *      MAX_CYCLE_ITERATIONS.
     *
     * @param plan      el plan de resolución del PropertyResolver.
     * @param objects   objetos activos en el mundo este frame.
     * @param deltaTime tiempo del frame en segundos.
     */
    public void evaluate(PhysicsPropertyResolver.ResolutionPlan plan,
                         List<GameObjects>               objects,
                         double                          deltaTime) {
        if (plan == null || plan.isEmpty()) return;
        if (objects == null || objects.isEmpty()) return;
        if (registry.isEmpty()) return;

        // ── Paso acíclico: evaluar cada step en orden ──────────────────
        for (PhysicsPropertyDependencyGraph.PropagationStep step : plan.getSteps()) {
            PhysicalRelation relation = step.getRelation();
            if (relation != null) {
                executeSingleRelation(relation, objects, deltaTime);
            } else {
                PhysicalProperty target = step.getProperty();
                List<PhysicalRelation> relevant = relationsProducing(target);
                if (!relevant.isEmpty())
                    executeRelations(relevant, objects, deltaTime);
            }
        }

        // ── Paso cíclico: iteración convergente ────────────────────────
        if (!plan.getCyclics().isEmpty()) {
            List<PhysicalRelation> cyclicRelations = new ArrayList<>();
            for (PhysicalProperty cyclic : plan.getCyclics()) {
                for (PhysicalRelation r : relationsProducing(cyclic)) {
                    if (!cyclicRelations.contains(r))
                        cyclicRelations.add(r);
                }
            }
            if (!cyclicRelations.isEmpty()) {
                for (int i = 0; i < PhysicsPropertyResolver.MAX_CYCLE_ITERATIONS; i++) {
                    executeRelations(cyclicRelations, objects, deltaTime);
                }
            }
        }
    }

    /**
     * Evaluación completa: evalúa todas las relaciones registradas sobre todos
     * los objetos. Usar para el primer frame o cuando se reinicia el mundo.
     *
     * @param objects   objetos activos.
     * @param deltaTime tiempo del frame en segundos.
     */
    public void evaluateAll(List<GameObjects> objects, double deltaTime) {
        solver.solve(objects, deltaTime);
    }

    // ── Implementación interna ────────────────────────────────────────────

    private void executeSingleRelation(PhysicalRelation  relation,
                                        List<GameObjects> objects,
                                        double            deltaTime) {
        RelationRegistry single = new RelationRegistry().register(relation);
        PhysicsSolver    temp   = new PhysicsSolver(evaluators);
        temp.registerAll(single);
        temp.solve(objects, deltaTime);
    }

    private void executeRelations(List<PhysicalRelation> relations,
                                   List<GameObjects>      objects,
                                   double                 deltaTime) {
        if (relations.isEmpty()) return;
        RelationRegistry batch = new RelationRegistry();
        for (PhysicalRelation r : relations) batch.register(r);
        PhysicsSolver temp = new PhysicsSolver(evaluators);
        temp.registerAll(batch);
        temp.solve(objects, deltaTime);
    }

    /**
     * Retorna todas las relaciones registradas cuyas propiedades participantes
     * incluyen el descriptor de la propiedad destino dada.
     *
     * @param property propiedad destino.
     * @return lista de relaciones que participan en esa propiedad. Nunca null.
     */
    private List<PhysicalRelation> relationsProducing(PhysicalProperty property) {
        List<PhysicalRelation> result = new ArrayList<>();
        PropertyDescriptor target = property.getDescriptor();
        for (PhysicalRelation r : registry.relations()) {
            if (r.getParticipatingProperties().contains(target))
                result.add(r);
        }
        return result;
    }
}
