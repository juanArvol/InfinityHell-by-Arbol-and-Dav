package Game.Engine.Entity.Properties.Dependencies;

import Game.Engine.Entity.Properties.PropertyKey;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resultado de una propagación de dependencias a través del grafo.
 *
 * ── RESPONSABILIDAD ÚNICA ─────────────────────────────────────────────────
 * PropagationResult es un objeto de valor inmutable que transporta el
 * resultado completo de una pasada de propagación del DependencyPropagator.
 *
 * ── IDENTIDAD DE CLAVE ───────────────────────────────────────────────────
 * Las claves del mapa de deltas son instancias de PropertyKey, no Strings.
 * La identidad es por referencia de objeto (IdentityHashMap).
 *
 * Los métodos affects() y getDelta() comparan por referencia de instancia,
 * de modo que solo las claves que son exactamente la misma instancia producen
 * un match.
 *
 * @see DependencyPropagator
 * @see PropertyDependencyGraph
 */
public final class PropagationResult {

    /**
     * Mapa de PropertyKey → delta acumulado.
     * Clave: la instancia exacta de PropertyKey afectada.
     * Valor: delta total calculado por el propagador.
     *
     * Usamos LinkedHashMap (no IdentityHashMap) aquí porque PropertyKey
     * ya no sobreescribe equals/hashCode y por tanto usa identidad de objeto
     * por defecto — cualquier HashMap se comporta como IdentityHashMap para
     * instancias sin equals/hashCode sobreescritos.
     */
    private final Map<PropertyKey<?>, Double> deltas;
    private final int                         cyclesDetected;
    private final int                         edgesEvaluated;

    PropagationResult(Map<PropertyKey<?>, Double> deltas,
                      int cyclesDetected,
                      int edgesEvaluated) {
        this.deltas         = Collections.unmodifiableMap(new LinkedHashMap<>(deltas));
        this.cyclesDetected = cyclesDetected;
        this.edgesEvaluated = edgesEvaluated;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public boolean hasChanges() { return !deltas.isEmpty(); }

    /**
     * True si la propagación afecta a la clave indicada.
     * La comparación es por referencia de instancia de PropertyKey.
     */
    public boolean affects(PropertyKey<?> key) {
        return key != null && deltas.containsKey(key);
    }

    /**
     * Delta acumulado para la clave indicada, o 0.0 si no fue afectada.
     * La comparación es por referencia de instancia de PropertyKey.
     */
    public double getDelta(PropertyKey<?> key) {
        if (key == null) return 0.0;
        Double d = deltas.get(key);
        return d != null ? d : 0.0;
    }

    /** Vista no modificable del mapa completo de deltas. */
    public Map<PropertyKey<?>, Double> allDeltas()  { return deltas; }

    public int getCyclesDetected()                  { return cyclesDetected; }
    public int getEdgesEvaluated()                  { return edgesEvaluated; }
    public boolean hadCycles()                      { return cyclesDetected > 0; }

    @Override
    public String toString() {
        return "PropagationResult["
            + "changes=" + deltas.size()
            + ", cycles=" + cyclesDetected
            + ", edges=" + edgesEvaluated
            + "]";
    }

    // ── Resultado vacío (singleton) ───────────────────────────────────────

    static final PropagationResult EMPTY =
        new PropagationResult(Map.of(), 0, 0);

    public static PropagationResult empty() { return EMPTY; }
}
