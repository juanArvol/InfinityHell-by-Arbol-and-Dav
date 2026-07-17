package Game.Living.Stats;

/**
 * Contenedor de estadísticas base de cualquier entidad viva.
 *
 * ── HRFC-007 — Generalización al Living Entity Core ──────────────────────
 * Reemplaza a EnemyStats como contenedor genérico reutilizable por Player,
 * Enemy, Boss, NPC, Pet, Summon, Companion, Turret y cualquier entidad viva.
 *
 * EntityStats es el BaseStats en la arquitectura RPG del engine:
 *
 *   EntityStats (BaseStats — inmutable en combate)
 *       ├── MovementStats   — velocidad, aceleración, salto, dash
 *       ├── CombatStats     — daño, defensa, rango, cooldown, crítico
 *       ├── PerceptionStats — visión, oído, ángulo de detección
 *       └── ResistanceStats — resistencias elementales
 *
 * Los Assemblers configuran EntityStats durante la construcción de la entidad.
 * Una vez iniciado el combate, NO se modifican directamente — se usa
 * RuntimeStats para calcular los valores efectivos aplicando modificadores.
 *
 * ── Separación base / runtime ────────────────────────────────────────────
 *   EntityStats  → valores permanentes. Definidos por el Assembler.
 *   RuntimeStats → valores efectivos. EntityStats + modificadores activos.
 *
 * ── Acceso ────────────────────────────────────────────────────────────────
 *   // En un Assembler (configuración base):
 *   entity.getStats().combat().setDamage(15).setAttackCooldown(60);
 *   entity.getStats().movement().setSpeed(2.4);
 *
 *   // En combate (valor efectivo con modificadores aplicados):
 *   double spd = entity.getRuntimeStats().getMovement().getSpeed();
 *   int    dmg = entity.getRuntimeStats().getCombat().getDamageInt();
 */
public class EntityStats {

    private final MovementStats   movementStats   = new MovementStats();
    private final CombatStats     combatStats     = new CombatStats();
    private final PerceptionStats perceptionStats = new PerceptionStats();
    private final ResistanceStats resistanceStats = new ResistanceStats();

    // ── Acceso a categorías ───────────────────────────────────────────────

    /** Estadísticas de movimiento: velocidad, aceleración, salto, dash. */
    public MovementStats movement()     { return movementStats; }

    /** Estadísticas de combate: daño, defensa, rango, cooldown, crítico. */
    public CombatStats combat()         { return combatStats; }

    /** Estadísticas de percepción: visión, oído, ángulo de detección. */
    public PerceptionStats perception() { return perceptionStats; }

    /** Resistencias elementales: fuego, hielo, electricidad, veneno, maldición. */
    public ResistanceStats resistance() { return resistanceStats; }

    // ── Atajos de conveniencia ────────────────────────────────────────────
    // Permiten que el código de Assembler/fases sea fluido sin tener que
    // encadenar entity.getStats().combat().setDamage() constantemente.

    /** Shortcut → movement().setSpeed(v). */
    public EntityStats setSpeed(double v)           { movementStats.setSpeed(v); return this; }
    public double      getSpeed()                   { return movementStats.getSpeed(); }

    /** Shortcut → combat().setDamage(v). */
    public EntityStats setDamage(double v)          { combatStats.setDamage(v); return this; }
    public EntityStats setDamage(int v)             { combatStats.setDamage(v); return this; }
    public double      getDamage()                  { return combatStats.getDamage(); }
    public int         getDamageInt()               { return combatStats.getDamageInt(); }

    /** Shortcut → combat().setDefense(v). */
    public EntityStats setDefense(double v)         { combatStats.setDefense(v); return this; }
    public double      getDefense()                 { return combatStats.getDefense(); }

    /** Shortcut → combat().setAttackCooldown(v). */
    public EntityStats setAttackCooldown(double v)  { combatStats.setAttackCooldown(v); return this; }
    public EntityStats setAttackCooldown(int v)     { combatStats.setAttackCooldown(v); return this; }
    public double      getAttackCooldown()          { return combatStats.getAttackCooldown(); }
    public int         getAttackCooldownInt()       { return combatStats.getAttackCooldownInt(); }

    /** Shortcut → combat().setAttackRange(v). */
    public EntityStats setAttackRange(double v)     { combatStats.setAttackRange(v); return this; }
    public double      getAttackRange()             { return combatStats.getAttackRange(); }

    /** Shortcut → perception().setVisionRange(v). */
    public EntityStats setVisionRange(double v)     { perceptionStats.setVisionRange(v); return this; }
    public double      getVisionRange()             { return perceptionStats.getVisionRange(); }

    /** Shortcut → combat().setTeleportRange(v). */
    public EntityStats setTeleportRange(double v)   { combatStats.setTeleportRange(v); return this; }
    public double      getTeleportRange()           { return combatStats.getTeleportRange(); }
}
