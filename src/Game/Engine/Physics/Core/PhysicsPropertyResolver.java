package Game.Engine.Physics.Core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resuelve qué propiedades deben recalcularse y en qué orden.
 *
 * ── HRFC-021 — Property-Driven Physics Architecture ───────────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PropertyResolver es el único sistema que sabe qué propiedades necesitan
 * ser reevaluadas en un frame dado y en qué secuencia.
 *
 * Lo hace únicamente consultando el PropertyDependencyGraph.
 * No contiene ningún algoritmo físico.
 * No conoce entidades.
 * No conoce materiales.
 * No contiene ninguna regla específica de objeto.
 *
 * ── FLUJO ─────────────────────────────────────────────────────────────────
 *
 *   1. El RelationResolver o PhysicsCoordinator notifica que una propiedad cambió:
 *      resolver.markChanged(physicalProperty)
 *
 *   2. PropertyResolver consulta el grafo de dependencias para conocer el
 *      orden de propagación desde esa propiedad:
 *      graph.propagationOrderFrom(property)
 *
 *   3. Construye un ResolutionPlan: lista ordenada de propiedades que el
 *      RelationResolver debe evaluar, respetando prioridades y distinguiendo
 *      propiedades cíclicas (que requieren iteración convergente).
 *
 *   4. El RelationResolver ejecuta las leyes en el orden indicado por el plan.
 *
 * ── DETECCIÓN DE CAMBIOS ─────────────────────────────────────────────────
 * PropertyResolver compara los valores de inicio de frame con los valores
 * resultantes. Si una propiedad cambió más de su umbral de sensibilidad,
 * la marca como "changed" y propaga sus dependientes.
 *
 * Esto garantiza que propiedades en equilibrio (sin cambio neto) no provocan
 * recálculos en cascada innecesarios — el sistema solo evalúa lo que
 * realmente necesita actualizarse.
 *
 * ── CICLOS FÍSICOS ────────────────────────────────────────────────────────
 * Cuando el grafo contiene ciclos (corriente → calor → conductividad → corriente),
 * PropertyResolver los gestiona mediante iteración convergente:
 *
 *   - Las propiedades cíclicas se evalúan hasta MAX_CYCLE_ITERATIONS veces.
 *   - En cada iteración se compara el valor con la iteración anterior.
 *   - Si el cambio cae por debajo de CONVERGENCE_THRESHOLD, se detiene.
 *
 * Esto modela la convergencia física real de los ciclos de retroalimentación.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene lógica física.
 *   ✗ No conoce materiales ni tipos de entidad.
 *   ✗ No contiene condiciones del tipo if (property == TEMPERATURE).
 *   ✓ Solo consulta el grafo y construye planes de resolución.
 *   ✓ Todo el conocimiento de qué depende de qué vive en el grafo.
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class PhysicsPropertyResolver {

    /**
     * Número máximo de iteraciones para resolver ciclos físicos.
     * Evita bucles infinitos en ciclos que no convergen.
     */
    public static final int MAX_CYCLE_ITERATIONS = 8;

    /**
     * Umbral de convergencia para ciclos físicos.
     * Si el delta absoluto entre iteraciones cae por debajo de este valor,
     * el ciclo se considera convergido.
     */
    public static final double CONVERGENCE_THRESHOLD = 1e-6;

    /** Grafo de dependencias del universo. */
    private final PhysicsPropertyDependencyGraph graph;

    /**
     * Conjunto de propiedades marcadas como cambiadas en el frame actual.
     * Usa IdentityHashMap para mantener consistencia con PhysicalState.
     */
    private final Set<PhysicalProperty> changedProperties =
        Collections.newSetFromMap(new IdentityHashMap<>());

    // ── Constructor ───────────────────────────────────────────────────────

    /**
     * Crea un PropertyResolver asociado al grafo de dependencias dado.
     *
     * @param graph el grafo de dependencias del universo. No puede ser null.
     */
    public PhysicsPropertyResolver(PhysicsPropertyDependencyGraph graph) {
        if (graph == null)
            throw new IllegalArgumentException("graph no puede ser null");
        this.graph = graph;
    }

    // ── Marcado de cambios ────────────────────────────────────────────────

    /**
     * Marca una propiedad como cambiada en el frame actual.
     *
     * El PropertyResolver propagará sus dependientes en el ResolutionPlan.
     * Llamar este método para cada propiedad cuyo valor haya cambiado al
     * inicio de un frame (por influencias externas, campos, eventos...).
     *
     * @param property la propiedad que cambió. Ignorado si null.
     */
    public void markChanged(PhysicalProperty property) {
        if (property != null) changedProperties.add(property);
    }

    /**
     * Marca un conjunto de propiedades como cambiadas.
     *
     * @param properties propiedades que cambiaron. Ignorado si null.
     */
    public void markChanged(PhysicalProperty... properties) {
        if (properties == null) return;
        for (PhysicalProperty p : properties)
            markChanged(p);
    }

    /**
     * Limpia el registro de propiedades cambiadas.
     * Llamar al inicio de cada frame o después de consumir el ResolutionPlan.
     */
    public void clearChanges() {
        changedProperties.clear();
    }

    // ── Resolución ────────────────────────────────────────────────────────

    /**
     * Construye el ResolutionPlan para el frame actual.
     *
     * El plan contiene, en orden de evaluación, todas las propiedades que
     * el RelationResolver debe recalcular como consecuencia de los cambios
     * marcados mediante {@link #markChanged}.
     *
     * Las propiedades cíclicas aparecen en el plan con su flag de ciclo
     * activado; el RelationResolver las evalúa de forma iterativa.
     *
     * @return plan de resolución ordenado. Nunca null.
     */
    public ResolutionPlan resolve() {
        if (changedProperties.isEmpty() || graph.isEmpty())
            return ResolutionPlan.empty();

        // Usar LinkedHashSet para mantener orden de inserción y evitar duplicados
        Set<PhysicalProperty>                         seen     = Collections.newSetFromMap(new IdentityHashMap<>());
        List<PhysicsPropertyDependencyGraph.PropagationStep> steps    = new ArrayList<>();
        List<PhysicalProperty>                        cyclics  = new ArrayList<>();

        for (PhysicalProperty changed : changedProperties) {
            List<PhysicsPropertyDependencyGraph.PropagationStep> propagation =
                graph.propagationOrderFrom(changed);

            for (PhysicsPropertyDependencyGraph.PropagationStep step : propagation) {
                PhysicalProperty prop = step.getProperty();
                if (step.isCyclic()) {
                    if (!cyclics.contains(prop)) cyclics.add(prop);
                } else if (!seen.contains(prop)) {
                    seen.add(prop);
                    steps.add(step);
                }
            }
        }

        return new ResolutionPlan(
            Collections.unmodifiableList(steps),
            Collections.unmodifiableList(cyclics)
        );
    }

    /**
     * Detecta qué propiedades de una entidad cambiaron entre el snapshot de
     * inicio de frame y el estado resultante tras la ejecución de leyes.
     *
     * Marca automáticamente como changed cada propiedad cuyo delta absoluto
     * supera el umbral de sensibilidad dado. Usar en la fase de post-commit
     * para alimentar el siguiente frame del resolver.
     *
     * @param state      el PhysicalState de la entidad.
     * @param snapshot   mapa de valores al inicio del frame (descriptor → valor).
     * @param properties las PhysicalProperty registradas en el grafo que
     *                   corresponden a las propiedades del estado.
     * @param threshold  umbral mínimo de cambio para marcar la propiedad.
     *                   Usar {@link #CONVERGENCE_THRESHOLD} por defecto.
     */
    public void detectAndMarkChanges(
            PhysicalState                        state,
            Map<PropertyDescriptor, Double>      snapshot,
            List<PhysicalProperty>               properties,
            double                               threshold) {
        if (state == null || snapshot == null || properties == null) return;
        for (PhysicalProperty prop : properties) {
            PropertyDescriptor desc     = prop.getDescriptor();
            Double             before   = snapshot.get(desc);
            if (before == null) continue;
            double current = state.get(desc);
            if (Math.abs(current - before) > threshold)
                markChanged(prop);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // ResolutionPlan
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Plan de resolución producido por PropertyResolver.
     *
     * Contiene dos listas:
     *
     *   steps   → propiedades a evaluar en orden topológico (sin ciclos).
     *             El RelationResolver ejecuta cada una una sola vez, en secuencia.
     *
     *   cyclics → propiedades que forman parte de ciclos físicos.
     *             El RelationResolver las evalúa iterativamente hasta convergencia
     *             (máximo MAX_CYCLE_ITERATIONS iteraciones).
     *
     * El RelationResolver consume este plan. El PropertyResolver no lo ejecuta.
     */
    public static final class ResolutionPlan {

        /** Pasos de propagación en orden topológico. Sin ciclos. */
        private final List<PhysicsPropertyDependencyGraph.PropagationStep> steps;

        /** Propiedades que participan en ciclos físicos. */
        private final List<PhysicalProperty> cyclics;

        /** Plan vacío singleton. */
        private static final ResolutionPlan EMPTY =
            new ResolutionPlan(Collections.emptyList(), Collections.emptyList());

        ResolutionPlan(List<PhysicsPropertyDependencyGraph.PropagationStep> steps,
                       List<PhysicalProperty>                        cyclics) {
            this.steps   = steps;
            this.cyclics = cyclics;
        }

        /** Plan vacío. El RelationResolver no tiene nada que hacer. */
        public static ResolutionPlan empty() { return EMPTY; }

        /**
         * Pasos de propagación en orden topológico.
         *
         * @return lista inmutable de PropagationStep. Nunca null.
         */
        public List<PhysicsPropertyDependencyGraph.PropagationStep> getSteps() {
            return steps;
        }

        /**
         * Propiedades participantes en ciclos físicos.
         * El RelationResolver las evalúa por iteración convergente.
         *
         * @return lista inmutable de PhysicalProperty. Nunca null.
         */
        public List<PhysicalProperty> getCyclics() {
            return cyclics;
        }

        /** True si el plan no tiene ningún paso que ejecutar. */
        public boolean isEmpty() {
            return steps.isEmpty() && cyclics.isEmpty();
        }

        /** Número total de pasos (no cíclicos). */
        public int stepCount() { return steps.size(); }

        /** Número de propiedades en ciclos. */
        public int cyclicCount() { return cyclics.size(); }

        @Override
        public String toString() {
            return "ResolutionPlan[steps=" + steps.size()
                + " cyclics=" + cyclics.size() + "]";
        }
    }
}
