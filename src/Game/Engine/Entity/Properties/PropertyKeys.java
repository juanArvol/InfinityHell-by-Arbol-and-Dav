package Game.Engine.Entity.Properties;

/**
 * Catálogo de claves de propiedad del núcleo de Infinity Hell.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Define las propiedades modificables fundamentales que son transversales
 * a todos los sistemas del juego. Son los "parámetros del universo" sobre
 * los cuales los modificadores operan.
 *
 * ── HRFC-015 — World Simulation Core ──────────────────────────────────────
 * Se añaden las claves del dominio físico del mundo. Estas claves representan
 * propiedades físicas fundamentales — no fenómenos de gameplay.
 *
 * La distinción es esencial: TEMPERATURE no es "el personaje está quemado".
 * Es la temperatura actual del objeto en el mundo. Los fenómenos observables
 * (Burning, Frozen, Electrified…) emergen cuando los módulos de simulación
 * evalúan cómo estas propiedades interactúan con el material de cada entidad.
 *
 * ── CÓMO EXTENDER ────────────────────────────────────────────────────────
 * Nuevos sistemas declaran sus propias claves en su propio catálogo:
 *
 *   // En un módulo de magia:
 *   public final class SpellPropertyKeys {
 *       public static final PropertyKey<Double> MANA_COST =
 *           PropertyKey.of("SpellManaCost", Double.class, 0.0);
 *       public static final PropertyKey<Double> CAST_TIME =
 *           PropertyKey.of("SpellCastTime", Double.class, 1.0);
 *   }
 *
 * No es necesario registrar las claves en ningún sistema central. Una clave
 * existe en el momento en que se declara como constante estática.
 *
 * ── VALORES POR DEFECTO ───────────────────────────────────────────────────
 * Cada clave tiene un valor por defecto. Este es el valor que retorna
 * PropertyMap.getBase() cuando la clave no ha sido registrada explícitamente.
 * Los valores por defecto son neutros: no dañan, no mueven, no enfrían.
 *
 * ── NAMING ────────────────────────────────────────────────────────────────
 * Los IDs son PascalCase sin puntos: "Damage", "Speed", "Cooldown".
 * Esto los distingue de los IDs de tags que usan notación de punto.
 */
public final class PropertyKeys {

    private PropertyKeys() {}

    // ── Combate ───────────────────────────────────────────────────────────

    /** Daño base que aplica una entidad por impacto o acción. */
    public static final PropertyKey<Double> DAMAGE =
        PropertyKey.of("Damage", Double.class, 0.0);

    /** Probabilidad de golpe crítico en [0.0, 1.0]. */
    public static final PropertyKey<Double> CRITICAL_CHANCE =
        PropertyKey.of("CriticalChance", Double.class, 0.0);

    /** Multiplicador de daño en golpe crítico. */
    public static final PropertyKey<Double> CRITICAL_MULTIPLIER =
        PropertyKey.of("CriticalMultiplier", Double.class, 2.0);

    /** Número de entidades adicionales que puede penetrar un proyectil. */
    public static final PropertyKey<Double> PENETRATION =
        PropertyKey.of("Penetration", Double.class, 0.0);

    // ── Movimiento ────────────────────────────────────────────────────────

    /** Velocidad de movimiento base en unidades/frame. */
    public static final PropertyKey<Double> SPEED =
        PropertyKey.of("Speed", Double.class, 1.0);

    /** Masa de la entidad. Influye en knockback y aceleración. */
    public static final PropertyKey<Double> MASS =
        PropertyKey.of("Mass", Double.class, 1.0);

    /** Elasticidad al rebotar contra superficies (0 = sin rebote, 1 = perfecto). */
    public static final PropertyKey<Double> ELASTICITY =
        PropertyKey.of("Elasticity", Double.class, 0.0);

    // ── Temporización ─────────────────────────────────────────────────────

    /** Tiempo de reutilización en frames. */
    public static final PropertyKey<Double> COOLDOWN =
        PropertyKey.of("Cooldown", Double.class, 0.0);

    /** Duración de vida en frames. Para proyectiles, efectos, invocaciones. */
    public static final PropertyKey<Double> LIFETIME =
        PropertyKey.of("Lifetime", Double.class, 0.0);

    // ── Física / área ─────────────────────────────────────────────────────

    /** Radio de área de efecto en unidades lógicas. */
    public static final PropertyKey<Double> RADIUS =
        PropertyKey.of("Radius", Double.class, 0.0);

    // ── Propiedades térmicas — HRFC-015 ───────────────────────────────────
    //
    // Estas claves representan el estado térmico y las propiedades de
    // transferencia de calor de un objeto. No son fenómenos de gameplay:
    // son valores físicos puros que los módulos de simulación procesan.
    //
    // Un objeto con TEMPERATURE = 1500 no está "en llamas".
    // Está a 1500 unidades de calor. Si su material es susceptible de
    // combustión y la energía térmica supera el umbral de sus propiedades
    // de material, el InteractionRegistry evaluará las consecuencias.

    /**
     * Temperatura actual del objeto.
     * Unidad: grados arbitrarios del universo de Infinity Hell.
     * Valores negativos = frío; positivos = calor; 0 = temperatura ambiente.
     */
    public static final PropertyKey<Double> TEMPERATURE =
        PropertyKey.of("Temperature", Double.class, 0.0);

    /**
     * Conductividad térmica del material.
     * Rango [0, 1]: 0 = aislante perfecto, 1 = conductor perfecto.
     */
    public static final PropertyKey<Double> THERMAL_CONDUCTIVITY =
        PropertyKey.of("ThermalConductivity", Double.class, 0.1);

    /**
     * Capacidad calorífica del material.
     * Energía necesaria para cambiar la temperatura del objeto 1 unidad.
     */
    public static final PropertyKey<Double> HEAT_CAPACITY =
        PropertyKey.of("HeatCapacity", Double.class, 1.0);

    // ── Propiedades eléctricas — HRFC-015 ─────────────────────────────────

    /**
     * Carga eléctrica actual del objeto.
     * Positiva = exceso de carga; negativa = déficit; 0 = neutro.
     */
    public static final PropertyKey<Double> ELECTRICAL_CHARGE =
        PropertyKey.of("ElectricalCharge", Double.class, 0.0);

    /**
     * Resistencia eléctrica del material.
     * Rango [0, 1]: 0 = conductor perfecto, 1 = aislante perfecto.
     */
    public static final PropertyKey<Double> ELECTRICAL_RESISTANCE =
        PropertyKey.of("ElectricalResistance", Double.class, 0.5);

    // ── Propiedades fluídicas / ambientales — HRFC-015 ────────────────────

    /**
     * Humedad actual del objeto o del área local.
     * Rango [0, 1]: 0 = completamente seco, 1 = saturado de agua.
     */
    public static final PropertyKey<Double> HUMIDITY =
        PropertyKey.of("Humidity", Double.class, 0.0);

    /**
     * Presión local sobre o alrededor del objeto.
     * Unidad: unidades arbitrarias de presión del universo del juego.
     */
    public static final PropertyKey<Double> PRESSURE =
        PropertyKey.of("Pressure", Double.class, 0.0);
}
