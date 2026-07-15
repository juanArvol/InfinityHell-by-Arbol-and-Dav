package Game.Gameplay.Core.Dependencies;

import Game.Gameplay.Core.Properties.PropertyKey;
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
 * Responde a:
 *   "¿Qué propiedades cambiaron y en cuánto, como consecuencia de que
 *    la propiedad X cambió de V_anterior a V_nuevo?"
 *
 * ── CONTENIDO ────────────────────────────────────────────────────────────
 * - Mapa de PropertyKey → delta calculado para cada propiedad que debe
 *   recibir un cambio como consecuencia de la propagación.
 * - La propiedad origen NO está incluida en este mapa (su cambio ya ocurrió).
 * - Las propiedades con delta = 0 se incluyen si la dependencia fue evaluada
 *   y su condición era verdadera pero la transformación retornó 0.
 *   Esto permite distinguir "no propagó" de "propagó sin efecto".
 * - Los ciclos detectados durante la propagación se registran como advertencias
 *   accesibles mediante getCyclesDetected().
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * PropagationResult es completamente inmutable una vez construido.
 * Se crea únicamente por DependencyPropagator.
 *
 * @see DependencyPropagator
 * @see PropertyDependencyGraph
 */
public final class PropagationResult {

    /** Mapa de clave de propiedad destino → delta acumulado. */
    private final Map<String, Double>       deltas;

    /** Mapa de clave de propiedad destino → PropertyKey para acceso tipado. */
    private final Map<String, PropertyKey<?>> keys;

    /** Número de aristas de ciclo detectadas y cortadas durante esta propagación. */
    private final int cyclesDetected;

    /** Número total de aristas de dependencia evaluadas (para diagnóstico). */
    private final int edgesEvaluated;

    // ── Constructor (package-private, solo DependencyPropagator lo crea) ──

    PropagationResult(Map<String, Double> deltas,
                      Map<String, PropertyKey<?>> keys,
                      int cyclesDetected,
                      int edgesEvaluated) {
        this.deltas         = Collections.unmodifiableMap(new LinkedHashMap<>(deltas));
        this.keys           = Collections.unmodifiableMap(new LinkedHashMap<>(keys));
        this.cyclesDetected = cyclesDetected;
        this.edgesEvaluated = edgesEvaluated;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * True si alguna propiedad tiene un delta calculado (incluyendo delta = 0
     * de dependencias cuya condición fue verdadera pero la transformación retornó 0).
     */
    public boolean hasChanges() {
        return !deltas.isEmpty();
    }

    /**
     * True si la propiedad indicada tiene un delta en este resultado.
     *
     * @param key propiedad a consultar
     */
    public boolean affects(PropertyKey<?> key) {
        return key != null && deltas.containsKey(key.id());
    }

    /**
     * Retorna el delta calculado para la propiedad indicada.
     * Si la propiedad no fue afectada, retorna 0.0.
     *
     * @param key propiedad a consultar
     * @return delta acumulado para esa propiedad, o 0.0 si no fue afectada
     */
    public double getDelta(PropertyKey<?> key) {
        if (key == null) return 0.0;
        return deltas.getOrDefault(key.id(), 0.0);
    }

    /**
     * Vista no modificable del mapa completo id → delta.
     * Útil para iterar sobre todos los cambios sin conocer las claves de antemano.
     */
    public Map<String, Double> allDeltas() {
        return deltas;
    }

    /**
     * Vista no modificable del mapa id → PropertyKey para acceso tipado.
     */
    public Map<String, PropertyKey<?>> affectedKeys() {
        return keys;
    }

    /**
     * Número de ciclos detectados y cortados durante la propagación.
     * 0 en una propagación limpia. > 0 indica dependencias cíclicas en el grafo.
     */
    public int getCyclesDetected() {
        return cyclesDetected;
    }

    /**
     * Número total de aristas de dependencia evaluadas durante la propagación.
     * Útil para diagnóstico de rendimiento y profundidad de propagación.
     */
    public int getEdgesEvaluated() {
        return edgesEvaluated;
    }

    /**
     * True si se detectó al menos un ciclo durante la propagación.
     * Los ciclos son cortados automáticamente — la propagación nunca es infinita.
     */
    public boolean hadCycles() {
        return cyclesDetected > 0;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "PropagationResult["
            + "changes=" + deltas.size()
            + ", cycles=" + cyclesDetected
            + ", edges=" + edgesEvaluated
            + "]";
    }

    // ── Resultado vacío (singleton) ───────────────────────────────────────

    /** Resultado vacío: ninguna propiedad cambió. */
    static final PropagationResult EMPTY =
        new PropagationResult(Map.of(), Map.of(), 0, 0);

    /** Retorna un resultado vacío. */
    public static PropagationResult empty() {
        return EMPTY;
    }
}
