package Game.Engine.Colisions;

import Game.Enemys.Enemy;
import Game.Engine.GameObjects;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
import Game.Bullets.Bullet;

public interface Collidable extends VisitorsAcepts {
    void acceptCollision(GameObjects other);

    void onCollisionWith(Player player);
    void onCollisionWith(Enemy enemy);
    void onCollisionWith(Bullet bullet);
    void onCollisionWith(BlockWorld block);
    void onCollisionWith(Obstacle obstacle);

    // Método genérico para Collidables que no sean tipos específicos
    default void onCollisionWith(GameObjects other) {}
} 