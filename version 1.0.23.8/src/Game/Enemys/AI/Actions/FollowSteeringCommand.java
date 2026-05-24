package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Enemy;
import Game.Player.Player;
import GameMath.Vector2D;

/**
 * FIX BUG-14: steeringForce era 9999999, lo que causaba que los enemigos
 * voladeros se teleportaran instantaneamente al jugador en lugar de perseguirlo
 * con steering suave.
 *
 * Un steeringForce razonable (0.1 - 0.3) produce movimiento fluido:
 * el enemigo acelera hacia el jugador pero puede sobrepasar ligeramente
 * y corregir, dando sensacion de persecucion real.
 */
public class FollowSteeringCommand implements EnemyAction {

    private final Player player;

    private final double maxSpeed;

    // FIX BUG-14: era 9999999 (teleport instantaneo). Ahora 0.15 = steering suave.
    private final double steeringForce;

    public FollowSteeringCommand(Player player) {
        this(player, 3.0, 0.15);
    }

    public FollowSteeringCommand(Player player, double maxSpeed, double steeringForce) {
        this.player        = player;
        this.maxSpeed      = maxSpeed;
        this.steeringForce = steeringForce;
    }

    @Override
    public void execute(Enemy enemy) {

        enemy.getState().setMoving(true);

        Vector2D desired =
                player.getCenter()
                .subtract(enemy.getCenter());

        if (desired.lengthSquared() == 0)
            return;

        desired.normalize().scaleLocal(maxSpeed);

        Vector2D steering =
                desired.subtract(
                        enemy.getPhysics()
                             .getVelocity()
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
