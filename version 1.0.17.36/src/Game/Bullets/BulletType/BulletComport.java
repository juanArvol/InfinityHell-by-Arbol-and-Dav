package Game.Bullets.BulletType;

import Game.Bullets.Bullet;
import Game.Bullets.BulletType.BulletClass.BulletCollidable;
import Game.Player;

public interface BulletComport extends BulletCollidable{
    double getSpeed();
    boolean hasGravity();
    int getDamage();
    void onUpdate(Bullet bullet, Player owner);
    void onCollision(Bullet bullet, Player owner);
}