package Game.Engine.Entity.Flags;

/**
 * Capacidades permanentes o semipermanentes de una entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Flags a Game.Living.Flags.
 * Describe lo que la entidad PUEDE HACER en condiciones normales.
 * Son capacidades de diseño, no estados transitorios de combate.
 *
 * ── Diferencia con StateFlags / ImpairmentFlags ───────────────────────────
 *   CapabilityFlags — definidos por el diseño de la entidad.
 *                     Ejemplo: una Turret nunca puede moverse → canMove=false.
 *                     Un NPC sin magia → canCast=false.
 *
 *   ImpairmentFlags — resultado de efectos de estado transitorios.
 *                     Ejemplo: stunned=true inhibe el movimiento este frame.
 *
 * ── Regla de uso ─────────────────────────────────────────────────────────
 *   boolean moverEsteFrame =
 *       capabilities.canMove()           // capacidad de diseño
 *       && !impairments.isStunned()      // sin estados de incapacitación
 *       && !impairments.isFrozen();
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   canMove          — puede desplazarse en el mundo.
 *   canAttack        — puede ejecutar ataques.
 *   canRotate        — puede cambiar de orientación/dirección.
 *   canCast          — puede usar habilidades mágicas o especiales.
 *   canInteract      — puede interactuar con objetos del mundo.
 *   canReceiveDamage — puede recibir daño (false = inmune por diseño).
 *   canBeTargeted    — puede ser seleccionado como objetivo por la IA.
 */
public class CapabilityFlags {

    private boolean canMove          = true;
    private boolean canAttack        = true;
    private boolean canRotate        = true;
    private boolean canCast          = false;
    private boolean canInteract      = false;
    private boolean canReceiveDamage = true;
    private boolean canBeTargeted    = true;

    public boolean canMove()                             { return canMove; }
    public CapabilityFlags setCanMove(boolean v)         { canMove = v; return this; }

    public boolean canAttack()                           { return canAttack; }
    public CapabilityFlags setCanAttack(boolean v)       { canAttack = v; return this; }

    public boolean canRotate()                           { return canRotate; }
    public CapabilityFlags setCanRotate(boolean v)       { canRotate = v; return this; }

    public boolean canCast()                             { return canCast; }
    public CapabilityFlags setCanCast(boolean v)         { canCast = v; return this; }

    public boolean canInteract()                         { return canInteract; }
    public CapabilityFlags setCanInteract(boolean v)     { canInteract = v; return this; }

    public boolean canReceiveDamage()                            { return canReceiveDamage; }
    public CapabilityFlags setCanReceiveDamage(boolean v)        { canReceiveDamage = v; return this; }

    public boolean canBeTargeted()                           { return canBeTargeted; }
    public CapabilityFlags setCanBeTargeted(boolean v)       { canBeTargeted = v; return this; }
}
