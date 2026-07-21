package Game.Engine.Entity.Flags;

/**
 * Estado derivado de fenómenos de daño periódico activos en la entidad.
 *
 * ── HRFC-014 — GAP-11: Derived State, encapsulado por diseño ────────────
 *
 * DamageFlags representa fenómenos observables, no causas concretas.
 * "isBurning" significa "la entidad presenta actualmente el fenómeno de
 * estar ardiendo", independientemente de si lo causó BurningEffect,
 * HellFireEffect, LavaEffect, NapalmEffect, etc.
 *
 * ── Restricción de escritura — impuesta por diseño, no por convención ─────
 *
 * Los setters son package-private. Solo EntityFlags (mismo paquete) puede
 * invocarlos, a través del método EntityFlags.synchronize(StatusEffectComponent).
 *
 * Ningún código externo al paquete Game.Engine.Entity.Flags puede modificar
 * estos valores directamente. La restricción es estructural, no documental.
 *
 * ── Diagrama de sincronización ────────────────────────────────────────────
 *
 *   StatusEffectComponent          ← fuente de verdad
 *           ↓
 *   StatusEffectSystem             ← lee StatusEffectComponent
 *           ↓
 *   entity.getFlags().synchronize() ← único punto de entrada público
 *           ↓
 *   DamageFlags (setters pkg-priv)  ← estado derivado, lectura pública
 *           ↓
 *   Render / Partículas / IA        ← consultas rápidas por fenómeno
 *
 * ── Fenómenos representados ───────────────────────────────────────────────
 *   burning      — quemadura activa (cualquier fuente de fuego)
 *   poisoned     — envenenamiento activo (cualquier fuente de veneno)
 *   bleeding     — sangrado activo (cualquier fuente de sangrado)
 *   electrified  — descarga eléctrica activa
 *   corroded     — corrosión activa
 *   cursed       — maldición activa
 *   infected     — infección activa
 */
public final class DamageFlags {

    private boolean burning     = false;
    private boolean poisoned    = false;
    private boolean bleeding    = false;
    private boolean electrified = false;
    private boolean corroded    = false;
    private boolean cursed      = false;
    private boolean infected    = false;

    // ── Consultas públicas ────────────────────────────────────────────────
    // Cualquier sistema puede leer el estado derivado.

    /** True si la entidad presenta el fenómeno de quemadura activa. */
    public boolean isBurning()     { return burning; }

    /** True si la entidad presenta el fenómeno de envenenamiento activo. */
    public boolean isPoisoned()    { return poisoned; }

    /** True si la entidad presenta el fenómeno de sangrado activo. */
    public boolean isBleeding()    { return bleeding; }

    /** True si la entidad presenta el fenómeno de descarga eléctrica activa. */
    public boolean isElectrified() { return electrified; }

    /** True si la entidad presenta el fenómeno de corrosión activa. */
    public boolean isCorroded()    { return corroded; }

    /** True si la entidad presenta el fenómeno de maldición activa. */
    public boolean isCursed()      { return cursed; }

    /** True si la entidad presenta el fenómeno de infección activa. */
    public boolean isInfected()    { return infected; }

    /**
     * True si hay al menos un fenómeno de daño periódico activo.
     * Útil para que sistemas de render omitan procesamiento innecesario.
     */
    public boolean hasAnyDamageOverTime() {
        return burning || poisoned || bleeding || electrified || infected;
    }

    // ── Escritura — package-private ────────────────────────────────────────
    // Solo EntityFlags puede invocar estos métodos (mismo paquete).
    // El punto de entrada externo es EntityFlags.synchronize().

    void setBurning(boolean v)     { burning     = v; }
    void setPoisoned(boolean v)    { poisoned    = v; }
    void setBleeding(boolean v)    { bleeding    = v; }
    void setElectrified(boolean v) { electrified = v; }
    void setCorroded(boolean v)    { corroded    = v; }
    void setCursed(boolean v)      { cursed      = v; }
    void setInfected(boolean v)    { infected    = v; }

    void clearAll() {
        burning = poisoned = bleeding = electrified =
        corroded = cursed = infected = false;
    }
}
