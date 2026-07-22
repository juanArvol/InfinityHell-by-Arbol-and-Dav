package Game.Engine.World.Solver;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Almacén transitorio de magnitudes físicas derivadas para un frame de simulación.
 *
 * ── HRFC-022 — Identidad fuerte en FrameState ────────────────────────────
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * FrameState NO es estado del mundo.
 * FrameState NO persiste entre frames.
 * FrameState NO se convierte en propiedades de PhysicalState.
 *
 * FrameState representa exclusivamente magnitudes físicas derivadas calculadas
 * durante la resolución de un frame: corriente eléctrica, flujo de calor,
 * potencia radiante, gradiente de presión, fuerza de empuje, etc.
 *
 * ── IDENTIDAD FUERTE ─────────────────────────────────────────────────────
 * Toda escritura y lectura ocurre exclusivamente a través de FrameMagnitude.
 * No existe ningún método público que acepte String como identificador.
 *
 * Es al FrameState lo que PhysicalState es a PropertyDescriptor.
 * El compilador garantiza consistencia porque los evaluadores referencian
 * directamente las constantes de FrameMagnitudes.
 *
 * ── IMPLEMENTACIÓN ────────────────────────────────────────────────────────
 * Usa IdentityHashMap<FrameMagnitude, Double> por las mismas razones que
 * PhysicalState usa IdentityHashMap<PropertyDescriptor, Double>:
 *   - La igualdad entre descriptores es por referencia (mismo objeto).
 *   - O(1) acceso garantizado.
 *   - Imposible colisión entre descriptores con el mismo id textual.
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * 1. FrameState se crea en PhysicsSolver al inicio del frame por entidad.
 * 2. Los evaluadores escriben magnitudes derivadas durante la evaluación.
 * 3. Evaluadores posteriores las leen desde el mismo FrameState.
 * 4. Al finalizar el frame, commit() limpia el FrameState.
 * 5. El objeto queda inutilizable hasta el próximo frame.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ Nunca se convierte en PropertyDescriptor ni CoreProperty.
 *   ✗ Nunca se serializa ni persiste.
 *   ✗ Ningún método público acepta String.
 *   ✓ Existe únicamente entre la creación del FrameContext y su destrucción.
 *   ✓ Acceso O(1) por identidad de descriptor.
 *   ✓ Valores se acumulan (add) o sobreescriben (put) según necesidad.
 */
public final class FrameState {

    private final Map<FrameMagnitude, Double> values = new IdentityHashMap<>();

    // ── Escritura ─────────────────────────────────────────────────────────

    /**
     * Establece el valor de la magnitud derivada.
     * Sobreescribe cualquier valor previo para ese descriptor.
     *
     * @param magnitude descriptor de la magnitud. No puede ser null.
     * @param value     valor de la magnitud.
     */
    public void put(FrameMagnitude magnitude, double value) {
        if (magnitude == null) return;
        values.put(magnitude, value);
    }

    /**
     * Acumula un valor adicional sobre la magnitud derivada.
     * Si no existe valor previo, equivale a put(magnitude, delta).
     *
     * Usar cuando múltiples pares de objetos contribuyen a la misma magnitud
     * en una entidad (p.ej. varias fuentes de corriente sobre la misma entidad).
     *
     * @param magnitude descriptor de la magnitud. No puede ser null.
     * @param delta     valor a añadir.
     */
    public void add(FrameMagnitude magnitude, double delta) {
        if (magnitude == null) return;
        Double prev = values.get(magnitude);
        values.put(magnitude, prev != null ? prev + delta : delta);
    }

    // ── Lectura ───────────────────────────────────────────────────────────

    /**
     * Retorna el valor de la magnitud derivada.
     * Retorna 0.0 si el descriptor no tiene valor este frame.
     *
     * @param magnitude descriptor de la magnitud.
     * @return valor acumulado, o 0.0 si no existe.
     */
    public double get(FrameMagnitude magnitude) {
        if (magnitude == null) return 0.0;
        Double v = values.get(magnitude);
        return v != null ? v : 0.0;
    }

    /**
     * True si existe un valor para el descriptor indicado este frame.
     *
     * @param magnitude descriptor de la magnitud.
     * @return true si existe.
     */
    public boolean has(FrameMagnitude magnitude) {
        return magnitude != null && values.containsKey(magnitude);
    }

    // ── Ciclo de frame ────────────────────────────────────────────────────

    /**
     * Elimina todas las magnitudes derivadas.
     * Llamado automáticamente por PhysicsSolver al finalizar el frame.
     * No debe ser llamado por los evaluadores.
     */
    public void clear() {
        values.clear();
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "FrameState[" + values.size() + " magnitudes]";
    }
}
