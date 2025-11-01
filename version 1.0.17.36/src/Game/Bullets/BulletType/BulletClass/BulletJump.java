package Game.Bullets.BulletType.BulletClass;

import Game.Bullets.Bullet;
import Game.Bullets.BulletType.BulletComport;
import Game.GameObjects;
import Game.Player;

public class BulletJump implements BulletComport {
    private Bullet b;
    @Override
    public double getSpeed() { return 1; }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 15; }

    @Override
    public void onUpdate(Bullet bullet, Player owner) {
        this.b=bullet;
        // rebota cuando toca el suelo
    }

    @Override
    public void onCollision(Bullet bullet, GameObjects other, Player owner) {
    }
}
