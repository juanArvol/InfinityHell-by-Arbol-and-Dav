package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Enemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import java.util.List;

/**
 * Sigue una ruta de waypoints con steering suave.
 * Sin referencia a Player — ya estaba bien en este aspecto.
 * Limpieza: configurable via constructor fluido.
 */
public class PathSteeringCommand implements EnemyAction {

    private final List<Vector2D> path;
    private final double maxSpeed;
    private final double steeringForce;
    private final double waypointReachRadius;

    private int index = 0;

    public PathSteeringCommand(List<Vector2D> path) {
        this(path, 2.0, 0.1, 10.0);
    }

    public PathSteeringCommand(List<Vector2D> path,
                               double maxSpeed,
                               double steeringForce,
                               double waypointReachRadius) {
        this.path                = List.copyOf(path); // defensiva
        this.maxSpeed            = maxSpeed;
        this.steeringForce       = steeringForce;
        this.waypointReachRadius = waypointReachRadius;
    }

    @Override
    public void execute(Enemy enemy) {
        if (path.isEmpty()) return;

        Vector2D target = path.get(index);

        if (enemy.getCenter().distance(target) < waypointReachRadius) {
            index = (index + 1) % path.size();
            target = path.get(index);
        }

        Vector2D desired = target.subtract(enemy.getCenter());
        if (desired.lengthSquared() == 0) return;

        enemy.getState().setMoving(true);

        desired.normalizeLocal().scaleLocal(maxSpeed);

        Vector2D steering = desired.subtract(
            enemy.getPhysics().getVelocity()
        );
        steering.scaleLocal(steeringForce);

        enemy.getPhysics().getVelocity().addLocal(steering);
        enemy.getPhysics().getVelocity().limitLocal(maxSpeed);
    }

    public void resetPath() { index = 0; }
}
