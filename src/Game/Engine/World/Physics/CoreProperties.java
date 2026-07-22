package Game.Engine.World.Physics;

/**
 * Catálogo de descriptores de propiedades físicas fundamentales.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * CoreProperties reemplaza a PhysicalProperties y MaterialProperties,
 * unificando ambos catálogos bajo el único modelo: PropertyDescriptor.
 *
 * No existe distinción entre "propiedades físicas" y "propiedades de material".
 * Todas son descriptores registrados en el mismo PhysicalState del objeto.
 *
 * Las relaciones físicas (CoreRelations) referencian estas constantes por su campo id,
 * que es el string que el sistema de resolución usa para leer y escribir en PhysicalState.
 *
 * ── ESTRUCTURA ────────────────────────────────────────────────────────────
 * Los descriptores están agrupados semánticamente para legibilidad,
 * pero esa agrupación no tiene efecto en el Engine — son simples constantes.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir nuevas propiedades (magnetismo, radiación, espín cuántico...):
 *
 *   // En cualquier catálogo del juego o de un mod:
 *   PropertyDescriptor MAGNETIC_FIELD =
 *       PropertyDescriptor.of("magnetic_field", 0.0, "Intensidad de campo magnético");
 *
 *   PropertyDescriptor RADIATION_LEVEL =
 *       PropertyDescriptor.of("radiation_level", 0.0, "Nivel de radiación acumulada");
 *
 * No modifica CoreProperties. No modifica PhysicsSolver. No modifica PhysicalState.
 * Solo crea una nueva constante y la registra en el PhysicalState del objeto.
 *
 * ── PROPIEDAD DE ESTADO vs. DE MATERIAL ──────────────────────────────────
 * Esta distinción ya no existe en el Core. Sin embargo, conceptualmente:
 *
 *   Propiedades de estado    → valores que cambian durante la simulación.
 *                              Ejemplos: TEMPERATURE, CHARGE, HUMIDITY, PRESSURE.
 *
 *   Propiedades de material  → valores constantes que describen la naturaleza
 *                              del material del objeto.
 *                              Ejemplos: THERMAL_CONDUCTIVITY, HEAT_CAPACITY.
 *
 * Ambas se registran en PhysicalState. El Solver no las distingue.
 * Las leyes pueden leer y escribir cualquiera de ellas con la misma API.
 *
 * En la práctica, las propiedades de material rara vez reciben deltas
 * (son constantes del material), pero el Engine no impone esa restricción.
 */
public final class CoreProperties {

    private CoreProperties() {}

    // ── Propiedades de estado — térmicas ──────────────────────────────────

    /**
     * Temperatura / energía térmica almacenada.
     * 0 = temperatura ambiente. Positiva = más caliente. Negativa = más fría.
     */
    public static final PropertyDescriptor TEMPERATURE =
        PropertyDescriptor.of("temperature", 0.0,
            "Energía térmica almacenada relativa al ambiente");

    // ── Propiedades de estado — eléctricas ────────────────────────────────

    /**
     * Carga eléctrica neta.
     * 0 = neutro. Positiva = carga positiva. Negativa = carga negativa.
     */
    public static final PropertyDescriptor CHARGE =
        PropertyDescriptor.of("charge", 0.0,
            "Carga eléctrica neta acumulada");

    // ── Propiedades de estado — fluídicas ─────────────────────────────────

    /**
     * Contenido fluídico (humedad).
     * Rango natural [0, 1]: 0 = completamente seco, 1 = saturado.
     */
    public static final PropertyDescriptor HUMIDITY =
        PropertyDescriptor.ofBounded("humidity", 0.0, 0.0, 1.0,
            "Contenido fluídico relativo [0=seco, 1=saturado]");

    // ── Propiedades de estado — mecánicas ─────────────────────────────────

    /**
     * Presión local relativa al equilibrio del mundo.
     * 0 = presión ambiente. Positiva = sobrepresión. Negativa = subpresión.
     */
    public static final PropertyDescriptor PRESSURE =
        PropertyDescriptor.of("pressure", 0.0,
            "Presión local relativa al equilibrio ambiental");

    // ── Propiedades de material — térmicas ────────────────────────────────

    /**
     * Conductividad térmica.
     * Velocidad de intercambio de energía térmica con otros objetos.
     * Rango [0, 1]: 0 = aislante perfecto, 1 = conductor perfecto.
     */
    public static final PropertyDescriptor THERMAL_CONDUCTIVITY =
        PropertyDescriptor.ofBounded("thermal_conductivity", 0.1, 0.0, 1.0,
            "Conductividad térmica del material [0=aislante, 1=conductor]");

    /**
     * Capacidad calorífica específica.
     * Resistencia al cambio de temperatura. Mayor = más inercia térmica.
     */
    public static final PropertyDescriptor HEAT_CAPACITY =
        PropertyDescriptor.ofPositive("heat_capacity", 1000.0,
            "Capacidad calorífica específica del material");

    /**
     * Difusividad térmica.
     * Velocidad de disipación de energía térmica hacia el ambiente.
     * Rango [0, 1]: 0 = no disipa, 1 = disipación instantánea.
     */
    public static final PropertyDescriptor THERMAL_DIFFUSIVITY =
        PropertyDescriptor.ofBounded("thermal_diffusivity", 0.1, 0.0, 1.0,
            "Difusividad térmica interna del material");

    /**
     * Punto de fusión en unidades del juego.
     * Temperatura en la que el material cambia de fase sólido→líquido.
     */
    public static final PropertyDescriptor MELTING_POINT =
        PropertyDescriptor.of("melting_point", Double.POSITIVE_INFINITY,
            "Temperatura de fusión en unidades del juego");

    /**
     * Punto de ebullición en unidades del juego.
     * Temperatura en la que el material cambia de fase líquido→gas.
     */
    public static final PropertyDescriptor BOILING_POINT =
        PropertyDescriptor.of("boiling_point", Double.POSITIVE_INFINITY,
            "Temperatura de ebullición en unidades del juego");

    // ── Propiedades de material — eléctricas ──────────────────────────────

    /**
     * Conductividad eléctrica efectiva.
     * Rango [0, 1]: 0 = aislante perfecto, 1 = conductor perfecto.
     */
    public static final PropertyDescriptor ELECTRICAL_CONDUCTIVITY =
        PropertyDescriptor.ofBounded("electrical_conductivity", 0.2, 0.0, 1.0,
            "Conductividad eléctrica del material [0=aislante, 1=conductor]");

    // ── Propiedades de material — fluídicas ───────────────────────────────

    /**
     * Coeficiente de absorción de humedad.
     * Velocidad de absorción/liberación de humedad del entorno.
     * Rango [0, 1]: 0 = impermeable, 1 = absorción instantánea.
     */
    public static final PropertyDescriptor HUMIDITY_ABSORPTION =
        PropertyDescriptor.ofBounded("humidity_absorption", 0.1, 0.0, 1.0,
            "Coeficiente de absorción de humedad [0=impermeable, 1=máxima]");

    /**
     * Viscosidad del material.
     * Resistencia al flujo interno. 0 = fluido ideal.
     */
    public static final PropertyDescriptor VISCOSITY =
        PropertyDescriptor.ofPositive("viscosity", 0.0,
            "Viscosidad del material [0=fluido ideal]");

    // ── Propiedades de material — mecánicas ───────────────────────────────

    /**
     * Compresibilidad.
     * Facilidad de cambio de volumen bajo presión.
     * Rango [0, 1]: 0 = incompresible, 1 = muy compresible.
     */
    public static final PropertyDescriptor COMPRESSIBILITY =
        PropertyDescriptor.ofBounded("compressibility", 0.1, 0.0, 1.0,
            "Compresibilidad del material [0=incompresible, 1=muy compresible]");

    /**
     * Elasticidad.
     * Fracción de energía cinética conservada en colisiones.
     * Rango [0, 1]: 0 = inelástico, 1 = elástico perfecto.
     */
    public static final PropertyDescriptor ELASTICITY =
        PropertyDescriptor.ofBounded("elasticity", 0.3, 0.0, 1.0,
            "Elasticidad del material [0=inelástico, 1=elástico perfecto]");

    /**
     * Dureza.
     * Resistencia a deformación o penetración mecánica.
     * Rango [0, 1]: 0 = muy blando, 1 = muy duro.
     */
    public static final PropertyDescriptor HARDNESS =
        PropertyDescriptor.ofBounded("hardness", 0.5, 0.0, 1.0,
            "Dureza del material [0=blando, 1=duro]");

    /**
     * Densidad relativa del material.
     * Masa por unidad de volumen en unidades del juego.
     */
    public static final PropertyDescriptor DENSITY =
        PropertyDescriptor.ofPositive("density", 1000.0,
            "Densidad del material en unidades relativas del juego");
}
