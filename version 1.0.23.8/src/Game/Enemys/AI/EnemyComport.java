package Game.Enemys.AI;

import Game.Enemys.Enemy;
import Game.Player.Player;

public interface EnemyComport {

    EnemyAction decideAction(
            Enemy enemy,
            Player player
    );

} 