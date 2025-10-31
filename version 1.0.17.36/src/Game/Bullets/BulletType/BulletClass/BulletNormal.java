package Game.Bullets.BulletType.BulletClass;

import Game.Bullets.Bullet;
import Game.Bullets.BulletType.BulletComport;
import Game.Player;

public class BulletNormal implements BulletComport {
    @Override
    public double getSpeed() { return 10; }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 10; }

    @Override
    public void onUpdate(Bullet bullet, Player owner) {
        // Movimiento recto simple, sin efectos raros
    }
}
