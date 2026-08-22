package Game.Enemys.Core.AI.Behaviors;

import Game.Enemys.Core.AI.Actions.FollowSteeringCommand;
import Game.Enemys.Core.AI.EnemyAction;
import Game.Enemys.Core.AI.EnemyComport;
import Game.Enemys.Core.AI.EnemyContext;
import Game.Enemys.Core.Enemy;

/**
 * Comportamiento de vuelo — persigue al objetivo con steering suave.
 *
 * ── HRFC — Enemy Physics & Domain Refactor ───────────────────────────────
 *
 * MIGRADO: maxSpeed ya NO se almacena localmente.
 * AHORA: se consulta dinámicamente desde enemy.getStats().getSpeed().
 *
 * El command cacheado se actualiza cada frame con el speed actual,
 * permitiendo que buffs/debuffs afecten el movimiento sin reconstruir
 * el Behavior.
 *
 * Uso:
 *   new FlyingBehavior()           // steeringForce = 0.15
 *   new FlyingBehavior(0.1)        // custom steering
 */
public class FlyingBehavior implements EnemyComport {

    private final double steeringForce;

    /** Instancia cacheada — creada una sola vez, reutilizada cada frame. */
    private FollowSteeringCommand cachedCommand;

    public FlyingBehavior() {
        this(0.15);
    }

    public FlyingBehavior(double steeringForce) {
        this.steeringForce = steeringForce;
    }

    @Override
    public EnemyAction decideAction(Enemy enemy, EnemyContext ctx) {
        // Consultar speed desde EntityStats en runtime
        double maxSpeed = enemy.getStats().getSpeed();
        
        if (cachedCommand == null) {
            // Primera llamada: crear la instancia con el contexto inicial.
            cachedCommand = new FollowSteeringCommand(ctx, maxSpeed, steeringForce);
        } else {
            // Frames posteriores: actualizar contexto y speed sin crear objeto nuevo.
            cachedCommand.updateContext(ctx);
            cachedCommand.updateMaxSpeed(maxSpeed);
        }
        return cachedCommand;
    }
}
