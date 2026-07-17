package Game.Living.Flags;

/**
 * Contenedor de flags booleanos de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Reemplaza a EnemyFlags como contenedor genérico reutilizable por Player,
 * Enemy, Boss, NPC, Pet, Summon, Companion, Turret y cualquier entidad viva.
 *
 * ── Arquitectura de flags por responsabilidad ─────────────────────────────
 *
 *   EntityFlags
 *       ├── CapabilityFlags  — lo que la entidad ES CAPAZ de hacer (diseño)
 *       ├── StateFlags       — estados internos activos (vuelo, rabia, invencible)
 *       ├── ImpairmentFlags  — estados que LIMITAN capacidades (stun, freeze…)
 *       ├── DamageFlags      — estados de daño periódico (burn, poison, bleed…)
 *       └── UtilityFlags     — estados misceláneos (stealth, mark, channel…)
 *
 * ── Regla de consulta compuesta ──────────────────────────────────────────
 *
 *   boolean moverEsteFrame =
 *       flags.capabilities().canMove()          // capacidad de diseño
 *       && !flags.impairments().isMovementInhibited(); // sin incapacitaciones
 *
 * ── Shortcuts de conveniencia ────────────────────────────────────────────
 * EntityFlags expone los shortcuts más usados directamente para mantener
 * la API fluida en Assemblers, fases y componentes:
 *
 *   entity.getFlags().setInvincible(true)
 *   entity.getFlags().isAbleToMove()
 *   entity.getFlags().canAttack()
 */
public class EntityFlags {

    private final CapabilityFlags capabilities = new CapabilityFlags();
    private final StateFlags      states       = new StateFlags();
    private final ImpairmentFlags impairments  = new ImpairmentFlags();
    private final DamageFlags     damage       = new DamageFlags();
    private final UtilityFlags    utility      = new UtilityFlags();

    // ── Acceso a sub-objetos ──────────────────────────────────────────────

    /** Capacidades de diseño de la entidad (canMove, canAttack, canCast…). */
    public CapabilityFlags capabilities() { return capabilities; }

    /** Estados internos activos (invincible, flying, rageMode…). */
    public StateFlags states()            { return states; }

    /** Estados que limitan capacidades (stunned, frozen, silenced…). */
    public ImpairmentFlags impairments()  { return impairments; }

    /** Estados de daño periódico activos (burning, poisoned, bleeding…). */
    public DamageFlags damage()           { return damage; }

    /** Estados misceláneos (stealthed, marked, channeling…). */
    public UtilityFlags utility()         { return utility; }

    // ── Shortcuts — CapabilityFlags ───────────────────────────────────────

    public boolean canMove()                        { return capabilities.canMove(); }
    public EntityFlags setCanMove(boolean v)        { capabilities.setCanMove(v); return this; }

    public boolean canAttack()                      { return capabilities.canAttack(); }
    public EntityFlags setCanAttack(boolean v)      { capabilities.setCanAttack(v); return this; }

    public boolean canRotate()                      { return capabilities.canRotate(); }
    public EntityFlags setCanRotate(boolean v)      { capabilities.setCanRotate(v); return this; }

    public boolean canCast()                        { return capabilities.canCast(); }
    public EntityFlags setCanCast(boolean v)        { capabilities.setCanCast(v); return this; }

    public boolean canInteract()                    { return capabilities.canInteract(); }
    public EntityFlags setCanInteract(boolean v)    { capabilities.setCanInteract(v); return this; }

    // ── Shortcuts — StateFlags ────────────────────────────────────────────

    public boolean isInvincible()                   { return states.isInvincible(); }
    public EntityFlags setInvincible(boolean v)     { states.setInvincible(v); return this; }

    public boolean isFlying()                       { return states.isFlying(); }
    public EntityFlags setFlying(boolean v)         { states.setFlying(v); return this; }

    public boolean isInvisible()                    { return states.isInvisible(); }
    public EntityFlags setInvisible(boolean v)      { states.setInvisible(v); return this; }

    public boolean isRageMode()                     { return states.isRageMode(); }
    public EntityFlags setRageMode(boolean v)       { states.setRageMode(v); return this; }

    // ── Shortcuts — ImpairmentFlags ───────────────────────────────────────

    public boolean isStunned()                      { return impairments.isStunned(); }
    public EntityFlags setStunned(boolean v)        { impairments.setStunned(v); return this; }

    public boolean isFrozen()                       { return impairments.isFrozen(); }
    public EntityFlags setFrozen(boolean v)         { impairments.setFrozen(v); return this; }

    public boolean isSleeping()                     { return impairments.isSleeping(); }
    public EntityFlags setSleeping(boolean v)       { impairments.setSleeping(v); return this; }

    public boolean isConfused()                     { return impairments.isConfused(); }
    public EntityFlags setConfused(boolean v)       { impairments.setConfused(v); return this; }

    // ── Shortcuts — DamageFlags ───────────────────────────────────────────

    public boolean isBurning()                      { return damage.isBurning(); }
    public EntityFlags setBurning(boolean v)        { damage.setBurning(v); return this; }

    public boolean isPoisoned()                     { return damage.isPoisoned(); }
    public EntityFlags setPoisoned(boolean v)       { damage.setPoisoned(v); return this; }

    public boolean isBleeding()                     { return damage.isBleeding(); }
    public EntityFlags setBleeding(boolean v)       { damage.setBleeding(v); return this; }

    // ── Consultas compuestas (capability + impairment) ────────────────────

    /**
     * True si la entidad puede moverse este frame.
     * Combina la capacidad de diseño con los estados de incapacitación activos.
     */
    public boolean isAbleToMove() {
        return capabilities.canMove() && !impairments.isMovementInhibited();
    }

    /**
     * True si la entidad puede atacar este frame.
     * Combina la capacidad de diseño con los estados de incapacitación activos.
     */
    public boolean isAbleToAttack() {
        return capabilities.canAttack() && !impairments.isAttackInhibited();
    }

    /**
     * True si la entidad puede usar habilidades mágicas este frame.
     */
    public boolean isAbleToCast() {
        return capabilities.canCast() && !impairments.isCastInhibited();
    }
}
