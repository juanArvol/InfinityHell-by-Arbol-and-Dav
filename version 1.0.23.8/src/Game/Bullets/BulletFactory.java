package Game.Bullets;

import Game.Bullets.BulletComport.BulletBehavior;
import Game.Bullets.BulletComport.BulletStats;
import GameMath.Vector2D;
import Graficos.Bullets.BulletAssets;

public class BulletFactory {

    public static Bullet createBullet(
            double startX,
            double startY,
            Vector2D direction,
            BulletType type,
            double weaponBaseSpeed,
            double damage
    ) {

        Vector2D spawn = new Vector2D(startX, startY);

        BulletBehavior comport = type.create();

        double finalSpeed = weaponBaseSpeed * comport.getSpeedFactor();
        double finalDamage = damage + comport.getBulletBaseDamage();

        double xSpeed = direction.getX() * finalSpeed;
        double ySpeed = direction.getY() * finalSpeed;

        return new Bullet(
                spawn,
                BulletAssets.bala.getSprite(),
                comport,
                xSpeed,
                ySpeed,
                comport.getLifeTime(),
                finalDamage
        );
    }

    public static BulletStats getStats(
            BulletType type,
            double weaponBaseSpeed,
            double weaponDamageBonus
    ) {

        BulletBehavior comport = type.create();

        double finalSpeed = weaponBaseSpeed * comport.getSpeedFactor();
        double finalDamage = weaponDamageBonus + comport.getBulletBaseDamage();

        return new BulletStats(
                finalSpeed,
                finalDamage,
                comport.getLifeTime(),
                comport.hasGravity()
        );
    }
}