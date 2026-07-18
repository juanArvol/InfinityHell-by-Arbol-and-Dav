package Game.Enemys.EnemyTypes.Hybrid;

import Game.Enemys.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Movement.GroundMovement;
import Game.Enemys.EnemyPhysicsConfig;
import Game.Engine.Components.Visuals.AnimationControllerComponent;
import Game.Living.Attributes.EntityAttributes;
import Game.Living.Combat.AttackSource;
import Game.Living.Combat.AttackSources;
import Game.Living.Stats.EntityStats;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;

/**
 * Ensamblador del enemigo híbrido tierra/vuelo.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * Migrado a los tipos genéricos del Living Entity Core:
 *   EnemyStats       → EntityStats
 *   EnemyAttributes  → EntityAttributes
 *   AttackSource/s   → Game.Living.Combat
 *
 * Fuente de ataque: NATURAL (criatura orgánica que ataca cuerpo a cuerpo
 * en ambos modos de movimiento).
 */
public final class HybridAssembler extends EnemyAssembler {

    private static final int    MAX_HEALTH      = 120;
    private static final double DETECTION_RANGE = 400.0;
    private static final double ATTACK_RANGE    = 50.0;
    private static final double MOVE_SPEED      = 1.2;
    private static final int    DAMAGE          = 15;

    @Override
    protected EnemyDefinition definition() {
        return EnemyDefinition.builder()
            .sprite(EnemyAssets.normalHandle)
            .health(MAX_HEALTH)
            .physics(EnemyPhysicsConfig.groundStandard())
            .collider(24, 30)
            .animation("idle")
            .animation("fly")
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
        attributes.setFaction(EntityAttributes.Faction.MONSTER)
                  .setEntityClass(EntityAttributes.EntityClass.ELITE);
    }

    @Override
    protected void configureAttackSources(AttackSources sources) {
        sources.add(AttackSource.NATURAL);
    }

    @Override
    protected void configureMovement(Enemy enemy) {
        // Comienza en modo terrestre.
        // Una fase o EnemyComponent puede transicionar a FlyingMovement en runtime.
        enemy.getMovementController().setStrategy(new GroundMovement());
        enemy.getAIController().setBehavior(
            new AggressiveBehavior(DETECTION_RANGE, ATTACK_RANGE, MOVE_SPEED)
        );
    }

    @Override
    protected void configureVisual(Enemy enemy) {
        enemy.addComponent(new AnimationControllerComponent(EnemyAssets.normalHandle, "idle"));
    }
}
