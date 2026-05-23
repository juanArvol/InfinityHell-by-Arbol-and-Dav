package Game.Weapons;

import java.util.List;

import Game.Bullets.Bullet;
import Game.Bullets.BulletType;
import Game.Weapons.WeaponType.WeaponComport;
import Game.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Weapons.WeaponType.Shoot.ShootResult;
import GameMath.Vector2D;
import Source.Sounds;

public class Weapon {

    private final WeaponComport comport;
    private final BulletType bulletType;

    public Weapon(WeaponComport comport, BulletType bulletType) {
        this.comport = comport;
        this.bulletType = bulletType;
    }

    public List<Bullet> handleInput(
            boolean held,
            boolean pressed,
            double x,
            double y,
            boolean right,
            Vector2D direction) {
        
        FireModeResult fireModeResult = comport.getFireMode().handleInput(held, pressed, comport);

        if (fireModeResult.shouldShoot()) {
            return tryShoot(x, y, right, direction, fireModeResult.getDamageMultiplier(), fireModeResult.getSpeedMultiplier());
        }

        return List.of();
    }

    public List<Bullet> tryShoot(
            double x,
            double y,
            boolean right,
            Vector2D direction,
            double damageMultiplier,
            double speedMultiplier
            ) {

        ShootResult result =
                comport.shoot(x, y, right, direction, bulletType, damageMultiplier,speedMultiplier);

        if (result.sound() != null) {
            Sounds.playSound(result.sound());
        }

        return result.bullets();
    }

    public void update() {
        comport.update();
    }

    public void reload() {
        comport.startReload();
    }
    public void resetBurst() {
        comport.resetBurst();
    }

    public WeaponComport getComport() {
        return comport;
    }
    public BulletType getBulletType() {
        return bulletType;
    }
}