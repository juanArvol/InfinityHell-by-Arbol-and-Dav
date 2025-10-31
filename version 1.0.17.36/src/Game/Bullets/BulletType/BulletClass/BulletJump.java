package Game.Bullets.BulletType.BulletClass;

import Game.Bullets.Bullet;
import Game.Bullets.BulletType.BulletComport;
import Game.Player;

public class BulletJump implements BulletComport {
    @Override
    public double getSpeed() { return 1; }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 15; }

    @Override
    public void onUpdate(Bullet bullet, Player owner) {
        // rebota cuando toca el suelo
    }
}
