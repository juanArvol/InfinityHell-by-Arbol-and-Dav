package Game.Items.Types.Weapons.WeaponType.Shoot;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Bullets.BulletType;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeaponShoot implements Shoot{
    private WeaponComport comport;

    public WeaponShoot(WeaponComport comport){
        this.comport = comport;
    }
    @Override
    public ShootResult shoot(double spawnX,
                         double spawnY,
                         boolean right,
                         Vector2D direction,
                         BulletType type,
                         double damageFireModeMultiplier,
                         double speedFireModeMultiplier 
                        ) {

        if (comport.getFireWait() > 0 || !comport.canShoot())
            return new ShootResult(Collections.emptyList(), null);

        List<Bullet> bullets = new ArrayList<>();

        for (int i = 0; i < comport.getStats().getBulletsPerShot(); i++) {
            Vector2D spreadDirection = direction.applySpread(direction, comport.getStats().getSpread()).normalize();
            bullets.add(BulletFactory.createBullet(spawnX, spawnY, spreadDirection, type, comport.getStats().getBulletSpeedBase() * speedFireModeMultiplier, comport.getStats().getDamageBonusByWeapon() * damageFireModeMultiplier));
        }
        comport.triggerCooldown();
        comport.incrementBurst();
        comport.consumeAmmo();
        
        return new ShootResult(bullets, "Gun.wav");
    }
}
