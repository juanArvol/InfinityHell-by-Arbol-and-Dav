package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Enemy;

public class IdleCommand implements EnemyAction {

    @Override
    public void execute(Enemy enemy){

        enemy.getState().setMoving(false);

        enemy.getPhysics().stopX();

    }

}