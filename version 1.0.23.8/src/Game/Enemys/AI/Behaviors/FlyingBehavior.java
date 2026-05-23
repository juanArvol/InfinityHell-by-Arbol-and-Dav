package Game.Enemys.AI.Behaviors;

import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.Actions.FollowSteeringCommand;
import Game.Enemys.Enemy;
import Game.Player.Player;

public class FlyingBehavior implements EnemyComport{
    private EnemyAction action;

    public FlyingBehavior(Player player){
        action = new FollowSteeringCommand(player);

    }

    @Override
    public EnemyAction decideAction(
            Enemy enemy,
            Player player){
        return action;
    }
}