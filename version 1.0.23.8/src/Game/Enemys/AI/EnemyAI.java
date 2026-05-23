package Game.Enemys.AI;

import Game.Enemys.Enemy;
import Game.Player.Player;

public class EnemyAI {

    private EnemyComport behavior;

    public EnemyAI(EnemyComport behavior){
        this.behavior = behavior;
    }

    public void setBehavior(EnemyComport behavior){
        this.behavior = behavior;
    }

    public void update(
            Enemy enemy,
            Player player
    ){

        EnemyAction action =
            behavior.decideAction(enemy, player);

        if(action != null)
            action.execute(enemy);
    }

}
