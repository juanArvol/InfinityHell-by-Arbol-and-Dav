package Game.Engine.Physics.Core;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Estado físico transitorio de una entidad durante la resolución de un frame.
 *
 * ── HRFC-022 — Alineación conceptual de FrameState ───────────────────────
 *
 * ── QUÉ ES FrameState ────────────────────────────────────────────────────
 * FrameState es el estado físico transitorio que una entidad posee durante
 * la resolución de un frame de simulación.
 *
 * No es el estado permanente del mundo — eso es PhysicalState.
 * No es un resultado de la simulación — eso se escribe en PhysicalState.
 * Es el estado intermedio que existe entre evaluadores dentro de un mismo frame.
 *
 * ── RELACIÓN CON PhysicalState ────────────────────────────────────────────
 * PhysicalState y FrameState representan dos tipos distintos de estado,
 * con filosofía idéntica pero duración diferente:
 *
 *   PhysicalState  →  estado persistente del mundo.
 *                     Representa lo que una entidad ES al inicio de cada frame.
 *                     Sobrevive entre frames.
 *                     Se actualiza mediante commit al finalizar la resolución.
 *
 *   FrameState     →  estado transitorio del frame.
 *                     Representa lo que ocurrió DURANTE la resolución de este frame.
 *                     No sobrevive entre frames.
 *                     Es destruido al finalizar la resolución.
 *
 * La simetría conceptual entre ambos es completa e intencional:
 *
 *   physicalState.get(CoreProperties.TEMPERATURE)   frameState.get(FrameMagnitudes.CURRENT)
 *   physicalState.set(CoreProperties.TEMPERATURE, v) frameState.set(FrameMagnitudes.CURRENT, v)
 *   physicalState.add(CoreProperties.TEMPERATURE, d) frameState.add(FrameMagnitudes.CURRENT, d)
 *   physicalState.has(CoreProperties.TEMPERATURE)   frameState.has(FrameMagnitudes.CURRENT)
 *
 * PropertyDescriptor es al PhysicalState lo que FrameMagnitude es al FrameState.
 *
 * ── PARA QUÉ SIRVE ───────────────────────────────────────────────────────
 * FrameState es el medio de comunicación entre evaluadores independientes
 * dentro de un mismo frame.
 *
 * Cuando un fenómeno físico produce un resultado que otro fenómeno necesita
 * consumir en el mismo frame, ese resultado no puede escribirse en PhysicalState
 * (que refleja el estado del frame anterior) ni puede transmitirse por
 * referencia directa entre evaluadores (que no se conocen entre sí).
 *
 * La solución es FrameState: el primer evaluador escribe la magnitud derivada,
 * el segundo la lee, y ambos operan sobre el mismo frame sin acoplarse:
 *
 *   PhysicalState (snapshot del frame anterior)
 *       ↓
 *   OhmEvaluator         → calcula corriente → escribe FrameMagnitudes.CURRENT
 *       ↓
 *   JouleEvaluator       → lee FrameMagnitudes.CURRENT → produce ΔTemperature
 *       ↓
 *   PhysicalState (actualizado al finalizar el frame)
 *
 * Los evaluadores nunca se conocen entre sí.
 * Nunca se llaman entre ellos.
 * Únicamente intercambian magnitudes transitorias a través de FrameState.
 *
 * ── DURACIÓN ─────────────────────────────────────────────────────────────
 * El FrameState de una entidad existe exclusivamente durante la resolución
 * del frame en que fue creado. Al finalizar la resolución, es destruido.
 *
 * Ninguna magnitud registrada en FrameState persiste en PhysicalState.
 * Ninguna magnitud de FrameState forma parte del estado permanente del mundo.
 * Ninguna magnitud de FrameState es visible en el siguiente frame.
 *
 * ── API ───────────────────────────────────────────────────────────────────
 * La única forma de interactuar con FrameState es mediante FrameMagnitude.
 * No existe ningún método que hable en términos de claves, mapas o colecciones.
 * La API habla exclusivamente el lenguaje del dominio físico.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ Nunca persiste entre frames.
 *   ✗ Nunca se convierte en estado de PhysicalState.
 *   ✗ Nunca se serializa.
 *   ✗ Ningún método público acepta String como identificador.
 *   ✗ El mecanismo de almacenamiento interno no forma parte del contrato público.
 *   ✓ Toda interacción ocurre exclusivamente mediante FrameMagnitude.
 *   ✓ Los evaluadores leen y escriben magnitudes físicas, no entradas de un mapa.
 */
public final class FrameState {

    // Detalle de implementación privado.
    // El resto del Engine no debe razonar sobre esta estructura.
    private final Map<FrameMagnitude, Double> values = new IdentityHashMap<>();

    // ── Escritura ─────────────────────────────────────────────────────────

    /**
     * Establece el valor de la magnitud en el estado de este frame.
     * Sobreescribe cualquier valor previo producido durante este frame.
     *
     * Usar cuando el valor representa un resultado puntual y no
     * debe acumularse con otras contribuciones previas.
     *
     * @param magnitude la magnitud a escribir. No puede ser null.
     * @param value     el valor de la magnitud para este frame.
     */
    public void set(FrameMagnitude magnitude, double value) {
        if (magnitude == null) return;
        values.put(magnitude, value);
    }

    /**
     * Acumula una contribución adicional a la magnitud en el estado de este frame.
     * Si la magnitud no tiene valor previo en este frame, equivale a set().
     *
     * Usar cuando múltiples fenómenos o múltiples pares de entidades
     * contribuyen a la misma magnitud sobre una misma entidad durante el frame.
     * Por ejemplo: varias interacciones eléctricas sobre la misma entidad
     * producen corriente acumulable en FrameMagnitudes.CURRENT.
     *
     * @param magnitude la magnitud a la que se añade la contribución. No puede ser null.
     * @param delta     la contribución a acumular.
     */
    public void add(FrameMagnitude magnitude, double delta) {
        if (magnitude == null) return;
        Double prev = values.get(magnitude);
        values.put(magnitude, prev != null ? prev + delta : delta);
    }

    // ── Lectura ───────────────────────────────────────────────────────────

    /**
     * Retorna el valor de la magnitud en el estado de este frame.
     * Retorna 0.0 si la magnitud no tiene valor en este frame.
     *
     * @param magnitude la magnitud a consultar.
     * @return el valor acumulado durante este frame, o 0.0 si no existe.
     */
    public double get(FrameMagnitude magnitude) {
        if (magnitude == null) return 0.0;
        Double v = values.get(magnitude);
        return v != null ? v : 0.0;
    }

    /**
     * True si la magnitud tiene un valor registrado en el estado de este frame.
     *
     * @param magnitude la magnitud a consultar.
     * @return true si existe un valor para esta magnitud en este frame.
     */
    public boolean has(FrameMagnitude magnitude) {
        return magnitude != null && values.containsKey(magnitude);
    }

    // ── Ciclo de frame — acceso restringido al paquete ────────────────────

    /**
     * Destruye el estado de este frame.
     *
     * Invocado por PhysicsSolver al finalizar la resolución del frame.
     * Garantiza que ninguna magnitud transitoria sobreviva entre frames.
     *
     * Package-private: ningún evaluador ni componente externo al paquete
     * puede invocar este método. El ciclo de vida del estado transitorio
     * es responsabilidad exclusiva del sistema de resolución.
     */
    void clear() {
        values.clear();
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "FrameState[" + values.size() + " magnitudes]";
    }
}
