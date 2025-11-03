package Game.Colisions.SystemColisions;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public interface Collidable {
    void acceptCollision(Collidable other);
    void onCollisionWith(Player player);
    void onCollisionWith(EnimyNormal enemy);
    void onCollisionWith(Bullet bullet);
    void onCollisionWith(Ambiente ambiente);
    void onCollisionWith(GameObjects other);
}
