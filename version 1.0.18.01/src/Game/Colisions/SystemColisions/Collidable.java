package Game.Colisions.SystemColisions;

import Game.Ambiente;
import Game.EnimyNormal;
import Game.Player;
import Game.Bullets.Bullet;

public interface Collidable extends VisitorsAcepts {
    void acceptCollision(Collidable other);
    void onCollisionWith(Player player);
    void onCollisionWith(EnimyNormal enemy);
    void onCollisionWith(Bullet bullet);
    void onCollisionWith(Ambiente ambiente);
}
