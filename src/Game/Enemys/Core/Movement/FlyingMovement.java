package Game.Enemys.Core.Movement;

import Game.Enemys.Core.Contracts.MovementStrategy;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.AI.EnemyContext;

/**
 * Estrategia de movimiento volador.
 *
 * Los voladores no tienen gravedad — el steering maneja todo el movimiento.
 * Esta estrategia es un no-op en move(): el steering lo ejecuta la IA
 * (FlyingBehavior → FollowSteeringCommand). Esta clase activa el flag
 * de vuelo en EnemyState al ser asignada.
 *
 * ── Por qué existe ───────────────────────────────────────────────────────
 * En el modelo legacy, FlyingTypeEnemy.updateTypePhysics() era vacío.
 * Ahora vive aquí como estrategia intercambiable.
 *
 * Un Boss que empieza en tierra y luego levanta vuelo simplemente hace:
 *   enemy.getMovementController().setStrategy(new FlyingMovement(), enemy);
 * Sin jerarquías paralelas, sin reconstruir el Enemy.
 *
 * ── Relación con la física ───────────────────────────────────────────────
 * El EnemyAssembler del volador configura EnemyPhysics con gravity mínima
 * (0.1) en la EnemyDefinition. FlyingMovement asume que la física
 * ya está configurada correctamente — no la modifica.
 */
public final class FlyingMovement implements MovementStrategy {

    @Override
    public void move(Enemy enemy, EnemyContext ctx, double deltaTime) {
        // Sin gravedad: el steering aplicado por FlyingBehavior (via EnemyAIController)
        // maneja todo el movimiento del volador.
        // Este método es intencionalmente vacío.
    }

    @Override
    public void onActivate(Enemy enemy) {
        enemy.getState().setFlying(true);
        enemy.getState().setEnElSuelo(false);
    }

    @Override
    public void onDeactivate(Enemy enemy) {
        enemy.getState().setFlying(false);
    }
}
