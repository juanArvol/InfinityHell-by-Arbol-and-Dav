package Game.Enemys.EnemyTypes.Hybrid;

import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Movement.GroundMovement;
import Game.Enemys.Core.Variables.EnemyVariables;
import Game.Enemys.AI.Behaviors.AggressiveBehavior;
import Game.Enemys.EnemyPhysicsConfig;
import Game.Engine.Components.Visuals.AnimationController;
import Sprites.Enemys.EnemyAssets;

/**
 * Ensamblador del enemigo híbrido tierra/vuelo.
 *
 * Reemplaza HybridFlyGroundTypeEnemy (legacy).
 *
 * ── Diferencia con el legacy ─────────────────────────────────────────────
 * El legacy usaba un flag booleano flyingMode y subclaseaba Enemy.
 * En el nuevo framework, cambiar de modo terrestre a aéreo es simplemente:
 *
 *   enemy.getMovementController().setStrategy(new FlyingMovement(), enemy);
 *   enemy.getAIController().setBehavior(new FlyingBehavior());
 *
 * Una fase o un EnemyComponent pueden hacer esa transición en runtime sin
 * conocer la clase concreta del enemy. El Enemy es siempre el mismo objeto.
 *
 * ── Configuración inicial ────────────────────────────────────────────────
 *   Empieza en modo terrestre. La lógica de juego (una fase, un trigger de
 *   gameplay) puede llamar setStrategy(new FlyingMovement()) en cualquier
 *   momento para activar el modo vuelo.
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
            .build();
    }

    @Override
    protected void configure(Enemy enemy) {
        // ── Movimiento inicial: terrestre ─────────────────────────────────
        enemy.getMovementController().setStrategy(new GroundMovement());

        // ── IA: persecución agresiva ──────────────────────────────────────
        enemy.getAIController().setBehavior(
            new AggressiveBehavior(DETECTION_RANGE, ATTACK_RANGE, MOVE_SPEED)
        );

        // ── Variables ─────────────────────────────────────────────────────
        enemy.getVariables()
            .set(EnemyVariables.Keys.SPEED,           MOVE_SPEED)
            .set(EnemyVariables.Keys.DAMAGE,          DAMAGE)
            .set(EnemyVariables.Keys.DETECTION_RANGE, DETECTION_RANGE)
            .set(EnemyVariables.Keys.ATTACK_RANGE,    ATTACK_RANGE)
            .set("can_fly", true);  // indica capacidad de vuelo al sistema de fases

        // ── Visual ────────────────────────────────────────────────────────
        enemy.addComponent(new AnimationController(EnemyAssets.normalHandle, "idle"));
    }
}
