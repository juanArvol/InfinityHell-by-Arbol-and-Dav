package Game.Engine.Entity.Flags;

/**
 * Estados persistentes que producen daño o alteraciones periódicas.
 *
 * ── HRFC-007 — Nueva categoría del Living Entity Core ────────────────────
 * DamageFlags agrupa estados de daño continuo aplicados por StatusEffects.
 *
 * ── Responsabilidad única ─────────────────────────────────────────────────
 * DamageFlags NO implementa la lógica de daño por tick.
 * La lógica vive en el StatusEffect correspondiente (BurnEffect, PoisonEffect…).
 * Los flags existen únicamente como representación rápida:
 *
 *   if (entity.getFlags().damage().isBurning())   → mostrar partículas de fuego
 *   if (entity.getFlags().damage().isPoisoned())  → tint verde en el renderer
 *
 * ── Flujo correcto ────────────────────────────────────────────────────────
 *   StatusEffect aplica daño cada tick → actualiza DamageFlags → expira → limpia flags
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   burning      — recibe daño por quemadura periódica.
 *   poisoned     — recibe daño por veneno periódico.
 *   bleeding     — recibe daño por sangrado continuo.
 *   electrified  — recibe daño eléctrico y puede transferirlo a adyacentes.
 *   corroded     — reducción de defensa por corrosión activa.
 *   cursed       — bajo efecto de maldición activa.
 *   infected     — bajo efecto de infección activa.
 */
public class DamageFlags {

    private boolean burning     = false;
    private boolean poisoned    = false;
    private boolean bleeding    = false;
    private boolean electrified = false;
    private boolean corroded    = false;
    private boolean cursed      = false;
    private boolean infected    = false;

    public boolean isBurning()                   { return burning; }
    public DamageFlags setBurning(boolean v)     { burning = v; return this; }

    public boolean isPoisoned()                  { return poisoned; }
    public DamageFlags setPoisoned(boolean v)    { poisoned = v; return this; }

    public boolean isBleeding()                  { return bleeding; }
    public DamageFlags setBleeding(boolean v)    { bleeding = v; return this; }

    public boolean isElectrified()                   { return electrified; }
    public DamageFlags setElectrified(boolean v)     { electrified = v; return this; }

    public boolean isCorroded()                  { return corroded; }
    public DamageFlags setCorroded(boolean v)    { corroded = v; return this; }

    public boolean isCursed()                    { return cursed; }
    public DamageFlags setCursed(boolean v)      { cursed = v; return this; }

    public boolean isInfected()                  { return infected; }
    public DamageFlags setInfected(boolean v)    { infected = v; return this; }

    /** True si la entidad tiene al menos un efecto de daño periódico activo. */
    public boolean hasAnyDamageOverTime() {
        return burning || poisoned || bleeding || electrified || infected;
    }
}
