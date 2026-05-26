package Game.Weapons;

import java.util.List;
import Game.Bullets.Bullet;
import Game.Bullets.BulletType;
import Game.Weapons.WeaponType.WeaponComport;
import Game.Weapons.WeaponType.WeaponStats;
import GameMath.Vector2D;

public class WeaponSelected {

    private Weapon weapon;

    public WeaponSelected( WeaponComport comport, BulletType bulletType) { 
        this.weapon = new Weapon(comport, bulletType);
    }

    public List<Bullet> handleInput(
            boolean held,
            boolean pressed,
            double x,
            double y,
            boolean right,
            Vector2D direction) {

        return weapon.handleInput(held, pressed, x, y, right, direction);
    }
    
    public void update(){
        weapon.update();
    }

    public WeaponComport getWeaponComport(){
        return weapon.getComport();
    }
    
    public void resetBurst(){
        weapon.resetBurst();
    }

    public void reload() {
        weapon.reload();
    }

    public int getFireWait() {
        return weapon.getComport().getFireWait();
    }

    public int getCooldown() {
        return weapon.getComport().getCooldown();
    }
    public BulletType getBulletType() {
        return weapon.getBulletType();
    }

    public double getBulletSpeedBase(){
        return weapon.getComport().getStats().getBulletSpeedBase();
    }
    public int getMaxAmmo(){
        return weapon.getComport().getChargerSize();
    }

    public int getCurrentAmmo(){
        return weapon.getComport().getCurrentAmmo();
    }

    public boolean isReloading(){
        return weapon.getComport().isReloading();
    }

    public WeaponStats getStats(){
        return weapon.getComport().getStats();
    }
} 