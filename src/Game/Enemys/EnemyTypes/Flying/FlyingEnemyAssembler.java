package Game.Enemys.EnemyTypes.Flying;

import Game.Enemys.AI.Behaviors.FlyingBehavior;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Movement.FlyingMovement;
import Game.Enemys.EnemyPhysicsConfig;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSource;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Components.Visuals.ShadowComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;

/**
 * Ensamblador del enemigo volador.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * Migrado a los tipos genéricos del Living Entity Core:
 *   EnemyStats       → EntityStats
 *   EnemyFlags       → EntityFlags
 *   EnemyAttributes  → EntityAttributes
 *   AttackSource/s   → Game.Living.Combat
 *
 * Fuente de ataque: NATURAL (embestida aérea, picoteo, garras).
 */
public final class FlyingEnemyAssembler extends EnemyAssembler {

    private static final int    MAX_HEALTH     = 80;
    private static final double MAX_SPEED      = 3.0;
    private static final double STEERING_FORCE = 0.15;

    @Override
    protected EnemyDefinition definition() {
        return EnemyDefinition.builder()
            .sprite(EnemyAssets.flyingHandle)
            .health(MAX_HEALTH)
            .physics(EnemyPhysicsConfig.flyingStandard())
            .collider(24, 30)
            .shadow(18, 7)
            .animation("idle")
            .build();
    }

    @Override
    protected void configureStats(EntityStats stats) {
        stats.setSpeed(MAX_SPEED);
    }

    @Override
    protected void configureFlags(EntityFlags flags) {
        flags.setFlying(true);
    }

    @Override
    protected void configureAttributes(EntityAttributes attributes) {
        attributes.setFaction(EntityAttributes.Faction.MONSTER)
                  .setEntityClass(EntityAttributes.EntityClass.COMMON);
    }

    @Override
    protected void configureAttackSources(AttackSources sources) {
        sources.add(AttackSource.NATURAL);
    }

    @Override
    protected void configureMovement(Enemy enemy) {
        enemy.getMovementController().setStrategy(new FlyingMovement());
        enemy.getAIController().setBehavior(
            new FlyingBehavior(MAX_SPEED, STEERING_FORCE)
        );
    }

    @Override
    protected void configureVisual(Enemy enemy) {
        enemy.addComponent(new ShadowComponent(18, 7));
        enemy.addComponent(new AnimationControllerComponent(EnemyAssets.flyingHandle, "idle"));
    }
}
