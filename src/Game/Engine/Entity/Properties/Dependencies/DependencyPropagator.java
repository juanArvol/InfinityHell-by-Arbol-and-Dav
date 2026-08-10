package Game.Engine.Entity.Properties.Dependencies;

import Game.Engine.Entity.Properties.PropertyKey;
import java.util.*;

/**
 * Propagador de cambios a través del PropertyDependencyGraph.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * DependencyPropagator responde a:
 *   "Dada una propiedad que cambió de V_anterior a V_nuevo,
 *    ¿qué cambios deben ocurrir en las demás propiedades?"
 *
 * NO aplica cambios al PropertyMap.
 * NO crea modificadores.
 * NO toca entidades del juego.
 *
 * Solo calcula el PropagationResult: el mapa de PropertyKey → delta.
 *
 * ── IDENTIDAD DE CLAVE ───────────────────────────────────────────────────
 * Los mapas internos usan PropertyKey como clave directamente.
 * Como PropertyKey no sobreescribe equals/hashCode, la JVM usa identidad
 * de referencia de objeto por defecto — equivalente a IdentityHashMap sin
 * necesitar IdentityHashMap explícito.
 *
 * ── ALGORITMO: BFS ITERATIVO ─────────────────────────────────────────────
 * Usa BFS iterativo para:
 *   - Evitar stack overflow con grafos profundos.
 *   - Detectar ciclos con un conjunto "en progreso" por frame.
 *   - Garantizar que cada propiedad se visita una sola vez.
 *   - Acumular deltas cuando múltiples caminos llegan al mismo destino.
 *
 * ── STATELESS ─────────────────────────────────────────────────────────────
 * DependencyPropagator es stateless entre llamadas.
 *
 * @see PropertyDependencyGraph
 * @see PropagationResult
 */
public final class DependencyPropagator {

    private final PropertyDependencyGraph graph;

    public DependencyPropagator(PropertyDependencyGraph graph) {
        if (graph == null)
            throw new IllegalArgumentException("graph no puede ser null.");
        this.graph = graph;
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Propaga el cambio de una propiedad a través del grafo de dependencias.
     * No aplica ningún cambio — solo calcula el PropagationResult.
     */
    public PropagationResult propagate(
            PropertyKey<?> changedKey, double previousValue, double newValue) {

        if (changedKey == null) return PropagationResult.empty();
        if (!graph.hasDependenciesFrom(changedKey)) return PropagationResult.empty();

        // Usamos IdentityHashMap explícito para los conjuntos de visita, ya que
        // necesitamos semántica de referencia garantizada incluso si en el futuro
        // alguien sobreescribe equals/hashCode en una subclase hipotética.
        Map<PropertyKey<?>, Double> accumulatedDeltas = new IdentityHashMap<>();
        Set<PropertyKey<?>>         inProgress        = Collections.newSetFromMap(new IdentityHashMap<>());
        int[] cyclesDetected = { 0 };
        int[] edgesEvaluated = { 0 };

        inProgress.add(changedKey);

        Deque<PropagationStep> queue = new ArrayDeque<>();
        queue.add(new PropagationStep(changedKey, previousValue, newValue));

        while (!queue.isEmpty()) {
            PropagationStep step = queue.poll();
            List<PropertyDependency> deps = graph.getDependenciesFrom(step.key);

            for (PropertyDependency dep : deps) {
                edgesEvaluated[0]++;

                PropertyKey<?> targetKey = dep.getTargetKey();

                if (inProgress.contains(targetKey)) {
                    cyclesDetected[0]++;
                    continue;
                }

                if (!dep.shouldPropagate(step.previousValue, step.newValue)) continue;

                double delta = dep.computeDelta(step.previousValue, step.newValue);

                double currentAccumulated = accumulatedDeltas.getOrDefault(targetKey, 0.0);
                accumulatedDeltas.put(targetKey, currentAccumulated + delta);

                if (graph.hasDependenciesFrom(targetKey) && !inProgress.contains(targetKey)) {
                    inProgress.add(targetKey);
                    queue.add(new PropagationStep(targetKey, 0.0, delta));
                }
            }
        }

        return new PropagationResult(accumulatedDeltas, cyclesDetected[0], edgesEvaluated[0]);
    }

    /**
     * Propaga el cambio de múltiples propiedades en una sola pasada.
     *
     * @param changes mapa de PropertyKey → {previousValue, newValue}
     */
    public PropagationResult propagateAll(Map<PropertyKey<?>, double[]> changes) {
        if (changes == null || changes.isEmpty()) return PropagationResult.empty();

        Map<PropertyKey<?>, Double> mergedDeltas = new IdentityHashMap<>();
        int totalCycles = 0, totalEdges = 0;

        for (Map.Entry<PropertyKey<?>, double[]> entry : changes.entrySet()) {
            double[] vals = entry.getValue();
            if (vals == null || vals.length < 2) continue;

            PropagationResult partial = propagate(entry.getKey(), vals[0], vals[1]);
            for (Map.Entry<PropertyKey<?>, Double> d : partial.allDeltas().entrySet()) {
                mergedDeltas.merge(d.getKey(), d.getValue(), Double::sum);
            }
            totalCycles += partial.getCyclesDetected();
            totalEdges  += partial.getEdgesEvaluated();
        }

        return new PropagationResult(mergedDeltas, totalCycles, totalEdges);
    }

    public PropertyDependencyGraph getGraph() { return graph; }

    // ── Clase interna: paso de propagación ───────────────────────────────

    private static final class PropagationStep {
        final PropertyKey<?> key;
        final double         previousValue;
        final double         newValue;

        PropagationStep(PropertyKey<?> key, double previousValue, double newValue) {
            this.key           = key;
            this.previousValue = previousValue;
            this.newValue      = newValue;
        }
    }
}
