package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Core.Enemy;

/** Mueve al enemigo en el eje X. Sin cambios respecto al original. */
public class MoveCommand implements EnemyAction {

    private final double speed;
    private final boolean right;

    public MoveCommand(double speed, boolean right) {
        this.speed = speed;
        this.right = right;
    }

    @Override
    public void execute(Enemy enemy) {
        enemy.getState().setMoving(true);
        enemy.getPhysics().moveX(
            right ? speed : -speed,
            enemy.getState().isEnElSuelo(),
            false
        );
    }
}
