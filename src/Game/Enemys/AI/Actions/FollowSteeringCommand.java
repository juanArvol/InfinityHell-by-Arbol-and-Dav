package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.EnemyContext;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Enemys.Enemy;

/**
 * Sigue al objetivo con steering suave.
 *
 * CAMBIO vs. original:
 *   - Recibe `EnemyContext ctx` en lugar de `Player player`.
 *   - Mismo algoritmo de steering — solo cambia de dónde lee la posición objetivo.
 *
 * FIX BUG-14 (conservado): steeringForce era 9999999 (teleport).
 * Ahora default = 0.15 → persecución fluida y realista.
 */
public class FollowSteeringCommand implements EnemyAction {

    private final EnemyContext ctx;
    private final double maxSpeed;
    private final double steeringForce;

    /** Constructor con EnemyContext — el preferido desde FlyingBehavior. */
    public FollowSteeringCommand(EnemyContext ctx, double maxSpeed, double steeringForce) {
        this.ctx           = ctx;
        this.maxSpeed      = maxSpeed;
        this.steeringForce = steeringForce;
    }

    /** Defaults: maxSpeed=3.0, steeringForce=0.15 */
    public FollowSteeringCommand(EnemyContext ctx) {
        this(ctx, 3.0, 0.15);
    }

    @Override
    public void execute(Enemy enemy) {
        enemy.getState().setMoving(true);

        Vector2D desired = ctx.getCenter()
                .subtract(enemy.getCenter());

        if (desired.lengthSquared() == 0) return;

        desired.normalize().scaleLocal(maxSpeed);

        Vector2D steering = desired.subtract(
            enemy.getPhysics().getVelocity()
        );

        steering.scaleLocal(steeringForce);

        enemy.getPhysics()
             .getVelocity()
             .addLocal(steering);

        enemy.getPhysics()
             .getVelocity()
             .limit(maxSpeed);
    }
}
