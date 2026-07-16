package Game.Enemys.EnemyTypes.Zombie;

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
 * Ensamblador del Zombie — enemigo terrestre estándar.
 *
 * Reemplaza la cadena EnemyNormal → GroundTypeEnemy → Enemy (legacy).
 *
 * ── Lo que configura ────────────────────────────────────────────────────
 *   Movimiento : GroundMovement (sincroniza enElSuelo desde física)
 *   IA         : AggressiveBehavior (persigue al jugador en eje X)
 *   Variables  : speed=1.0, damage=10, detection_range=400, attack_range=50
 *   Visual     : AnimationController con "idle"
 *
 * ── Lo que NO hace ───────────────────────────────────────────────────────
 *   No tiene fases ni patrones de ataque — es un enemigo simple.
 *   No participa en el ciclo de vida después de retornar el Enemy.
 */
public final class ZombieAssembler extends EnemyAssembler {

    private static final int    MAX_HEALTH       = 100;
    private static final double DETECTION_RANGE  = 400.0;
    private static final double ATTACK_RANGE     = 50.0;
    private static final double MOVE_SPEED       = 1.0;
    private static final int    DAMAGE           = 10;

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
        // ── Movimiento ────────────────────────────────────────────────────
        enemy.getMovementController().setStrategy(new GroundMovement());

        // ── IA ────────────────────────────────────────────────────────────
        enemy.getAIController().setBehavior(
            new AggressiveBehavior(DETECTION_RANGE, ATTACK_RANGE, MOVE_SPEED)
        );

        // ── Variables ─────────────────────────────────────────────────────
        enemy.getVariables()
            .set(EnemyVariables.Keys.SPEED,           MOVE_SPEED)
            .set(EnemyVariables.Keys.DAMAGE,          DAMAGE)
            .set(EnemyVariables.Keys.DETECTION_RANGE, DETECTION_RANGE)
            .set(EnemyVariables.Keys.ATTACK_RANGE,    ATTACK_RANGE);

        // ── Visual ────────────────────────────────────────────────────────
        enemy.addComponent(new AnimationController(EnemyAssets.normalHandle, "idle"));
    }
}
