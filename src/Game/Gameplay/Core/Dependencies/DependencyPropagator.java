package Game.Gameplay.Core.Dependencies;

import Game.Gameplay.Core.Properties.PropertyKey;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Propagador de cambios a través del PropertyDependencyGraph.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * DependencyPropagator responde a:
 *
 *   "Dada una propiedad que cambió de V_anterior a V_nuevo,
 *    ¿qué cambios deben ocurrir en las demás propiedades?"
 *
 * NO aplica cambios al PropertyMap.
 * NO crea modificadores.
 * NO toca entidades del juego.
 *
 * Solo calcula el PropagationResult: el mapa de propiedad → delta para
 * cada propiedad alcanzada por la propagación.
 *
 * Aplicar los deltas al mundo es responsabilidad del caller (ResolutionPipeline).
 *
 * ── ALGORITMO: BFS ITERATIVO ─────────────────────────────────────────────
 * La propagación usa BFS iterativo (no recursivo) para:
 *   - Evitar stack overflow con grafos profundos.
 *   - Detectar ciclos con un conjunto de "en progreso" por frame.
 *   - Garantizar que cada propiedad se visita una sola vez por propagación.
 *   - Acumular deltas cuando múltiples caminos llegan al mismo destino.
 *
 * ── FLUJO CONCEPTUAL ─────────────────────────────────────────────────────
 *
 *   Temperature cambia de 10 a -30
 *       ↓
 *   PropertyDependencyGraph encuentra: Temperature → MovementSpeed
 *       ↓
 *   Condición se cumple: ON_CHANGE → true
 *       ↓
 *   Transform: linear(-0.05) → delta = (-30 - 10) * -0.05 = 2.0
 *       ↓
 *   MovementSpeed debe sumar 2.0
 *       ↓
 *   PropertyDependencyGraph encuentra: MovementSpeed → AttackSpeed
 *       ↓
 *   Continúa con nuevo previousValue=base(speed), newValue=base(speed)+2.0
 *       ↓
 *   ... hasta que no hay más aristas activas
 *       ↓
 *   PropagationResult: { Speed → +2.0, AttackSpeed → ..., ... }
 *
 * ── DETECCIÓN DE CICLOS ──────────────────────────────────────────────────
 * Cada propagación inicia con un conjunto "en progreso" vacío.
 * Cuando se visita una propiedad, se añade al conjunto.
 * Si se intenta visitar una propiedad que ya está en el conjunto,
 * se detecta un ciclo y se corta la propagación en ese punto.
 *
 * Esto garantiza que no hay propagación infinita, sin importar la
 * estructura del grafo (incluyendo grafos con ciclos declarados con
 * addEdgeUnchecked).
 *
 * ── ACUMULACIÓN DE DELTAS ────────────────────────────────────────────────
 * Si múltiples caminos convergen en la misma propiedad, los deltas
 * se ACUMULAN (suman). Esto modela convergencia causal correctamente:
 *
 *   Temperature → Speed (delta +2.0)
 *   Fatigue     → Speed (delta -1.0, si también se propaga en el mismo frame)
 *   Speed total: base + 2.0 + (-1.0) = base + 1.0
 *
 * ── STATELESS ─────────────────────────────────────────────────────────────
 * DependencyPropagator es stateless entre llamadas. Todo el estado de una
 * propagación vive en variables locales. El grafo solo se lee, no se muta.
 *
 * Todos los métodos son de instancia pero el propagador puede reutilizarse
 * para múltiples propagaciones consecutivas sin reseteo.
 *
 * @see PropertyDependencyGraph
 * @see PropagationResult
 */
public final class DependencyPropagator {

    private final PropertyDependencyGraph graph;

    /**
     * Crea un propagador asociado al grafo dado.
     *
     * @param graph grafo de dependencias a usar en la propagación
     * @throws IllegalArgumentException si graph es null
     */
    public DependencyPropagator(PropertyDependencyGraph graph) {
        if (graph == null)
            throw new IllegalArgumentException("graph no puede ser null.");
        this.graph = graph;
    }

    // ── API pública ───────────────────────────────────────────────────────

    /**
     * Propaga el cambio de una propiedad a través del grafo de dependencias.
     *
     * Calcula los deltas que deben aplicarse a todas las propiedades que
     * dependen (directa o transitivamente) de la propiedad origen.
     *
     * No aplica ningún cambio — solo calcula el PropagationResult.
     *
     * @param changedKey    la propiedad que cambió
     * @param previousValue valor anterior de la propiedad
     * @param newValue      valor nuevo de la propiedad
     * @return resultado de la propagación con los deltas calculados
     */
    public PropagationResult propagate(
            PropertyKey<?> changedKey,
            double         previousValue,
            double         newValue) {

        if (changedKey == null) return PropagationResult.empty();
        if (!graph.hasDependenciesFrom(changedKey)) return PropagationResult.empty();

        // Estado acumulado de la propagación
        Map<String, Double>       accumulatedDeltas = new HashMap<>();
        Map<String, PropertyKey<?>> affectedKeys    = new HashMap<>();
        int[]                     cyclesDetected    = { 0 };
        int[]                     edgesEvaluated    = { 0 };

        // Conjunto de propiedades "en progreso" en esta propagación (anti-ciclo)
        Set<String> inProgress = new HashSet<>();
        inProgress.add(changedKey.id());

        // Cola BFS: cada entrada es (key, previousValue, newValue)
        Deque<PropagationStep> queue = new ArrayDeque<>();
        queue.add(new PropagationStep(changedKey, previousValue, newValue));

        while (!queue.isEmpty()) {
            PropagationStep step = queue.poll();
            List<PropertyDependency> deps = graph.getDependenciesFrom(step.key);

            for (PropertyDependency dep : deps) {
                edgesEvaluated[0]++;

                PropertyKey<?> targetKey = dep.getTargetKey();
                String targetId = targetKey.id();

                // Detección de ciclo: si el destino ya está en progreso, cortar
                if (inProgress.contains(targetId)) {
                    cyclesDetected[0]++;
                    continue;
                }

                // Evaluar condición de la dependencia
                if (!dep.shouldPropagate(step.previousValue, step.newValue)) {
                    continue;
                }

                // Calcular delta de la propiedad destino
                double delta = dep.computeDelta(step.previousValue, step.newValue);

                // Acumular el delta (puede llegar por múltiples caminos)
                double currentAccumulated =
                    accumulatedDeltas.getOrDefault(targetId, 0.0);
                double newAccumulated = currentAccumulated + delta;

                accumulatedDeltas.put(targetId, newAccumulated);
                affectedKeys.put(targetId, targetKey);

                // Continuar propagación desde el destino:
                // el "previous" del destino es su valor actual (antes de este delta),
                // el "new" es su valor actual + el delta calculado.
                // Usamos el delta acumulado hasta ahora para la continuación,
                // para que las dependencias transitivas vean el efecto completo.
                double targetPrevious = 0.0;     // valor relativo base (neutro)
                double targetNew      = delta;   // delta de esta arista

                // Solo continuar si el destino tiene dependencias salientes
                // y no está ya en progreso (evitar re-entrar en el mismo nivel)
                if (graph.hasDependenciesFrom(targetKey) && !inProgress.contains(targetId)) {
                    inProgress.add(targetId);
                    queue.add(new PropagationStep(targetKey, targetPrevious, targetNew));
                }
            }
        }

        return new PropagationResult(
            accumulatedDeltas,
            affectedKeys,
            cyclesDetected[0],
            edgesEvaluated[0]
        );
    }

    /**
     * Propaga el cambio de múltiples propiedades en una sola pasada.
     *
     * Los resultados de cada propagación individual se fusionan en un único
     * PropagationResult. Los deltas de propiedades alcanzadas por más de una
     * propagación se acumulan.
     *
     * Útil cuando múltiples propiedades cambian simultáneamente en el mismo
     * frame (ej: una acción afecta Temperature y Mass al mismo tiempo).
     *
     * @param changes mapa de PropertyKey → (previousValue, newValue) pares
     * @return resultado fusionado de todas las propagaciones
     */
    public PropagationResult propagateAll(
            Map<PropertyKey<?>, double[]> changes) {

        if (changes == null || changes.isEmpty()) return PropagationResult.empty();

        Map<String, Double>       mergedDeltas = new HashMap<>();
        Map<String, PropertyKey<?>> mergedKeys = new HashMap<>();
        int totalCycles = 0;
        int totalEdges  = 0;

        for (Map.Entry<PropertyKey<?>, double[]> entry : changes.entrySet()) {
            double[] vals = entry.getValue();
            if (vals == null || vals.length < 2) continue;

            PropagationResult partial =
                propagate(entry.getKey(), vals[0], vals[1]);

            // Fusionar deltas
            for (Map.Entry<String, Double> deltaEntry : partial.allDeltas().entrySet()) {
                mergedDeltas.merge(deltaEntry.getKey(), deltaEntry.getValue(), Double::sum);
            }
            mergedKeys.putAll(partial.affectedKeys());
            totalCycles += partial.getCyclesDetected();
            totalEdges  += partial.getEdgesEvaluated();
        }

        return new PropagationResult(mergedDeltas, mergedKeys, totalCycles, totalEdges);
    }

    // ── Consultas de diagnóstico ──────────────────────────────────────────

    /**
     * Retorna el grafo asociado a este propagador.
     */
    public PropertyDependencyGraph getGraph() {
        return graph;
    }

    // ── Clase interna: paso de propagación ───────────────────────────────

    /**
     * Unidad de trabajo en la cola BFS.
     * Encapsula la propiedad que se propaga y los valores anterior/nuevo
     * de esa propiedad en este nivel del BFS.
     */
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
