package Game.Gameplay.Core.Capabilities;

/**
 * Catálogo de capacidades fundamentales del núcleo de Infinity Hell.
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * Cada constante aquí representa un tipo de capacidad. No hay lógica,
 * no hay datos — solo la declaración de que dicho comportamiento existe
 * como concepto en el universo del juego.
 *
 * La lógica que hace que una entidad PUEDA HACER algo está en los sistemas
 * que reaccionan a la presencia de la capacidad, no en la capacidad misma.
 *
 * ── CÓMO EXTENDER ────────────────────────────────────────────────────────
 * Nuevos sistemas declaran sus propias capacidades. No es necesario modificar
 * este archivo:
 *
 *   // En un módulo de fusiones:
 *   public final class FusionCapabilities {
 *       public static final GameplayCapability CAN_BE_FUSED =
 *           new GameplayCapability() {};
 *   }
 *
 * ── CAPACIDADES CON DATOS ─────────────────────────────────────────────────
 * Cuando una capacidad necesita transportar datos (no solo existir como flag),
 * se implementa como una clase concreta que implementa GameplayCapability:
 *
 *   // Capacidad de rebotar con un límite configurable:
 *   CapabilityComponent caps = new CapabilityComponent();
 *   caps.add(new BounceCapability(3));    // rebota hasta 3 veces
 *
 *   // El sistema de rebote la extrae tipada:
 *   BounceCapability bounce = caps.get(BounceCapability.class);
 *   if (bounce != null && bounce.hasBouncesLeft()) { ... }
 */
public final class CoreCapabilities {

    private CoreCapabilities() {}

    // ── Movimiento ────────────────────────────────────────────────────────

    /** La entidad puede rebotar contra superficies o entidades. */
    public static final GameplayCapability CAN_BOUNCE = new GameplayCapability() {};

    /** La entidad puede dividirse en múltiples entidades al impactar. */
    public static final GameplayCapability CAN_SPLIT = new GameplayCapability() {};

    // ── Interacciones elemental-físicas ───────────────────────────────────

    /** La entidad puede explotar, generando un área de efecto. */
    public static final GameplayCapability CAN_EXPLODE = new GameplayCapability() {};

    /** La entidad puede congelarse o congelar a otras entidades. */
    public static final GameplayCapability CAN_FREEZE = new GameplayCapability() {};

    /** La entidad puede incendiarse o incendiar a otras entidades. */
    public static final GameplayCapability CAN_IGNITE = new GameplayCapability() {};

    // ── Recepción de modificadores ────────────────────────────────────────

    /**
     * La entidad acepta modificadores de gameplay genéricos.
     * Sistema de buffs, debuffs, potenciadores.
     */
    public static final GameplayCapability CAN_RECEIVE_MODIFIERS = new GameplayCapability() {};

    /**
     * La entidad acepta modificadores de naturaleza mágica/arcana.
     * Permite que hechizos y reliquias operen sobre ella.
     */
    public static final GameplayCapability CAN_RECEIVE_MAGIC = new GameplayCapability() {};

    // ── Interacciones avanzadas ───────────────────────────────────────────

    /**
     * La entidad puede fusionarse con otras entidades del mismo tipo.
     * Mecánica de fusión de criaturas/proyectiles.
     */
    public static final GameplayCapability CAN_BE_FUSED = new GameplayCapability() {};

    /**
     * La entidad puede ser invocada por otra entidad (summoner).
     * Criaturas invocadas, familiares, compañeros de hechizo.
     */
    public static final GameplayCapability CAN_BE_SUMMONED = new GameplayCapability() {};

    /**
     * La entidad puede ser el objetivo de rastreo de proyectiles u otras
     * entidades con comportamiento de persecución (homing).
     */
    public static final GameplayCapability IS_TRACKABLE = new GameplayCapability() {};
}
