package Game.Enemys.AI.Behaviors;

import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.Actions.MoveCommand;
import Game.Enemys.AI.Actions.IdleCommand;
import Game.Enemys.Enemy;
import Game.Player.Player;

public class AggressiveBehavior implements EnemyComport{

    private EnemyAction idle =
        new IdleCommand();

    private EnemyAction moveLeft =
        new MoveCommand(1,false);

    private EnemyAction moveRight =
        new MoveCommand(1,true);

    @Override
    public EnemyAction decideAction(
            Enemy enemy,
            Player player){

        double dx =
        player.getPosition().getX()
        - enemy.getTransform().getPosition().getX();

        if(Math.abs(dx) > 400)
            return idle;

        if(Math.abs(dx) > 50)
            return dx > 0
                    ? moveRight
                    : moveLeft;

        return idle;
    }

}