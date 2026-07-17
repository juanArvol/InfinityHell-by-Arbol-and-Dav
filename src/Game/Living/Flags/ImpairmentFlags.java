package Game.Living.Flags;

/**
 * Estados que limitan las capacidades de una entidad viva.
 *
 * ── HRFC-007 — Nueva categoría del Living Entity Core ────────────────────
 * ImpairmentFlags agrupa estados de incapacitación aplicados externamente
 * por StatusEffects, habilidades o trampas del entorno.
 *
 * ── Responsabilidad única ─────────────────────────────────────────────────
 * ImpairmentFlags NO implementa ninguna lógica.
 * La lógica vive en el StatusEffect correspondiente (FreezeEffect, StunEffect…).
 * ImpairmentFlags existe únicamente como representación rápida del estado:
 *
 *   if (entity.getFlags().impairments().isStunned())
 *
 * sin necesidad de iterar sobre todos los StatusEffects activos.
 *
 * ── Flujo correcto ────────────────────────────────────────────────────────
 *   StatusEffect
 *       ↓ cada frame
 *   actualiza RuntimeStats (StatModifiers)
 *       ↓
 *   actualiza ImpairmentFlags (setStunned, setFrozen…)
 *       ↓
 *   expira → revierte cambios
 *
 * ── Quién lee ImpairmentFlags ────────────────────────────────────────────
 *   EntityFlags.isAbleToMove()   — stunned || frozen || sleeping || rooted
 *   EntityFlags.isAbleToAttack() — stunned || frozen || sleeping || disarmed || silenced
 *   EntityFlags.isAbleToCast()   — silenced || confused
 *   AI                           — adapta su comportamiento a la entidad incapacitada
 *   AnimationSystem              — reproduce animación de estado (congelado, aturdido)
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   stunned   — aturdido: no puede moverse ni atacar.
 *   rooted    — inmovilizado: no puede moverse, pero sí atacar.
 *   frozen    — congelado: no puede moverse ni atacar.
 *   silenced  — silenciado: no puede usar habilidades mágicas.
 *   confused  — movimiento y ataques invertidos o aleatorios.
 *   sleeping  — dormido: inactivo hasta recibir daño o estímulo.
 *   feared    — huye sin control en dirección opuesta al origen del miedo.
 *   disarmed  — desarmado: no puede ejecutar ataques físicos.
 */
public class ImpairmentFlags {

    private boolean stunned  = false;
    private boolean rooted   = false;
    private boolean frozen   = false;
    private boolean silenced = false;
    private boolean confused = false;
    private boolean sleeping = false;
    private boolean feared   = false;
    private boolean disarmed = false;

    public boolean isStunned()               { return stunned; }
    public ImpairmentFlags setStunned(boolean v)  { stunned = v; return this; }

    public boolean isRooted()                { return rooted; }
    public ImpairmentFlags setRooted(boolean v)   { rooted = v; return this; }

    public boolean isFrozen()                { return frozen; }
    public ImpairmentFlags setFrozen(boolean v)   { frozen = v; return this; }

    public boolean isSilenced()              { return silenced; }
    public ImpairmentFlags setSilenced(boolean v) { silenced = v; return this; }

    public boolean isConfused()              { return confused; }
    public ImpairmentFlags setConfused(boolean v) { confused = v; return this; }

    public boolean isSleeping()              { return sleeping; }
    public ImpairmentFlags setSleeping(boolean v) { sleeping = v; return this; }

    public boolean isFeared()                { return feared; }
    public ImpairmentFlags setFeared(boolean v)   { feared = v; return this; }

    public boolean isDisarmed()              { return disarmed; }
    public ImpairmentFlags setDisarmed(boolean v) { disarmed = v; return this; }

    // ── Consultas compuestas ──────────────────────────────────────────────

    /** True si algún estado activo impide el movimiento. */
    public boolean isMovementInhibited() {
        return stunned || frozen || sleeping || rooted;
    }

    /** True si algún estado activo impide ataques físicos. */
    public boolean isAttackInhibited() {
        return stunned || frozen || sleeping || disarmed;
    }

    /** True si algún estado activo impide el uso de habilidades mágicas. */
    public boolean isCastInhibited() {
        return silenced || confused;
    }
}
