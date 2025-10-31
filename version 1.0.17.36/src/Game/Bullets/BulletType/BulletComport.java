package Game.Bullets.BulletType;

import Game.Bullets.Bullet;
import Game.Player;

public interface BulletComport {
    double getSpeed();
    boolean hasGravity();
    int getDamage();
    void onUpdate(Bullet bullet, Player owner);
}