package Game.Enemys.AI.Behaviors;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.AI.Actions.IdleCommand;
import Game.Enemys.AI.Actions.MoveCommand;
import Game.Enemys.Enemy;

/**
 * Comportamiento agresivo — persigue al objetivo en el eje X.
 *
 * CAMBIO vs. original:
 *   - Ya no recibe `Player player` — lee `ctx.getPosition()`.
 *   - Configurable: detectionRange y attackRange como parámetros.
 *   - Sin hardcodear 400 y 50 en el código.
 *
 * Uso:
 *   new AggressiveBehavior()               // defaults: detect=400, stop=50
 *   new AggressiveBehavior(300, 40, 1.5)   // custom: range, stop, speed
 */
public class AggressiveBehavior implements EnemyComport {

    private final double detectionRange;
    private final double attackStopRange;
    private final double moveSpeed;

    private final EnemyAction idle;
    private final EnemyAction moveLeft;
    private final EnemyAction moveRight;

    /** Defaults idénticos al original. */
    public AggressiveBehavior() {
        this(400, 50, 1.0);
    }

    public AggressiveBehavior(double detectionRange, double attackStopRange, double moveSpeed) {
        this.detectionRange  = detectionRange;
        this.attackStopRange = attackStopRange;
        this.moveSpeed       = moveSpeed;

        this.idle      = new IdleCommand();
        this.moveLeft  = new MoveCommand(moveSpeed, false);
        this.moveRight = new MoveCommand(moveSpeed, true);
    }

    @Override
    public EnemyAction decideAction(Enemy enemy, EnemyContext ctx) {
        double dx = ctx.getPosition().getX()
                  - enemy.getTransform().getPosition().getX();

        if (Math.abs(dx) > detectionRange)  return idle;
        if (Math.abs(dx) > attackStopRange) return dx > 0 ? moveRight : moveLeft;

        return idle;
    }
}
