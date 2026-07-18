package Game.Engine.Entity.Stats;

/**
 * Estadísticas de salud de cualquier entidad viva.
 *
 * ── HRFC-013 — Consolidación Definitiva del Dominio Entity ───────────────
 * Creado para completar la jerarquía de EntityStats:
 *
 *   EntityStats
 *       ├── HealthStats     ← este archivo
 *       ├── CombatStats
 *       ├── MovementStats
 *       ├── PerceptionStats
 *       └── ResistanceStats
 *
 * HealthStats es el propietario oficial de toda la información relacionada
 * con la salud de una entidad. Sigue exactamente la misma filosofía que
 * CombatStats y MovementStats:
 *
 *   ✓ Almacena y describe el estado de salud.
 *   ✗ No implementa lógica de gameplay (damage, heal, kill, revive...).
 *
 * La lógica de gameplay pertenece a HealthComponent, que actúa como
 * puente especializado sobre este modelo.
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   currentHp           — vida actual de la entidad.
 *   maxHp               — vida máxima alcanzable.
 *   shield              — escudo absorbente activo (se consume antes que la vida).
 *   maxShield           — capacidad máxima del escudo.
 *   barrier             — capa adicional de absorción (tipo barrera mágica).
 *   maxBarrier          — capacidad máxima de la barrera.
 *   healthRegen         — puntos de vida regenerados por frame.
 *   healingMultiplier   — multiplicador aplicado a toda curación recibida.
 *   incomingDamageMultiplier — multiplicador aplicado a todo daño recibido (1.0 = normal).
 *
 * ── Uso esperado ──────────────────────────────────────────────────────────
 *   // Assembler — configuración base:
 *   entity.getStats().health().setMaxHp(100).setMaxShield(30);
 *
 *   // HealthComponent — lectura durante gameplay:
 *   int current = entity.getStats().health().getCurrentHp();
 *
 *   // HealthComponent — escritura durante gameplay:
 *   entity.getStats().health().setCurrentHp(newValue);
 *
 * ── RuntimeStats ─────────────────────────────────────────────────────────
 *   Los modificadores de salud (buffs de curación, debuffs de daño) se
 *   aplican mediante StatTarget.HEALTH_* y RuntimeStats, exactamente igual
 *   que COMBAT_DAMAGE o MOVEMENT_SPEED.
 */
public class HealthStats {

    private int    currentHp                = 0;
    private int    maxHp                    = 1;    // mínimo 1 para evitar divisiones por cero
    private int    shield                   = 0;
    private int    maxShield                = 0;
    private int    barrier                  = 0;
    private int    maxBarrier               = 0;
    private double healthRegen              = 0.0;
    private double healingMultiplier        = 1.0;
    private double incomingDamageMultiplier = 1.0;

    // ── currentHp ─────────────────────────────────────────────────────────

    public int getCurrentHp()                       { return currentHp; }

    /**
     * Establece la vida actual, clamped al rango [0, maxHp].
     * Solo para escritura directa desde HealthComponent y Assemblers.
     */
    public HealthStats setCurrentHp(int v)          { currentHp = Math.max(0, Math.min(v, maxHp)); return this; }

    /**
     * Establece la vida actual sin clampear.
     * Usar únicamente desde HealthComponent durante inicialización
     * (equivalente al antiguo HealthComponent.initCurrentHP).
     */
    public HealthStats initCurrentHp(int v)         { currentHp = Math.max(0, Math.min(v, maxHp)); return this; }

    // ── maxHp ──────────────────────────────────────────────────────────────

    public int getMaxHp()                           { return maxHp; }

    public HealthStats setMaxHp(int v) {
        if (v < 1) throw new IllegalArgumentException("maxHp debe ser >= 1");
        maxHp = v;
        // Reajustar currentHp si supera el nuevo máximo
        if (currentHp > maxHp) currentHp = maxHp;
        return this;
    }

    // ── shield ────────────────────────────────────────────────────────────

    public int getShield()                          { return shield; }
    public HealthStats setShield(int v)             { shield = Math.max(0, Math.min(v, maxShield)); return this; }

    public int getMaxShield()                       { return maxShield; }
    public HealthStats setMaxShield(int v)          { maxShield = Math.max(0, v); return this; }

    // ── barrier ───────────────────────────────────────────────────────────

    public int getBarrier()                         { return barrier; }
    public HealthStats setBarrier(int v)            { barrier = Math.max(0, Math.min(v, maxBarrier)); return this; }

    public int getMaxBarrier()                      { return maxBarrier; }
    public HealthStats setMaxBarrier(int v)         { maxBarrier = Math.max(0, v); return this; }

    // ── healthRegen ───────────────────────────────────────────────────────

    public double getHealthRegen()                  { return healthRegen; }
    public HealthStats setHealthRegen(double v)     { healthRegen = Math.max(0.0, v); return this; }

    // ── healingMultiplier ─────────────────────────────────────────────────

    /** Multiplicador para toda curación recibida. 1.0 = normal, 1.5 = +50%, 0.5 = -50%. */
    public double getHealingMultiplier()            { return healingMultiplier; }
    public HealthStats setHealingMultiplier(double v) { healingMultiplier = Math.max(0.0, v); return this; }

    // ── incomingDamageMultiplier ──────────────────────────────────────────

    /** Multiplicador aplicado al daño bruto recibido. 1.0 = normal, 0.5 = mitad, 2.0 = doble. */
    public double getIncomingDamageMultiplier()     { return incomingDamageMultiplier; }
    public HealthStats setIncomingDamageMultiplier(double v) { incomingDamageMultiplier = Math.max(0.0, v); return this; }

    // ── Consultas de conveniencia ─────────────────────────────────────────

    /** True si currentHp == 0. No contiene lógica de gameplay. */
    public boolean isDead()        { return currentHp <= 0; }

    /** Porcentaje de vida restante en [0.0, 1.0]. */
    public double getHealthPercent() { return (double) currentHp / maxHp; }

    /** True si el porcentaje de vida es inferior al 25%. */
    public boolean isCritical()    { return getHealthPercent() < 0.25; }
}
