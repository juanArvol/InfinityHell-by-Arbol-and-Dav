package Game.Engine.Colisions;

import Game.Enemys.Enemy;
import Game.Engine.GameObjects;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
import Game.Bullets.Bullet;

public interface CollisionVisitor {
    void visit(Player player);
    void visit(Enemy enemy);
    void visit(BlockWorld block);
    void visit(Bullet bullet);
    void visit(Obstacle obstacle);

    void visit(GameObjects other);
}