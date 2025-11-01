package Game.Bullets.BulletType;

import Game.Bullets.Bullet;
import Game.GameObjects;
import Game.Player;

public interface BulletComport {
    double getSpeed();
    boolean hasGravity();
    int getDamage();
    void onUpdate(Bullet bullet, Player owner);
    void onCollision(Bullet bullet, GameObjects other, Player owner);
}