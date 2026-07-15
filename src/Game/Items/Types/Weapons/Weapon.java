package Game.Items.Types.Weapons;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletType;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.Shoot.ShootResult;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Sprites.Source.Sounds;
import java.util.List;

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