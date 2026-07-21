package Game.Engine.Entity.Flags;

import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.Systems.StatusEffectSystem;

/**
 * Contenedor de flags de estado de cualquier entidad viva.
 *
 * ── Arquitectura de flags ─────────────────────────────────────────────────
 *
 *   EntityFlags
 *       ├── CapabilityFlags   — lo que la entidad ES CAPAZ de hacer (diseño)
 *       ├── StateFlags        — estados internos activos controlados por lógica
 *       ├── ImpairmentFlags   — fenómenos de incapacitación (Derived State)
 *       ├── DamageFlags       — fenómenos de daño periódico (Derived State)
 *       └── UtilityFlags      — estados misceláneos (stealth, mark, channel…)
 *
 * ── Categorías y quién puede escribirlas ─────────────────────────────────
 *
 *   CapabilityFlags → Assemblers y configuración estática.
 *                     Representa el diseño de la entidad (¿puede atacar?).
 *
 *   StateFlags      → Assemblers, Phases, MovementStrategies, Components.
 *                     Estado activo de gameplay (invincible, flying, rage).
 *
 *   ImpairmentFlags → Solo via EntityFlags.synchronize(StatusEffectComponent).
 *                     Estado DERIVADO de fenómenos de incapacitación.
 *                     Setters son package-private — inaccesibles desde fuera.
 *
 *   DamageFlags     → Solo via EntityFlags.synchronize(StatusEffectComponent).
 *                     Estado DERIVADO de fenómenos de daño periódico.
 *                     Setters son package-private — inaccesibles desde fuera.
 *
 *   UtilityFlags    → Assemblers, StatusEffects, sistemas de gameplay.
 *                     Estados misceláneos sin restricción adicional.
 *
 * ── Punto de sincronización de estado derivado (HRFC-014 — GAP-11) ────────
 *
 *   EntityFlags.synchronize(StatusEffectComponent) es el ÚNICO punto de
 *   entrada para proyectar el estado de StatusEffectComponent sobre los
 *   flags derivados (ImpairmentFlags, DamageFlags).
 *
 *   La restricción es estructural: los setters de ImpairmentFlags y
 *   DamageFlags son package-private. Solo EntityFlags (mismo paquete)
 *   puede invocarlos. El código externo no puede escribir esos flags
 *   directamente, solo puede leerlos.
 *
 *   Flujo completo:
 *
 *     StatusEffectComponent          ← fuente de verdad
 *             ↓
 *     StatusEffectSystem             ← lee efectos activos
 *             ↓
 *     entity.getFlags().synchronize() ← único punto de escritura público
 *             ↓
 *     ImpairmentFlags / DamageFlags   ← estado derivado (pkg-private setters)
 *             ↓
 *     EntityFlags.isAbleToMove() etc. ← consultas del Engine
 *
 * ── Declaración de fenómenos en StatusEffects ─────────────────────────────
 *
 *   Para que synchronize() proyecte un fenómeno, el StatusEffect debe
 *   implementar la interfaz marcadora correspondiente:
 *
 *     StatusEffectSystem.HasDamagePhenomenon
 *     StatusEffectSystem.HasImpairmentPhenomenon
 *
 *   Ejemplo:
 *
 *     public class BurningEffect
 *             implements StatusEffectComponent.StatusEffect,
 *                        StatusEffectSystem.HasDamagePhenomenon {
 *
 *         {@literal @}Override
 *         public StatusEffectSystem.DamagePhenomenon getDamagePhenomenon() {
 *             return StatusEffectSystem.DamagePhenomenon.BURNING;
 *         }
 *         // tick(), onExpire()...
 *     }
 */
public class EntityFlags {

    private final CapabilityFlags capabilities = new CapabilityFlags();
    private final StateFlags      states       = new StateFlags();
    private final ImpairmentFlags impairments  = new ImpairmentFlags();
    private final DamageFlags     damage       = new DamageFlags();
    private final UtilityFlags    utility      = new UtilityFlags();

    // ── Acceso a sub-objetos ──────────────────────────────────────────────

    /** Capacidades de diseño: canMove, canAttack, canCast… */
    public CapabilityFlags capabilities() { return capabilities; }

    /** Estados activos de gameplay: invincible, flying, rageMode… */
    public StateFlags states()            { return states; }

    /**
     * Fenómenos de incapacitación derivados: stunned, frozen, silenced…
     * Solo lectura. Se actualiza vía {@link #synchronize(StatusEffectComponent)}.
     */
    public ImpairmentFlags impairments()  { return impairments; }

    /**
     * Fenómenos de daño periódico derivados: burning, poisoned, bleeding…
     * Solo lectura. Se actualiza vía {@link #synchronize(StatusEffectComponent)}.
     */
    public DamageFlags damage()           { return damage; }

    /** Estados misceláneos: stealthed, marked, channeling… */
    public UtilityFlags utility()         { return utility; }

    // ── Sincronización de estado derivado (único punto de escritura) ──────

    /**
     * Proyecta el estado de {@code statusEffectComponent} sobre los flags
     * derivados (ImpairmentFlags, DamageFlags).
     *
     * Este es el ÚNICO método externo autorizado a modificar ImpairmentFlags
     * y DamageFlags. La restricción es estructural: sus setters son
     * package-private y solo este método puede invocarlos.
     *
     * Debe llamarse desde {@link StatusEffectSystem} DESPUÉS de que
     * StatusEffectComponent.update() haya procesado tick() y onExpire()
     * de todos los efectos activos del frame actual.
     *
     * Si {@code statusEffectComponent} es null o está vacío, los flags
     * derivados se resetean a false (sin fenómenos activos).
     *
     * @param sec el StatusEffectComponent de la entidad. Puede ser null.
     */
    public void synchronize(StatusEffectComponent sec) {
        // Resetear todos los fenómenos derivados antes de re-proyectar
        impairments.clearAll();
        damage.clearAll();

        if (sec == null || sec.isEmpty()) return;

        int count = sec.activeCount();
        for (int i = 0; i < count; i++) {
            StatusEffectComponent.StatusEffect effect = sec.getEffectAt(i);
            applyDamagePhenomenon(effect);
            applyImpairmentPhenomenon(effect);
        }
    }

    // ── Aplicación interna de fenómenos (acceso a setters pkg-private) ────

    private void applyDamagePhenomenon(StatusEffectComponent.StatusEffect effect) {
        if (!(effect instanceof StatusEffectSystem.HasDamagePhenomenon dp)) return;
        switch (dp.getDamagePhenomenon()) {
            case BURNING     -> damage.setBurning(true);
            case POISONED    -> damage.setPoisoned(true);
            case BLEEDING    -> damage.setBleeding(true);
            case ELECTRIFIED -> damage.setElectrified(true);
            case CORRODED    -> damage.setCorroded(true);
            case CURSED      -> damage.setCursed(true);
            case INFECTED    -> damage.setInfected(true);
        }
    }

    private void applyImpairmentPhenomenon(StatusEffectComponent.StatusEffect effect) {
        if (!(effect instanceof StatusEffectSystem.HasImpairmentPhenomenon ip)) return;
        switch (ip.getImpairmentPhenomenon()) {
            case STUNNED  -> impairments.setStunned(true);
            case ROOTED   -> impairments.setRooted(true);
            case FROZEN   -> impairments.setFrozen(true);
            case SILENCED -> impairments.setSilenced(true);
            case CONFUSED -> impairments.setConfused(true);
            case SLEEPING -> impairments.setSleeping(true);
            case FEARED   -> impairments.setFeared(true);
            case DISARMED -> impairments.setDisarmed(true);
        }
    }

    // ── Shortcuts — CapabilityFlags ───────────────────────────────────────

    public boolean canMove()                     { return capabilities.canMove(); }
    public EntityFlags setCanMove(boolean v)     { capabilities.setCanMove(v); return this; }

    public boolean canAttack()                   { return capabilities.canAttack(); }
    public EntityFlags setCanAttack(boolean v)   { capabilities.setCanAttack(v); return this; }

    public boolean canRotate()                   { return capabilities.canRotate(); }
    public EntityFlags setCanRotate(boolean v)   { capabilities.setCanRotate(v); return this; }

    public boolean canCast()                     { return capabilities.canCast(); }
    public EntityFlags setCanCast(boolean v)     { capabilities.setCanCast(v); return this; }

    public boolean canInteract()                 { return capabilities.canInteract(); }
    public EntityFlags setCanInteract(boolean v) { capabilities.setCanInteract(v); return this; }

    // ── Shortcuts — StateFlags ────────────────────────────────────────────
    // StateFlags son estado activo de gameplay, sus setters son públicos.

    public boolean isInvincible()                { return states.isInvincible(); }
    public EntityFlags setInvincible(boolean v)  { states.setInvincible(v); return this; }

    public boolean isFlying()                    { return states.isFlying(); }
    public EntityFlags setFlying(boolean v)      { states.setFlying(v); return this; }

    public boolean isInvisible()                 { return states.isInvisible(); }
    public EntityFlags setInvisible(boolean v)   { states.setInvisible(v); return this; }

    public boolean isRageMode()                  { return states.isRageMode(); }
    public EntityFlags setRageMode(boolean v)    { states.setRageMode(v); return this; }

    // ── Shortcuts — ImpairmentFlags (solo lectura) ────────────────────────
    // No hay setters públicos. Solo synchronize() puede modificar estos valores.

    public boolean isStunned()  { return impairments.isStunned(); }
    public boolean isFrozen()   { return impairments.isFrozen(); }
    public boolean isSleeping() { return impairments.isSleeping(); }
    public boolean isConfused() { return impairments.isConfused(); }
    public boolean isRooted()   { return impairments.isRooted(); }

    // ── Consultas compuestas (capability + impairment) ────────────────────

    /**
     * True si la entidad puede moverse este frame.
     * Combina la capacidad de diseño con los fenómenos de incapacitación.
     */
    public boolean isAbleToMove() {
        return capabilities.canMove() && !impairments.isMovementInhibited();
    }

    /**
     * True si la entidad puede atacar este frame.
     * Combina la capacidad de diseño con los fenómenos de incapacitación.
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
