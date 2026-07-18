package Game.Engine.Entity.Components;

import Game.Engine.Component;
import Game.Engine.Entity.Stats.EntityStats;
import Game.Engine.Entity.Stats.HealthStats;

/**
 * Componente de salud — puente especializado hacia HealthStats.
 *
 * ── HRFC-013 — Consolidación Definitiva del Dominio Entity ───────────────
 *
 * CAMBIO ARQUITECTÓNICO:
 *   Antes, HealthComponent era propietario del estado de salud:
 *     private int current, max, shield, maxShield;
 *   Eso lo convertía en una excepción dentro del Engine: el único Component
 *   que almacenaba estado en lugar de delegarlo.
 *
 *   A partir de este HRFC, HealthComponent es un puente especializado.
 *   Todo el estado vive en EntityStats.health() (HealthStats).
 *   HealthComponent únicamente:
 *     - representa que la entidad posee capacidad de vida;
 *     - expone una API cómoda para operar sobre esa capacidad;
 *     - delega completamente sobre HealthStats.
 *
 * ── Modos de construcción ─────────────────────────────────────────────────
 *
 *   Modo enlazado (entidades con EntityStats — Enemy, Player, NPC...):
 *     new HealthComponent(entityStats)
 *     → usa entityStats.health() como fuente de verdad.
 *     → maxHp y currentHp iniciales deben configurarse en EntityStats
 *       antes o después (Assembler llama stats.setMaxHp(n)).
 *
 *   Modo standalone (objetos simples sin EntityStats — cajas, trampas...):
 *     new HealthComponent(maxHp)
 *     → crea un HealthStats interno propio.
 *     → comportamiento idéntico al antiguo HealthComponent(int maxHp).
 *
 * ── API pública — sin cambios funcionales ────────────────────────────────
 *
 *   damage(int)          heal(int)          isDead()
 *   addShield(int,int)   rechargeShield(int)
 *   getCurrent()         getMaxHP()         getShield()         getMaxShield()
 *   getHealthPercent()   isCritical()       initCurrentHP(int)
 *
 *   Métodos nuevos (antes no existían, expuestos porque HealthStats los tiene):
 *   getBarrier()         getMaxBarrier()    addBarrier(int,int)
 *   getHealthRegen()     getHealingMultiplier()
 *
 * ── Delegación completa ───────────────────────────────────────────────────
 *
 *   health.damage(20)          → stats.health().setCurrentHp(hp - 20)
 *   health.heal(30)            → stats.health().setCurrentHp(hp + 30)
 *   health.addShield(50, 50)   → stats.health().setMaxShield(50).setShield(50)
 *   health.isDead()            → stats.health().isDead()
 *
 * ── Hooks de extensión ────────────────────────────────────────────────────
 *
 *   onDamage(int)       onDeath()       onHeal(int)       onShieldAbsorb(int)
 *   Sobreescribibles exactamente igual que antes.
 *   Player sobreescribe onDeath() para activar game-over.
 *   Enemy gestiona su muerte desde update() comprobando isDead().
 *
 * ── Compatibilidad con el gameplay existente ─────────────────────────────
 *
 *   Todo el código de gameplay que usaba:
 *     hp.damage(n)   hp.heal(n)   hp.isDead()   hp.getCurrent()
 *   continúa funcionando sin modificaciones.
 */
public class HealthComponent extends Component {

    /** Fuente de verdad del estado de salud. Nunca null. */
    private final HealthStats stats;

    /** Flag para disparar onDeath() exactamente una vez. */
    private boolean deathFired = false;

    // ── Constructores ──────────────────────────────────────────────────────

    /**
     * Modo enlazado — usa el HealthStats de un EntityStats existente.
     *
     * El Assembler debe haber configurado stats.health().setMaxHp(n) antes
     * o después de crear este componente. Si maxHp todavía es 0 cuando se
     * añade el componente, currentHp quedará a 0 hasta que el Assembler
     * llame stats.setMaxHp(n).
     *
     * Uso canónico desde EnemyAssembler / PlayerAssembler:
     *   addComponent(new HealthComponent(entityStats));
     *   stats.setMaxHp(100);   // inicializa maxHp y currentHp al máximo
     *
     * @param entityStats stats de la entidad. No puede ser null.
     */
    public HealthComponent(EntityStats entityStats) {
        if (entityStats == null) throw new IllegalArgumentException("entityStats no puede ser null");
        this.stats = entityStats.health();
    }

    /**
     * Modo standalone — crea un HealthStats interno propio.
     *
     * Para objetos simples sin EntityStats (cajas destructibles, trampas,
     * objetos de escenario con vida). Comportamiento idéntico al antiguo
     * HealthComponent(int maxHp).
     *
     * @param maxHp vida máxima. Debe ser > 0.
     */
    public HealthComponent(int maxHp) {
        if (maxHp <= 0) throw new IllegalArgumentException("maxHp debe ser > 0");
        this.stats = new HealthStats();
        this.stats.setMaxHp(maxHp).setCurrentHp(maxHp);
    }

    /**
     * Establece la vida actual directamente, sin disparar hooks (onDamage, onDeath).
     * Solo para inicialización — no usar en gameplay.
     *
     * @param value vida inicial, clamped a [0, maxHp].
     */
    public void initCurrentHP(int value) {
        stats.initCurrentHp(value);
    }

    // ── Daño ──────────────────────────────────────────────────────────────

    /**
     * Aplica daño. Primero consume la barrera, luego el escudo, luego la vida.
     *
     * @param amount cantidad bruta de daño (>= 0).
     */
    public void damage(int amount) {
        if (amount <= 0 || isDead()) return;

        int remaining = amount;

        // 1. Barrera (absorbe antes que el escudo)
        if (stats.getBarrier() > 0) {
            int absorbed = Math.min(stats.getBarrier(), remaining);
            stats.setBarrier(stats.getBarrier() - absorbed);
            remaining -= absorbed;
        }

        // 2. Escudo
        if (remaining > 0 && stats.getShield() > 0) {
            int absorbed = Math.min(stats.getShield(), remaining);
            stats.setShield(stats.getShield() - absorbed);
            remaining -= absorbed;
            onShieldAbsorb(absorbed);
        }

        // 3. Vida
        if (remaining > 0) {
            int newHp = Math.max(0, stats.getCurrentHp() - remaining);
            stats.setCurrentHp(newHp);
            onDamage(remaining);
        }

        if (stats.getCurrentHp() <= 0 && !deathFired) {
            deathFired = true;
            onDeath();
        }
    }

    // ── Curación ──────────────────────────────────────────────────────────

    /**
     * Cura a la entidad. Respeta el healingMultiplier de HealthStats.
     *
     * @param amount cantidad base de curación (>= 0).
     */
    public void heal(int amount) {
        if (amount <= 0 || isDead()) return;
        int before = stats.getCurrentHp();
        int effective = (int) Math.round(amount * stats.getHealingMultiplier());
        stats.setCurrentHp(Math.min(stats.getCurrentHp() + effective, stats.getMaxHp()));
        int healed = stats.getCurrentHp() - before;
        if (healed > 0) onHeal(healed);
    }

    // ── Escudo ────────────────────────────────────────────────────────────

    /**
     * Añade escudo, estableciendo un nuevo máximo si es mayor al actual.
     *
     * @param amount     cantidad de escudo a añadir.
     * @param newMaxShield nuevo máximo de escudo.
     */
    public void addShield(int amount, int newMaxShield) {
        stats.setMaxShield(newMaxShield);
        stats.setShield(Math.min(stats.getShield() + amount, stats.getMaxShield()));
    }

    /** Recarga el escudo hasta el máximo actual. */
    public void rechargeShield(int amount) {
        stats.setShield(Math.min(stats.getShield() + amount, stats.getMaxShield()));
    }

    // ── Barrera ───────────────────────────────────────────────────────────

    /**
     * Añade barrera (capa de absorción previa al escudo).
     *
     * @param amount       cantidad de barrera a añadir.
     * @param newMaxBarrier nuevo máximo de barrera.
     */
    public void addBarrier(int amount, int newMaxBarrier) {
        stats.setMaxBarrier(newMaxBarrier);
        stats.setBarrier(Math.min(stats.getBarrier() + amount, stats.getMaxBarrier()));
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    public boolean isDead()           { return stats.isDead(); }
    public int     getCurrent()       { return stats.getCurrentHp(); }
    public int     getMaxHP()         { return stats.getMaxHp(); }
    public int     getShield()        { return stats.getShield(); }
    public int     getMaxShield()     { return stats.getMaxShield(); }
    public int     getBarrier()       { return stats.getBarrier(); }
    public int     getMaxBarrier()    { return stats.getMaxBarrier(); }
    public double  getHealthRegen()   { return stats.getHealthRegen(); }
    public double  getHealingMultiplier() { return stats.getHealingMultiplier(); }

    public double  getHealthPercent() { return stats.getHealthPercent(); }
    public boolean isCritical()       { return stats.isCritical(); }

    /**
     * Expone el HealthStats subyacente para lectura avanzada.
     * Preferir los métodos de conveniencia para operaciones de gameplay normales.
     * No usar para escribir estado directamente — usar damage/heal/addShield.
     */
    public HealthStats getHealthStats() { return stats; }

    // ── Hooks ─────────────────────────────────────────────────────────────

    protected void onDamage(int damageDealt)    {}
    protected void onDeath()                    {}
    protected void onHeal(int healed)           {}
    protected void onShieldAbsorb(int absorbed) {}
}
