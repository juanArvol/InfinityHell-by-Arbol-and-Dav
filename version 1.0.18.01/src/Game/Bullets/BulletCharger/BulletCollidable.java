package Game.Bullets.BulletCharger;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.Player;

public interface BulletCollidable extends Collidable {
    void bulletAcceptCollisionWith(Bullet b, Collidable other);
    void bulletOnCollisionWith(Bullet b ,Player player);
    void bulletOnCollisionWith(Bullet b, EnimyNormal enemy);
    void bulletOnCollisionWith(Bullet b, Ambiente ambiente);
}
