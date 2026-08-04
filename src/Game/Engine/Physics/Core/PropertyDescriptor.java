package Game.Engine.Physics.Core;

/**
 * Descriptor inmutable de una propiedad física del mundo.
 *
 * ── HRFC-020 — Consolidación Definitiva del Modelo Declarativo ────────────
 * ── HRFC-027 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PropertyDescriptor es la llave de acceso tipada al PhysicalState.
 *
 * Describe una propiedad física: su identificador, su valor por defecto,
 * sus límites opcionales y su descripción semántica. No contiene lógica
 * de simulación. No conoce entidades. No conoce leyes.
 *
 * Es al PhysicalState lo que FrameMagnitude es al FrameState.
 *
 * ── IDENTIDAD FUERTE ──────────────────────────────────────────────────────
 * La identidad es por referencia de objeto (identity-based equals/hashCode).
 * Dos descriptores con el mismo id textual son objetos distintos.
 *
 * El PhysicalState usa IdentityHashMap indexado por referencia del descriptor.
 * Esto garantiza que solo quien tiene la referencia al descriptor puede leer
 * o escribir esa propiedad — no un String arbitrario con el mismo texto.
 *
 * ── BOUNDED vs UNBOUNDED ─────────────────────────────────────────────────
 * Un descriptor bounded tiene rango [min, max] aplicado automáticamente
 * por PhysicalState.set() y PhysicalState.add() mediante clamp().
 * Un descriptor unbounded acepta cualquier valor double.
 *
 * ── USO EN LOS CATÁLOGOS DE DOMINIO ──────────────────────────────────────
 *
 *   // Propiedad sin límites (temperatura puede ser cualquier valor):
 *   public static final PropertyDescriptor TEMPERATURE =
 *       new PropertyDescriptor("temperature", 0.0,
 *           Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
 *           "Energía térmica almacenada relativa al ambiente");
 *
 *   // Propiedad acotada (conductividad entre 0 y 1):
 *   public static final PropertyDescriptor THERMAL_CONDUCTIVITY =
 *       new PropertyDescriptor("thermal_conductivity", 0.1, 0.0, 1.0, true,
 *           "Conductividad térmica del material [0=aislante, 1=conductor]");
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ningún algoritmo físico.
 *   ✗ Ninguna referencia al Solver ni a evaluadores.
 *   ✗ Ninguna lógica de simulación.
 *   ✗ Ningún acoplamiento a entidades concretas.
 */
public final class PropertyDescriptor {

    /** Identificador único de la propiedad. Nunca null ni vacío. */
    private final String  id;

    /** Valor por defecto al registrar la propiedad en un PhysicalState. */
    private final double  defaultValue;

    /** Límite inferior del rango válido. Ignorado si !bounded. */
    private final double  min;

    /** Límite superior del rango válido. Ignorado si !bounded. */
    private final double  max;

    /**
     * True si esta propiedad tiene un rango acotado [min, max].
     * PhysicalState aplica clamp automáticamente cuando bounded = true.
     */
    private final boolean bounded;

    /** Descripción semántica legible. Puede ser null. */
    private final String  description;

    // ── Constructor público ───────────────────────────────────────────────

    /**
     * Crea un descriptor de propiedad física.
     *
     * @param id           identificador único. No puede ser null ni vacío.
     * @param defaultValue valor por defecto al registrar la propiedad.
     * @param min          límite inferior del rango (ignorado si !bounded).
     * @param max          límite superior del rango (ignorado si !bounded).
     * @param bounded      true si el valor debe mantenerse en [min, max].
     * @param description  descripción semántica legible. Puede ser null.
     */
    public PropertyDescriptor(String  id,
                               double  defaultValue,
                               double  min,
                               double  max,
                               boolean bounded,
                               String  description) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id no puede ser null ni vacío");
        this.id           = id;
        this.defaultValue = defaultValue;
        this.min          = min;
        this.max          = max;
        this.bounded      = bounded;
        this.description  = description;
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Identificador único de la propiedad. Nunca null ni vacío. */
    public String getId() { return id; }

    /** Valor por defecto al registrar la propiedad en un PhysicalState. */
    public double getDefaultValue() { return defaultValue; }

    /** Límite inferior del rango válido. Relevante solo si bounded. */
    public double getMin() { return min; }

    /** Límite superior del rango válido. Relevante solo si bounded. */
    public double getMax() { return max; }

    /**
     * True si esta propiedad tiene un rango acotado [min, max].
     * PhysicalState.set() y PhysicalState.add() aplican clamp automáticamente.
     */
    public boolean isBounded() { return bounded; }

    /** Descripción semántica legible. Puede ser null. */
    public String getDescription() { return description; }

    // ── Clamp ─────────────────────────────────────────────────────────────

    /**
     * Ajusta el valor al rango [min, max] si la propiedad es bounded.
     * Si no es bounded, retorna el valor sin modificar.
     *
     * Usado internamente por PhysicalState.set() y PhysicalState.add().
     *
     * @param value el valor a ajustar.
     * @return valor ajustado al rango, o el mismo valor si !bounded.
     */
    public double clamp(double value) {
        if (!bounded) return value;
        return Math.max(min, Math.min(max, value));
    }

    // ── Object — identidad por referencia ────────────────────────────────

    /**
     * Identidad basada en referencia de objeto.
     * Dos descriptores son iguales si y solo si son el mismo objeto.
     * Garantiza que PhysicalState (IdentityHashMap) funcione correctamente.
     */
    @Override
    public boolean equals(Object o) { return this == o; }

    @Override
    public int hashCode() { return System.identityHashCode(this); }

    @Override
    public String toString() {
        return "PropertyDescriptor[" + id + (bounded ? " [" + min + "," + max + "]" : "") + "]";
    }
}
