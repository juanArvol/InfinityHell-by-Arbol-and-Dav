
package Game.Enemys.Core.Movement;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Contracts.MovementStrategy;
import Game.Enemys.Core.Enemy;
import Game.Engine.Entity.Components.Physics2DComponent;

/**
 * Estrategia de movimiento terrestre.
 *
 * Sincroniza el flag enElSuelo desde la física hacia EnemyState cada frame.
 * La física con gravedad ya está configurada en EnemyDefinition — esta
 * estrategia solo lee el resultado que el engine calculó en FASE 0.
 *
 * ── Por qué existe ───────────────────────────────────────────────────────
 * En el modelo legacy, GroundTypeEnemy.updateTypePhysics() hacía exactamente
 * esto. Ahora vive aquí como estrategia intercambiable: si un enemy terrestre
 * obtiene alas en una fase, setStrategy(new FlyingMovement()) lo transforma
 * sin reconstruir el Enemy ni cambiar su jerarquía.
 *
 * ── applyGravity() NO se llama aquí ─────────────────────────────────────
 * La gravedad la aplica CollisionsSystem en FASE 0.5, después de que
 * FASE 0 actualizó onGround. (Ver BUG-15 del proyecto).
 */
public final class GroundMovement implements MovementStrategy {

    @Override
    public void move(Enemy enemy, EnemyContext ctx, double deltaTime) {
        Physics2DComponent pc = enemy.getPhysicsComponent();
        if (pc == null) return;

        // Sincronizar enElSuelo desde la física hacia EnemyState.
        // MoveCommand.moveX() lo necesita para calcular la aceleración correctamente.
        enemy.getState().setEnElSuelo(pc.getPhysics().getOnGround());
    }

    @Override
    public void onActivate(Enemy enemy) {
        // Al activarse como terrestre, el enemy ya no está volando
        enemy.getState().setFlying(false);
    }
}
