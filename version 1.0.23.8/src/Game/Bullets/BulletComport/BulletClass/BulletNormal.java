package Game.Bullets.BulletComport.BulletClass;

import Game.Enemys.Enemy;
import Game.World.WorldObjects.Visuals.BackGround;
import Game.Bullets.Bullet;
import Game.Bullets.BulletComport.BulletBehavior;

public class BulletNormal extends BulletBehavior {

    public BulletNormal() {
        super(15, 1, false,0, 10);
    }

    @Override
    public void onCollision(Bullet bullet, Enemy enemy) {
        bullet.getBulletLife().setDead();
    }

    @Override
    public void onCollision(Bullet bullet, BackGround ambiente) {
        bullet.getBulletLife().setDead();
    }
} 