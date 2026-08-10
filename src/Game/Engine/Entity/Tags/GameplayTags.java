package Game.Engine.Entity.Tags;

/**
 * Catálogo de tags de entidad del núcleo de Infinity Hell.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Define el vocabulario de clasificación base del universo del juego.
 * Solo incluye conceptos fundamentales que son transversales a todos los
 * sistemas futuros.
 *
 * Nuevos sistemas (magia, reliquias, criaturas invocadas) añaden sus propios
 * catálogos de tags sin modificar este archivo. La jerarquía los integra
 * automáticamente: un "Entity.Spell.Fire" es automáticamente una "Entity".
 *
 * ── ÁRBOL DE TAGS DEL NÚCLEO ─────────────────────────────────────────────
 *
 *   ENTITY
 *     ├── ORGANIC
 *     │     ├── CREATURE
 *     │     └── COMPANION
 *     ├── MECHANICAL
 *     ├── PROJECTILE
 *     ├── ENVIRONMENTAL
 *     └── BOSS
 *
 *   ELEMENT
 *     ├── FIRE
 *     ├── ICE
 *     └── ELECTRIC
 *
 *   FACTION
 *     ├── PLAYER
 *     └── ENEMY
 *
 * ── CONVENCIÓN ────────────────────────────────────────────────────────────
 * Las constantes son public static final. Son compartidas como singletons:
 * comparar por referencia o por id() es equivalente.
 *
 * NO hay código de juego en este archivo. Solo declaraciones de tags.
 */
public final class GameplayTags {

    private GameplayTags() {}

    // ── ENTITY — Todo lo que existe como entidad interactuable ────────────

    /** Raíz de todas las entidades. */
    public static final GameplayTagNode ENTITY       = GameplayTagNode.root("Entity");

    /** Entidades de naturaleza orgánica/biológica. */
    public static final GameplayTagNode ORGANIC      = GameplayTagNode.of("Entity.Organic",      ENTITY);

    /** Criatura orgánica que combate. Enemigos y similares. */
    public static final GameplayTagNode CREATURE     = GameplayTagNode.of("Entity.Organic.Creature",   ORGANIC);

    /** Compañero aliado del jugador. */
    public static final GameplayTagNode COMPANION    = GameplayTagNode.of("Entity.Organic.Companion",  ORGANIC);

    /** Entidades de naturaleza mecánica/construida. */
    public static final GameplayTagNode MECHANICAL   = GameplayTagNode.of("Entity.Mechanical",   ENTITY);

    /** Proyectil — cualquier cosa que viaja y puede impactar. */
    public static final GameplayTagNode PROJECTILE   = GameplayTagNode.of("Entity.Projectile",   ENTITY);

    /** Elemento ambiental del mundo (trampas, zonas, obstáculos con efecto). */
    public static final GameplayTagNode ENVIRONMENTAL = GameplayTagNode.of("Entity.Environmental", ENTITY);

    /** Entidad Boss — variante de CREATURE con reglas especiales. */
    public static final GameplayTagNode BOSS         = GameplayTagNode.of("Entity.Boss",         ENTITY);

    // ── ELEMENT — Afinidades elementales ─────────────────────────────────

    /** Raíz de todos los elementos. */
    public static final GameplayTagNode ELEMENT      = GameplayTagNode.root("Element");

    /** Afinidad de fuego. */
    public static final GameplayTagNode FIRE         = GameplayTagNode.of("Element.Fire",        ELEMENT);

    /** Afinidad de hielo. */
    public static final GameplayTagNode ICE          = GameplayTagNode.of("Element.Ice",         ELEMENT);

    /** Afinidad eléctrica. */
    public static final GameplayTagNode ELECTRIC     = GameplayTagNode.of("Element.Electric",    ELEMENT);

    // ── FACTION — Bando en el conflicto ──────────────────────────────────

    /** Raíz de todas las facciones. */
    public static final GameplayTagNode FACTION      = GameplayTagNode.root("Faction");

    /** Bando del jugador. */
    public static final GameplayTagNode PLAYER_FACTION  = GameplayTagNode.of("Faction.Player",   FACTION);

    /** Bando enemigo. */
    public static final GameplayTagNode ENEMY_FACTION   = GameplayTagNode.of("Faction.Enemy",    FACTION);

    // ── STATUS — Estados alterados ────────────────────────────────────────

    /** Raíz de todos los estados alterados. */
    public static final GameplayTagNode STATUS       = GameplayTagNode.root("Status");

    /** Estado: quemando. */
    public static final GameplayTagNode STATUS_BURNING  = GameplayTagNode.of("Status.Burning",   STATUS);

    /** Estado: congelado. */
    public static final GameplayTagNode STATUS_FROZEN   = GameplayTagNode.of("Status.Frozen",    STATUS);

    /** Estado: aturdido. */
    public static final GameplayTagNode STATUS_STUNNED  = GameplayTagNode.of("Status.Stunned",   STATUS);

    /** Estado: envenenado. */
    public static final GameplayTagNode STATUS_POISONED = GameplayTagNode.of("Status.Poisoned",  STATUS);
}
