package Game.Bullets.BulletComport;

import Game.Enemys.Enemy;
import Game.Player.Player;
import Game.World.WorldObjects.BlockWorld;
import Game.World.WorldObjects.Obstacle;
import Game.World.WorldObjects.Visuals.BackGround;
import Game.Bullets.Bullet;

public abstract class BulletBehavior {

    private final int bulletBaseDamage;
    private final double speedFactor;
    private final boolean gravity;
    private final double gravityValue;
    private final int lifeTime;

    protected BulletBehavior(int bulletBaseDamage,
                             double speedFactor,
                             boolean gravity,
                             double gravityValue,
                             int lifeTime) {

        this.bulletBaseDamage = bulletBaseDamage;
        this.speedFactor = speedFactor;
        this.gravity = gravity;
        this.gravityValue = gravityValue;
        this.lifeTime = lifeTime;
    }

    public int getBulletBaseDamage() { return bulletBaseDamage; }
    public double getSpeedFactor() { return speedFactor; }
    public int getLifeTime() { return lifeTime; }
    public boolean hasGravity() { return gravity; }
    public double getGravityValue() { return gravityValue; }

    public void update(Bullet bullet) {}

    public void onCollision(Bullet bullet, Player player) {}
    public void onCollision(Bullet bullet, Enemy enemy) {}
    public void onCollision(Bullet bullet, BackGround ambiente) {}
    public void onCollision(Bullet bullet, Bullet other) {}
    public void onCollision(Bullet bullet, Obstacle obstacle) {}
    public void onCollision(Bullet bullet, BlockWorld block) {}
} 