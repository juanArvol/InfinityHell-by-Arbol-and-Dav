package Game.Engine.Entity.Stats;

/**
 * Estadísticas de combate de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Movido desde Game.Enemys.Core.Stats a Game.Living.Stats.
 * Describe ÚNICAMENTE las propiedades que cuantifican la capacidad ofensiva
 * y defensiva de una entidad. No contiene movimiento ni percepción.
 *
 * ── Campos ────────────────────────────────────────────────────────────────
 *   damage          — daño base por ataque.
 *   defense         — reducción porcentual de daño recibido (0.0–1.0).
 *   attackRange     — distancia máxima desde la que puede atacar (px).
 *   attackCooldown  — frames de espera entre ataques.
 *   criticalChance  — probabilidad de golpe crítico (0.0–1.0).
 *   teleportRange   — distancia máxima de teletransporte (0 = no puede).
 */
public class CombatStats {

    private double damage         = 0.0;
    private double defense        = 0.0;
    private double attackRange    = 50.0;
    private double attackCooldown = 120.0;
    private double criticalChance = 0.0;
    private double teleportRange  = 0.0;

    public double getDamage()                           { return damage; }
    public int    getDamageInt()                        { return (int) damage; }
    public CombatStats setDamage(double v)              { damage = v; return this; }
    public CombatStats setDamage(int v)                 { damage = v; return this; }

    public double getDefense()                          { return defense; }
    public CombatStats setDefense(double v)             { defense = v; return this; }

    public double getAttackRange()                      { return attackRange; }
    public CombatStats setAttackRange(double v)         { attackRange = v; return this; }

    public double getAttackCooldown()                   { return attackCooldown; }
    public int    getAttackCooldownInt()                { return (int) attackCooldown; }
    public CombatStats setAttackCooldown(double v)      { attackCooldown = v; return this; }
    public CombatStats setAttackCooldown(int v)         { attackCooldown = v; return this; }

    public double getCriticalChance()                   { return criticalChance; }
    public CombatStats setCriticalChance(double v)      { criticalChance = Math.max(0, Math.min(1, v)); return this; }

    public double getTeleportRange()                    { return teleportRange; }
    public CombatStats setTeleportRange(double v)       { teleportRange = v; return this; }
}
