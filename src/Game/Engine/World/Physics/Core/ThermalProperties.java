package Game.Engine.World.Physics.Core;

/**
 * Catálogo de descriptores de propiedades del dominio térmico.
 *
 * ── DOMINIO FÍSICO ────────────────────────────────────────────────────────
 * Este catálogo modela exclusivamente el fenómeno de la transferencia de
 * energía térmica: cómo un objeto almacena calor, con qué velocidad lo
 * intercambia con su entorno, y las condiciones de cambio de fase.
 *
 * Una propiedad pertenece a este catálogo si y solo si responde a la pregunta:
 *   ¿Describe el comportamiento térmico de un objeto?
 *
 * No se agrupan elementos aquí por cantidad ni por conveniencia histórica.
 * La cohesión semántica del dominio térmico prevalece sobre el tamaño del archivo.
 *
 * ── PROPIEDADES INCLUIDAS ─────────────────────────────────────────────────
 *
 *   TEMPERATURE            → energía térmica almacenada (propiedad de estado)
 *   THERMAL_CONDUCTIVITY   → velocidad de intercambio de calor con otros objetos
 *   HEAT_CAPACITY          → resistencia al cambio de temperatura (inercia térmica)
 *   THERMAL_DIFFUSIVITY    → velocidad de disipación hacia el ambiente
 *   MELTING_POINT          → umbral de cambio de fase sólido → líquido
 *   BOILING_POINT          → umbral de cambio de fase líquido → gas
 *
 * ── CATÁLOGOS DEL SISTEMA ─────────────────────────────────────────────────
 * Cada dominio físico tiene su propio catálogo:
 *
 *   ThermalProperties      → energía térmica y transferencia de calor  ← este
 *   ElectricalProperties   → carga eléctrica y conductividad
 *   FluidProperties        → flujo de masa, humedad y viscosidad
 *   MechanicalProperties   → presión, elasticidad y propiedades del sólido
 *   KinematicProperties    → velocidad y movimiento
 *   GravityProperties      → masa y gravedad
 *   ElectromagneticProperties → campo magnético y superconductividad
 *   RadiationProperties    → radiación y absorción
 *   MaterialStateProperties → transiciones de fase y estados especiales
 *   QuantumProperties      → espín cuántico y función de onda
 */
public final class ThermalProperties {

    private ThermalProperties() {}

    // ── Propiedad de estado ───────────────────────────────────────────────

    /**
     * Temperatura / energía térmica almacenada.
     * 0 = temperatura ambiente. Positiva = más caliente. Negativa = más fría.
     *
     * Es la propiedad de estado central del dominio térmico.
     * Cambia durante la simulación a través de FourierEvaluator,
     * JouleEvaluator, RadiationThermalEvaluator y AmbientDissipationEvaluator.
     */
    public static final PropertyDescriptor TEMPERATURE =
        new PropertyDescriptor("temperature", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Energía térmica almacenada relativa al ambiente");

    // ── Propiedades de material térmico ──────────────────────────────────

    /**
     * Conductividad térmica.
     * Velocidad de intercambio de energía térmica con otros objetos por contacto.
     * Rango [0, 1]: 0 = aislante perfecto, 1 = conductor perfecto.
     *
     * Usada por FourierEvaluator: determina la velocidad de conducción entre pares.
     */
    public static final PropertyDescriptor THERMAL_CONDUCTIVITY =
        new PropertyDescriptor("thermal_conductivity", 0.1, 0.0, 1.0, true,
            "Conductividad térmica del material [0=aislante, 1=conductor]");

    /**
     * Capacidad calorífica específica.
     * Resistencia al cambio de temperatura. Mayor valor = más inercia térmica.
     * El objeto acumula más calor sin elevar tanto su temperatura.
     *
     * Usada por FourierEvaluator, JouleEvaluator, RadiationThermalEvaluator.
     */
    public static final PropertyDescriptor HEAT_CAPACITY =
        new PropertyDescriptor("heat_capacity", 1000.0, 0.0, Double.POSITIVE_INFINITY, true,
            "Capacidad calorífica específica del material");

    /**
     * Difusividad térmica.
     * Velocidad de disipación de energía térmica hacia el ambiente (equilibrio = 0).
     * Rango [0, 1]: 0 = no disipa, 1 = disipación instantánea.
     *
     * Usada por AmbientDissipationEvaluator en la relación THERMAL_AMBIENT_DISSIPATION.
     */
    public static final PropertyDescriptor THERMAL_DIFFUSIVITY =
        new PropertyDescriptor("thermal_diffusivity", 0.1, 0.0, 1.0, true,
            "Difusividad térmica interna del material");

    /**
     * Punto de fusión en unidades del juego.
     * Temperatura en la que el material cambia de fase sólido → líquido.
     * Double.POSITIVE_INFINITY si el material no tiene punto de fusión definido.
     */
    public static final PropertyDescriptor MELTING_POINT =
        new PropertyDescriptor("melting_point", Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Temperatura de fusión en unidades del juego");

    /**
     * Punto de ebullición en unidades del juego.
     * Temperatura en la que el material cambia de fase líquido → gas.
     * Double.POSITIVE_INFINITY si el material no tiene punto de ebullición definido.
     */
    public static final PropertyDescriptor BOILING_POINT =
        new PropertyDescriptor("boiling_point", Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Temperatura de ebullición en unidades del juego");
}
