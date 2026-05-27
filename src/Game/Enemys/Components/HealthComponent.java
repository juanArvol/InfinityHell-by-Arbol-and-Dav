package Game.Enemys.Components;

/**
 * Componente de salud — gestiona vida, escudo y eventos de daño/muerte.
 *
 * MEJORAS vs. original:
 *   1. Healing: heal(amount) restaura vida sin superar el máximo.
 *   2. Shield: absorbe daño antes de la vida. Se puede recargar.
 *   3. Hooks: onDamage(), onDeath(), onHeal() — override en subclase.
 *   4. Muerte con gracia: isDead() es true cuando current <= 0,
 *      pero onDeath() solo se dispara una vez (flag deathFired).
 *   5. Porcentaje de vida: getHealthPercent() para UI y comportamientos
 *      que reaccionan a vida baja (flee cuando HP < 20%).
 *
 * Retro-compatible: damage(int) e isDead() funcionan igual que antes.
 */
public class HealthComponent {

    private int current;
    private final int max;

    private int shield    = 0;
    private int maxShield = 0;

    private boolean deathFired = false;

    public HealthComponent(int max) {
        if (max <= 0) throw new IllegalArgumentException("max HP debe ser > 0");
        this.max     = max;
        this.current = max;
    }

    // ── Daño ─────────────────────────────────────────────────────────────

    /**
     * Aplica daño. Primero consume el escudo, luego la vida.
     * @param amount cantidad bruta de daño (>= 0)
     */
    public void damage(int amount) {
        if (amount <= 0 || isDead()) return;

        int remaining = amount;

        // Escudo absorbe primero
        if (shield > 0) {
            int absorbed = Math.min(shield, remaining);
            shield   -= absorbed;
            remaining -= absorbed;
            onShieldAbsorb(absorbed);
        }

        if (remaining > 0) {
            current -= remaining;
            if (current < 0) current = 0;
            onDamage(remaining);
        }

        if (current <= 0 && !deathFired) {
            deathFired = true;
            onDeath();
        }
    }

    // ── Curación ──────────────────────────────────────────────────────────

    /**
     * Restaura vida sin superar el máximo.
     * @param amount puntos de vida a recuperar
     */
    public void heal(int amount) {
        if (amount <= 0 || isDead()) return;
        int before = current;
        current = Math.min(current + amount, max);
        if (current > before) {
            onHeal(current - before);
        }
    }

    // ── Escudo ────────────────────────────────────────────────────────────

    /**
     * Añade escudo. El escudo se apila hasta maxShield.
     */
    public void addShield(int amount, int newMaxShield) {
        this.maxShield = newMaxShield;
        this.shield    = Math.min(shield + amount, maxShield);
    }

    public void rechargeShield(int amount) {
        shield = Math.min(shield + amount, maxShield);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public boolean isDead()     { return current <= 0; }
    public int getCurrent()     { return current; }
    public int getMax()         { return max; }
    public int getShield()      { return shield; }
    public int getMaxShield()   { return maxShield; }

    /** Porcentaje de vida en [0.0, 1.0]. Útil para UI y AI reactiva. */
    public double getHealthPercent() {
        return (double) current / max;
    }

    public boolean isCritical() { return getHealthPercent() < 0.25; }

    // ── Hooks (override en subclase o con listener externo) ───────────────

    /** Llamado cuando se recibe daño a la vida (después de absorción de escudo). */
    protected void onDamage(int damageDealt) {}

    /** Llamado una sola vez cuando current llega a 0. */
    protected void onDeath() {}

    /** Llamado cuando se cura vida. */
    protected void onHeal(int healed) {}

    /** Llamado cuando el escudo absorbe daño. */
    protected void onShieldAbsorb(int absorbed) {}
}
