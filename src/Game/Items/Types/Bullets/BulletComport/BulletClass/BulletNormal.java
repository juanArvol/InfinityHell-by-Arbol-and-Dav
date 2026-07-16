package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Enemys.Core.Enemy;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.World.WorldObjects.Visuals.BackGround;

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