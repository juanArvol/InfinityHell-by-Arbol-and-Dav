package Game.Gameplay.Core.Properties;

/**
 * Catálogo de claves de propiedad del núcleo de Infinity Hell.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Define las propiedades modificables fundamentales que son transversales
 * a todos los sistemas del juego. Son los "parámetros del universo" sobre
 * los cuales los modificadores operan.
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
 * PropertyMap.get() cuando la clave no ha sido registrada explícitamente.
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

    // ── Física avanzada ───────────────────────────────────────────────────

    /** Radio de área de efecto en unidades lógicas. */
    public static final PropertyKey<Double> RADIUS =
        PropertyKey.of("Radius", Double.class, 0.0);

    /** Temperatura — eje de efectos de calor/frío (negativo = frío, positivo = calor). */
    public static final PropertyKey<Double> TEMPERATURE =
        PropertyKey.of("Temperature", Double.class, 0.0);
}
