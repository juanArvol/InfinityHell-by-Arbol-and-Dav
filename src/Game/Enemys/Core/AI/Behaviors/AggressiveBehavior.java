package Game.Enemys.Core.AI.Behaviors;

import Game.Enemys.Core.AI.Actions.IdleCommand;
import Game.Enemys.Core.AI.Actions.MoveCommand;
import Game.Enemys.Core.AI.EnemyAction;
import Game.Enemys.Core.AI.EnemyComport;
import Game.Enemys.Core.AI.EnemyContext;
import Game.Enemys.Core.Enemy;

/**
 * Comportamiento agresivo — persigue al objetivo en el eje X.
 *
 * ── HRFC — Enemy Physics & Domain Refactor ───────────────────────────────
 *
 * MIGRADO: moveSpeed ya NO se almacena localmente.
 * AHORA: se consulta dinámicamente desde enemy.getStats().getSpeed().
 *
 * Esto permite que buffs/debuffs o modificaciones de stats afecten
 * automáticamente el comportamiento sin reconstruir el Behavior.
 *
 * Uso:
 *   new AggressiveBehavior()           // defaults: detect=400, stop=50
 *   new AggressiveBehavior(300, 40)    // custom: range, stop
 */
public class AggressiveBehavior implements EnemyComport {

    private final double detectionRange;
    private final double attackStopRange;

    private final EnemyAction idle;

    /** Defaults idénticos al original. */
    public AggressiveBehavior() {
        this(400, 50);
    }

    public AggressiveBehavior(double detectionRange, double attackStopRange) {
        this.detectionRange  = detectionRange;
        this.attackStopRange = attackStopRange;
        this.idle = new IdleCommand();
    }

    @Override
    public EnemyAction decideAction(Enemy enemy, EnemyContext ctx) {
        double dx = ctx.getPosition().getX()
                  - enemy.getTransform().getPosition().getX();

        if (Math.abs(dx) > detectionRange)  return idle;
        
        if (Math.abs(dx) > attackStopRange) {
            // Consultar speed desde EntityStats en runtime
            double speed = enemy.getStats().getSpeed();
            return dx > 0 
                ? new MoveCommand(speed, true) 
                : new MoveCommand(speed, false);
        }

        return idle;
    }
}
