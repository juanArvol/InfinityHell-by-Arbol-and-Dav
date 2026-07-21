package Game.Engine.World.Physics;

/**
 * Descriptor declarativo e inmutable de una propiedad física.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * PropertyDescriptor unifica lo que antes eran dos modelos separados:
 *
 *   ANTES                                    AHORA
 *   ─────────────────────────────────────    ──────────────────────────────
 *   PhysicalProperty<D extends Domain>    →  PropertyDescriptor
 *   MaterialProperty<V>                   →  PropertyDescriptor
 *
 * La distinción entre "propiedad física" y "propiedad de material" no existe
 * en el Core. Ambas son simplemente descriptores registrados con un identificador,
 * un valor por defecto, límites opcionales y una descripción legible.
 *
 * TEMPERATURE, CHARGE, HUMIDITY, PRESSURE son descriptores.
 * THERMAL_CONDUCTIVITY, HEAT_CAPACITY, COMPRESSIBILITY son descriptores.
 * MAGNETISM, RADIATION, VELOCITY son descriptores.
 * Todos son tratados exactamente igual.
 *
 * ── ESTRUCTURA ────────────────────────────────────────────────────────────
 *
 *   id           → identificador único de texto. Es la clave en PhysicalState
 *                  y en los parámetros de LawEquation.
 *
 *   defaultValue → valor inicial que PhysicalState usa al registrar esta
 *                  propiedad sin valor explícito.
 *
 *   min / max    → límites opcionales del valor. Si bounded=true, PhysicalState
 *                  clampea automáticamente los valores al aplicar deltas.
 *                  Si bounded=false, el valor puede crecer sin restricción.
 *
 *   description  → texto legible para depuración. Nunca afecta al comportamiento.
 *
 * ── SIN DOMINIO GENÉRICO ─────────────────────────────────────────────────
 * PhysicalProperty usaba un parámetro de tipo D para garantizar type-safety
 * entre dominios en tiempo de compilación. Ese mecanismo desaparece porque:
 *
 *   1. LawEquation trabaja con strings, no con tipos generics.
 *   2. PhysicalState almacena doubles, no PhysicalQuantity<D>.
 *   3. El Solver es agnóstico al dominio — no hay código especializado por tipo.
 *
 * La responsabilidad de usar las propiedades correctas es del autor de la ley,
 * no del compilador. A cambio, el modelo es completamente plano y extensible:
 * cualquier string es una propiedad válida sin modificar ningún tipo del Core.
 *
 * ── USO COMO CATÁLOGO ────────────────────────────────────────────────────
 *
 *   // En un catálogo de propiedades del juego:
 *   public final class PhysicsProperties {
 *       public static final PropertyDescriptor TEMPERATURE =
 *           PropertyDescriptor.of("temperature", 0.0, "Energía térmica almacenada");
 *
 *       public static final PropertyDescriptor HUMIDITY =
 *           PropertyDescriptor.ofBounded("humidity", 0.0, 0.0, 1.0,
 *               "Contenido fluídico [0=seco, 1=saturado]");
 *
 *       public static final PropertyDescriptor THERMAL_CONDUCTIVITY =
 *           PropertyDescriptor.ofBounded("thermal_conductivity", 0.1, 0.0, 1.0,
 *               "Conductividad térmica del material");
 *   }
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una propiedad completamente nueva:
 *
 *   PropertyDescriptor.of("schwarzschild_radius", 0.0, "Radio de Schwarzschild")
 *   PropertyDescriptor.of("quantum_spin", 0.5, "Espín cuántico")
 *   PropertyDescriptor.of("plasma_temperature", 0.0, "Temperatura de plasma")
 *
 * No modifica PhysicsSolver. No modifica PhysicalState. No modifica ningún
 * tipo del Core. Solo crea una nueva constante.
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ningún parámetro de tipo de dominio.
 *   ✗ Ninguna referencia a CoreDomains ni PhysicalDomain.
 *   ✗ Ninguna distinción entre "propiedad física" y "propiedad de material".
 *   ✗ Ninguna lógica de simulación.
 *   ✗ Ningún getter especializado por fenómeno.
 */
public final class PropertyDescriptor {

    /** Identificador único. Es la clave en PhysicalState y LawContext. */
    private final String  id;

    /** Valor inicial cuando se registra la propiedad sin valor explícito. */
    private final double  defaultValue;

    /** Límite inferior. Double.NEGATIVE_INFINITY si no acotado. */
    private final double  min;

    /** Límite superior. Double.POSITIVE_INFINITY si no acotado. */
    private final double  max;

    /** True si el valor está acotado entre min y max. */
    private final boolean bounded;

    /** Descripción legible. Puede ser null. Solo para depuración. */
    private final String  description;

    // ── Constructor privado ────────────────────────────────────────────────

    private PropertyDescriptor(String id,
                                double defaultValue,
                                double min,
                                double max,
                                boolean bounded,
                                String description) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id no puede ser null ni vacío");
        this.id           = id;
        this.defaultValue = defaultValue;
        this.min          = min;
        this.max          = max;
        this.bounded      = bounded;
        this.description  = description;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Descriptor sin límites de valor.
     * El valor puede crecer o decrecer sin restricción.
     * Usar para temperatura, carga, presión, velocidad y cualquier magnitud
     * cuyo rango natural sea ilimitado.
     *
     * @param id           identificador único. No puede ser null ni vacío.
     * @param defaultValue valor inicial por defecto.
     * @param description  descripción legible (puede ser null).
     * @return descriptor configurado.
     */
    public static PropertyDescriptor of(String id, double defaultValue, String description) {
        return new PropertyDescriptor(id, defaultValue,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, description);
    }

    /**
     * Descriptor sin límites y sin descripción.
     *
     * @param id           identificador único.
     * @param defaultValue valor inicial por defecto.
     * @return descriptor configurado.
     */
    public static PropertyDescriptor of(String id, double defaultValue) {
        return of(id, defaultValue, null);
    }

    /**
     * Descriptor con límites de valor [min, max].
     * PhysicalState clampea automáticamente los valores al aplicar deltas.
     * Usar para humedad [0,1], conductividad [0,1], ángulos [0,360], etc.
     *
     * @param id           identificador único.
     * @param defaultValue valor inicial (se clampea al rango).
     * @param min          límite inferior.
     * @param max          límite superior.
     * @param description  descripción legible (puede ser null).
     * @return descriptor configurado.
     */
    public static PropertyDescriptor ofBounded(String id,
                                               double defaultValue,
                                               double min,
                                               double max,
                                               String description) {
        if (min > max) throw new IllegalArgumentException(
            "min (" + min + ") no puede ser mayor que max (" + max + ")");
        return new PropertyDescriptor(id, clamp(defaultValue, min, max),
            min, max, true, description);
    }

    /**
     * Descriptor acotado con límite inferior en 0.
     * Usar para magnitudes que no pueden ser negativas: masa, densidad, capacidades.
     *
     * @param id           identificador único.
     * @param defaultValue valor inicial (mínimo 0).
     * @param description  descripción legible (puede ser null).
     * @return descriptor configurado.
     */
    public static PropertyDescriptor ofPositive(String id,
                                                double defaultValue,
                                                String description) {
        return ofBounded(id, defaultValue, 0.0, Double.POSITIVE_INFINITY, description);
    }

    // ── Accesores ─────────────────────────────────────────────────────────

    /** Identificador único de la propiedad. La clave en PhysicalState y LawContext. */
    public String  getId()           { return id; }

    /** Valor inicial cuando se registra sin valor explícito. */
    public double  getDefaultValue() { return defaultValue; }

    /** Límite inferior del valor. Double.NEGATIVE_INFINITY si no acotado. */
    public double  getMin()          { return min; }

    /** Límite superior del valor. Double.POSITIVE_INFINITY si no acotado. */
    public double  getMax()          { return max; }

    /** True si el valor está acotado entre min y max. */
    public boolean isBounded()       { return bounded; }

    /** Descripción legible. Puede ser null. */
    public String  getDescription()  { return description; }

    /**
     * Clampea el valor al rango de este descriptor.
     * Si no está acotado, retorna el valor sin cambios.
     *
     * @param value valor a clampear.
     * @return valor dentro del rango válido.
     */
    public double clamp(double value) {
        return bounded ? clamp(value, min, max) : value;
    }

    // ── Object ────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        if (bounded) {
            return "PropertyDescriptor[" + id + " default=" + defaultValue
                + " in [" + min + ", " + max + "]]";
        }
        return "PropertyDescriptor[" + id + " default=" + defaultValue + "]";
    }

    // ── Impl ──────────────────────────────────────────────────────────────

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
