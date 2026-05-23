package Game.Weapons.WeaponType;

public class WeaponStats {

    private int cooldown;
    private int bulletsPerShot;
    private double spread;
    private double bulletSpeedBase;
    private double weaponDamageBulletBonus;

    public WeaponStats(int cooldown,
                       int bulletsPerShot,
                       double spread,
                       double weaponDamageBulletBonus,
                       double bulletSpeedBase
                    ) {

        this.cooldown = cooldown;
        this.bulletsPerShot = bulletsPerShot;
        this.spread = spread;
        this.weaponDamageBulletBonus = weaponDamageBulletBonus;
        this.bulletSpeedBase = bulletSpeedBase;
    }
    // getters
    public int getCooldown() { return cooldown; }
    public int getBulletsPerShot() { return bulletsPerShot; }
    public double getSpread() { return spread; }
    public double getDamageBonusByWeapon() { return weaponDamageBulletBonus; }
    public double getBulletSpeedBase() { return bulletSpeedBase; }
    // setters (para mejoras)
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public void setBulletsPerShot(int bulletsPerShot) { this.bulletsPerShot = bulletsPerShot; }
    public void setSpread(double spread) { this.spread = spread; }
    public void setDamageBonusByWeapon(double damage) { this.weaponDamageBulletBonus = damage; }
    public void setBulletSpeedBase(double bulletSpeed) { this.bulletSpeedBase = bulletSpeed; }
}