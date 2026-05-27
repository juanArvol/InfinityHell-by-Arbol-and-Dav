package Game.Enemys.AI.Behaviors;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.AI.Actions.FollowSteeringCommand;
import Game.Enemys.Enemy;

/**
 * Comportamiento de vuelo — persigue al objetivo con steering suave.
 *
 * CAMBIO vs. original:
 *   - Ya no recibe `Player player` en el constructor.
 *   - El Player (o cualquier objetivo) llega via EnemyContext en decideAction().
 *   - FollowSteeringCommand recibe el contexto cada frame, no una referencia fija.
 *
 * Esto permite que el enemigo volador cambie de objetivo en runtime
 * (ej: player muerto → ir a un punto de patrulla) sin reconstruir el behavior.
 *
 * Uso:
 *   new FlyingBehavior()                  // speed=3.0, steeringForce=0.15
 *   new FlyingBehavior(2.0, 0.1)          // más lento
 */
public class FlyingBehavior implements EnemyComport {

    private final double maxSpeed;
    private final double steeringForce;

    public FlyingBehavior() {
        this(3.0, 0.15);
    }

    public FlyingBehavior(double maxSpeed, double steeringForce) {
        this.maxSpeed      = maxSpeed;
        this.steeringForce = steeringForce;
    }

    @Override
    public EnemyAction decideAction(Enemy enemy, EnemyContext ctx) {
        // La action recibe el ctx cada frame — no guarda referencia al Player
        return new FollowSteeringCommand(ctx, maxSpeed, steeringForce);
    }
}
