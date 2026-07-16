package Game.Enemys.EnemyTypes.Flying;

import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Movement.FlyingMovement;
import Game.Enemys.Core.Variables.EnemyVariables;
import Game.Enemys.AI.Behaviors.FlyingBehavior;
import Game.Enemys.EnemyPhysicsConfig;
import Game.Engine.Components.Visuals.AnimationController;
import Game.Engine.Components.Visuals.ShadowComponent;
import Sprites.Enemys.EnemyAssets;

/**
 * Ensamblador del enemigo volador.
 *
 * Reemplaza la cadena EnemyFlying → FlyingTypeEnemy → Enemy (legacy).
 *
 * ── Lo que configura ─────────────────────────────────────────────────────
 *   Movimiento : FlyingMovement (activa flag flying, sin gravedad)
 *   IA         : FlyingBehavior (steering suave hacia el objetivo)
 *   Variables  : speed=3.0, steering_force=0.15
 *   Visual     : ShadowComponent + AnimationController "idle"
 *
 * ── Física ───────────────────────────────────────────────────────────────
 *   EnemyPhysicsConfig.flyingStandard() — gravity=0, sin aceleración terrestre.
 *   El steering de FlyingBehavior maneja todo el desplazamiento.
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
            .build();
    }

    @Override
    protected void configure(Enemy enemy) {
        // ── Movimiento ────────────────────────────────────────────────────
        enemy.getMovementController().setStrategy(new FlyingMovement());

        // ── IA ────────────────────────────────────────────────────────────
        enemy.getAIController().setBehavior(
            new FlyingBehavior(MAX_SPEED, STEERING_FORCE)
        );

        // ── Variables ─────────────────────────────────────────────────────
        enemy.getVariables()
            .set(EnemyVariables.Keys.SPEED, MAX_SPEED)
            .set("steering_force", STEERING_FORCE);

        // ── Visual ────────────────────────────────────────────────────────
        enemy.addComponent(new ShadowComponent(18, 7));
        enemy.addComponent(new AnimationController(EnemyAssets.flyingHandle, "idle"));
    }
}
