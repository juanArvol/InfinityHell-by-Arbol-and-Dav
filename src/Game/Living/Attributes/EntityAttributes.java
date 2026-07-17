package Game.Living.Attributes;

/**
 * Atributos de dominio de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Reemplaza a EnemyAttributes como contenedor genérico reutilizable por
 * Player, Enemy, Boss, NPC, Pet, Summon, Companion, Turret y cualquier
 * entidad viva del engine.
 *
 * EntityAttributes describe QUÉ ES la entidad en el dominio del juego:
 * a qué facción pertenece, qué elemento representa, cuál es su alineación
 * y qué dificultad implica. Ninguno de estos atributos implica posesión de
 * armas ni comportamiento concreto.
 *
 * ── Qué NO contiene ──────────────────────────────────────────────────────
 *   - Velocidad, daño, rangos → EntityStats
 *   - Flags de estado (invincible, stunned) → EntityFlags
 *   - Fuentes de ataque (Natural, Magic) → AttackSources
 *   - Armas o inventario → módulo Weapon (futuro)
 *
 * ── Diseño extensible ────────────────────────────────────────────────────
 * Los enums internos son el único punto de extensión para nuevas facciones,
 * elementos o clases. Añadir un valor NO requiere modificar ninguna entidad.
 *
 * ── Atributos universales vs específicos ─────────────────────────────────
 * Faction, Element, Alignment y EntityClass son universales: cualquier
 * entidad viva puede pertenecer a una facción, tener un elemento, una
 * alineación y una clase narrativa. DifficultyTier es relevante para las
 * entidades que el jugador enfrenta, pero es igualmente genérico.
 */
public class EntityAttributes {

    // ── Enums de dominio ──────────────────────────────────────────────────

    /**
     * Facción de la entidad.
     * Define con quién está aliado y a quién ataca por defecto.
     */
    public enum Faction {
        NEUTRAL,
        PLAYER,
        UNDEAD,
        MONSTER,
        BOSS,
        SUMMONED,
        TURRET,
        NPC,
        COMPANION
    }

    /**
     * Elemento principal de la entidad.
     * Utilizado por el sistema de resistencias y para efectos visuales.
     */
    public enum Element {
        NONE,
        FIRE,
        ICE,
        LIGHTNING,
        DARK,
        LIGHT,
        POISON,
        EARTH,
        WIND
    }

    /**
     * Alineación general de la entidad respecto al jugador.
     */
    public enum Alignment {
        HOSTILE,
        NEUTRAL,
        ALLY
    }

    /**
     * Clase narrativa / de diseño de la entidad.
     * Define su rol dentro del sistema de combate y la progresión.
     */
    public enum EntityClass {
        COMMON,
        ELITE,
        MINIBOSS,
        BOSS,
        SUMMON,
        COMPANION,
        NPC,
        PLAYER,
        DUMMY
    }

    /**
     * Nivel de dificultad que representa esta entidad.
     * Usado por el sistema de escalado, loot y puntuación.
     */
    public enum DifficultyTier {
        TRIVIAL,
        EASY,
        NORMAL,
        HARD,
        EXTREME
    }

    // ── Campos con valores por defecto ────────────────────────────────────

    private Faction        faction        = Faction.NEUTRAL;
    private Element        element        = Element.NONE;
    private Alignment      alignment      = Alignment.NEUTRAL;
    private EntityClass    entityClass    = EntityClass.COMMON;
    private DifficultyTier difficultyTier = DifficultyTier.NORMAL;

    // ── faction ───────────────────────────────────────────────────────────

    public Faction getFaction()                             { return faction; }
    public EntityAttributes setFaction(Faction v)          { faction = v; return this; }

    // ── element ───────────────────────────────────────────────────────────

    public Element getElement()                            { return element; }
    public EntityAttributes setElement(Element v)         { element = v; return this; }

    // ── alignment ─────────────────────────────────────────────────────────

    public Alignment getAlignment()                        { return alignment; }
    public EntityAttributes setAlignment(Alignment v)     { alignment = v; return this; }

    // ── entityClass ───────────────────────────────────────────────────────

    public EntityClass getEntityClass()                        { return entityClass; }
    public EntityAttributes setEntityClass(EntityClass v)     { entityClass = v; return this; }

    // ── difficultyTier ────────────────────────────────────────────────────

    public DifficultyTier getDifficultyTier()                       { return difficultyTier; }
    public EntityAttributes setDifficultyTier(DifficultyTier v)    { difficultyTier = v; return this; }

    // ── Consultas de conveniencia ─────────────────────────────────────────

    /** True si la entidad es hostil al jugador. */
    public boolean isHostile()   { return alignment == Alignment.HOSTILE; }

    /** True si la entidad es aliada del jugador. */
    public boolean isAlly()      { return alignment == Alignment.ALLY; }

    /** True si la entidad es un boss de algún tipo. */
    public boolean isBossClass() { return entityClass == EntityClass.BOSS || entityClass == EntityClass.MINIBOSS; }
}
