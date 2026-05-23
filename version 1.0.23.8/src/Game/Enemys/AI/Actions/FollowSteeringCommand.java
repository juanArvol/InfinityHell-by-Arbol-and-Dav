package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Enemy;
import Game.Player.Player;
import GameMath.Vector2D;

public class FollowSteeringCommand implements EnemyAction {

    private Player player;

    private double maxSpeed = 10;
    private double steeringForce = 9999999;

    public FollowSteeringCommand(Player player){
        this.player = player;
    }

    @Override
    public void execute(Enemy enemy){

        enemy.getState().setMoving(true);

        Vector2D desired =
                player.getCenter()
                .subtract(enemy.getCenter());

        if(desired.lengthSquared() == 0)
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