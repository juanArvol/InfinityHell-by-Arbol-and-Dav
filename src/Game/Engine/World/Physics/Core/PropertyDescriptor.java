package Game.Engine.World.Physics.Core;

/**
 * Descriptor declarativo e inmutable de una propiedad física.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 * ── HRFC-024 — Auditoría de Consistencia Arquitectónica ──────────────────
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
 * MASS, RADIATION_LEVEL, VELOCITY_X, VELOCITY_Y son descriptores.
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
 * no del compilador. A cambio, el modelo es completamente plano y extensible.
 *
 * ── INSTANCIACIÓN RESTRINGIDA AL PAQUETE — HRFC-024 ──────────────────────
 * El constructor es package-private. Solo los catálogos de propiedades que
 * residen en este paquete (Game.Engine.World.Physics) pueden crear instancias.
 * Ningún código externo puede fabricar PropertyDescriptor arbitrarios.
 *
 * Este principio es estructural: el compilador lo impone.
 * No depende de convenciones ni documentación.
 *
 * Catálogos autorizados (todos en este paquete):
 *   ThermalProperties
 *   ElectricalProperties
 *   FluidProperties
 *   MechanicalProperties
 *   KinematicProperties
 *   GravityProperties
 *   ElectromagneticProperties
 *   RadiationProperties
 *   MaterialStateProperties
 *   QuantumProperties
 *
 * ── USO COMO CATÁLOGO ────────────────────────────────────────────────────
 *
 *   // En un catálogo de propiedades (mismo paquete que PropertyDescriptor):
 *   public final class GameplayProperties {
 *       public static final PropertyDescriptor MANA =
 *           new PropertyDescriptor("mana", 100.0,
 *               Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
 *               "Energía mágica disponible");
 *   }
 *
 * Los consumidores (evaluadores, componentes del juego) únicamente referencian
 * las constantes de los catálogos. Nunca crean descriptores nuevos.
 *
 *   // Correcto — consumir una constante de catálogo:
 *   view.get(KinematicProperties.VELOCITY_Y);
 *   view.get(GravityProperties.MASS);
 *
 *   // PROHIBIDO — ningún código externo al paquete puede hacer esto:
 *   new PropertyDescriptor(...)              // ← error de compilación
 *
 * ── QUÉ NO CONTIENE ──────────────────────────────────────────────────────
 *   ✗ Ningún parámetro de tipo de dominio.
 *   ✗ Ninguna referencia a CoreDomains ni PhysicalDomain.
 *   ✗ Ninguna distinción entre "propiedad física" y "propiedad de material".
 *   ✗ Ninguna lógica de simulación.
 *   ✗ Ningún getter especializado por fenómeno.
 *   ✗ Ninguna factory pública (of/ofBounded/ofPositive eliminadas en HRFC-024).
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

    // ── Constructor package-private — solo catálogos del paquete pueden instanciar ─
    //
    // HRFC-024: La visibilidad package-private es el mecanismo estructural que
    // garantiza que ningún código externo al paquete Game.Engine.World.Physics
    // pueda crear PropertyDescriptor. El compilador impone esta restricción.
    //
    // Los catálogos autorizados (ThermalProperties, ElectricalProperties, etc.) viven
    // en este mismo paquete y son la única fuente legítima de descriptores.

    PropertyDescriptor(String id,
                       double defaultValue,
                       double min,
                       double max,
                       boolean bounded,
                       String description) {
        if (id == null || id.isBlank())
            throw new IllegalArgumentException("id no puede ser null ni vacío");
        if (bounded && min > max) throw new IllegalArgumentException(
            "min (" + min + ") no puede ser mayor que max (" + max + ")");
        this.id           = id;
        this.defaultValue = bounded ? clamp(defaultValue, min, max) : defaultValue;
        this.min          = min;
        this.max          = max;
        this.bounded      = bounded;
        this.description  = description;
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
