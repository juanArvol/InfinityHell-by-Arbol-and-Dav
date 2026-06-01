package Game.Engine.Components;

import Game.Engine.Component;

/**
 * Componente de salud — gestiona vida, escudo y eventos de daño/muerte.
 *
 * ── REFACTOR: MOVIDO DESDE Game.Enemys.Components ────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   HealthComponent vivía en Game.Enemys.Components. Conceptualmente, la
 *   salud no es exclusiva de los enemigos: el Player tiene vida (PlayerStats),
 *   los objetos destruibles necesitarán vida, los Bosses también. Tener
 *   HealthComponent dentro del módulo Enemy lo bloqueaba para uso general
 *   y obligaba a duplicar la lógica (PlayerStats hace lo mismo manualmente).
 *
 * SOLUCIÓN:
 *   Mover HealthComponent a Game.Engine.Components.Gameplay, la ubicación
 *   correcta para componentes de gameplay reutilizables entre entidades.
 *   El paquete Gameplay dentro de Components comunica la intención:
 *   no es infraestructura del engine (física, colisiones), sino gameplay
 *   compartido (vida, efectos, facción).
 *
 * BENEFICIO:
 *   - Player puede usar HealthComponent en lugar de PlayerStats duplicado.
 *   - Objetos destruibles, Bosses, NPCs comparten la misma implementación.
 *   - PlayerStats queda libre para contener solo stats genuinos (velocidad,
 *     fuerza, etc.) sin gestionar lógica de daño.
 *
 * ── API (sin cambios funcionales) ────────────────────────────────────────
 *
 * Retro-compatible: damage(int) e isDead() funcionan igual que antes.
 * Se añaden hooks override-ables para extensión limpia sin subclasificar.
 *
 * USO:
 *   // En el constructor de Enemy, Player, objeto destruible, etc.:
 *   addComponent(new HealthComponent(maxHp));
 *
 *   // Acceso:
 *   HealthComponent hp = getComponent(HealthComponent.class);
 *   hp.damage(10);
 *   hp.heal(5);
 *   hp.isDead();
 */
public class HealthComponent extends Component {

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

        if (shield > 0) {
            int absorbed = Math.min(shield, remaining);
            shield    -= absorbed;
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

    public void heal(int amount) {
        if (amount <= 0 || isDead()) return;
        int before = current;
        current = Math.min(current + amount, max);
        if (current > before) {
            onHeal(current - before);
        }
    }

    // ── Escudo ────────────────────────────────────────────────────────────

    public void addShield(int amount, int newMaxShield) {
        this.maxShield = newMaxShield;
        this.shield    = Math.min(shield + amount, maxShield);
    }

    public void rechargeShield(int amount) {
        shield = Math.min(shield + amount, maxShield);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public boolean isDead()           { return current <= 0; }
    public int     getCurrent()       { return current; }
    public int     getMaxHP()           { return max; }
    public int     getShield()        { return shield; }
    public int     getMaxShield()     { return maxShield; }

    public double getHealthPercent()  { return (double) current / max; }
    public boolean isCritical()       { return getHealthPercent() < 0.25; }

    // ── Hooks ─────────────────────────────────────────────────────────────

    protected void onDamage(int damageDealt)    {}
    protected void onDeath()                    {}
    protected void onHeal(int healed)           {}
    protected void onShieldAbsorb(int absorbed) {}
}
