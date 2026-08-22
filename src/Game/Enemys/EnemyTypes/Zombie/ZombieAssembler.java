package Game.Enemys.EnemyTypes.Zombie;

import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.Core.Movement.GroundMovement;
import Game.Enemys.EnemyPhysics;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSource;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Stats.EntityStats;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;

/**
 * Ensamblador del Zombie — enemigo terrestre estándar.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * Migrado a los tipos genéricos del Living Entity Core:
 *   EnemyStats       → EntityStats
 *   EnemyAttributes  → EntityAttributes
 *   AttackSource/s   → Game.Living.Combat
 *
 * Fuente de ataque: NATURAL. El Zombie es un muerto viviente que ataca
 * únicamente con su cuerpo.
 */
public final class ZombieAssembler extends EnemyAssembler {

    private static final int    MAX_HEALTH       = 100;
    private static final double DETECTION_RANGE  = 400.0;
    private static final double ATTACK_RANGE     = 50.0;
    private static final double MOVE_SPEED       = 1.0;
    private static final int    DAMAGE           = 10;

    @Override
    protected EnemyDefinition definition() {
        // Física terrestre con todos los parámetros de groundStandard()
        EnemyPhysics physics = new EnemyPhysics(
            580,  // gravity
            80,   // mass
            1.0,  // effectiveArea
            9,    // dragCoefficient
            0.8,  // slide
            190,  // aGround
            300,  // aAir
            900,  // speedMaxGround
            600   // speedMaxAir
        );
        
        return EnemyDefinition.builder()
            .sprite(EnemyAssets.normalHandle)
            .health(MAX_HEALTH)
            .physics(physics)
            .collider(24, 30)
            .animation("idle")
            .animation("walk")
            .lootTable("zombie_drops")
            .build();
    }

    @Override
    protected void configureStats(EntityStats stats) {
        stats.setSpeed(MOVE_SPEED)
             .setDamage(DAMAGE)
             .setVisionRange(DETECTION_RANGE)
             .setAttackRange(ATTACK_RANGE);
    }

    @Override
    protected void configureAttributes(EntityAttributes attributes) {
        attributes.setFaction(EntityAttributes.Faction.UNDEAD)
                  .setEntityClass(EntityAttributes.EntityClass.COMMON);
    }

    @Override
    protected void configureAttackSources(AttackSources sources) {
        sources.add(AttackSource.NATURAL);
    }

    @Override
    protected void configureMovement(Enemy enemy) {
        enemy.getMovementController().setStrategy(new GroundMovement());
        enemy.getAIController().setBehavior(
            new AggressiveBehavior(DETECTION_RANGE, ATTACK_RANGE)
        );
    }

    @Override
    protected void configureVisual(Enemy enemy) {
        enemy.addComponent(new AnimationControllerComponent(EnemyAssets.normalHandle, "idle"));
    }
}
