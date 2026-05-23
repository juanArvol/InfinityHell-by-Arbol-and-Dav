package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import GameMath.Vector2D;
import Game.Enemys.Enemy;

import java.util.List;

public class PathSteeringCommand implements EnemyAction {

    private List<Vector2D> path;
    private int index = 0;

    private double maxSpeed = 2;
    private double steeringForce = 0.1;

    public PathSteeringCommand(List<Vector2D> path){
        this.path = path;
    }

    @Override
    public void execute(Enemy enemy){

        if(path == null || path.isEmpty())
            return;

        Vector2D target = path.get(index);

        if(enemy.getCenter().distance(target) < 10){
            index = (index + 1) % path.size();
        }

        Vector2D desired =
                target.subtract(enemy.getCenter());

        if(desired.lengthSquared() == 0)
            return;

        enemy.getState().setMoving(true);

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