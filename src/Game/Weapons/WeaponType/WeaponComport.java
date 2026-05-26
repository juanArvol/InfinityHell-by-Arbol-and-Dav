package Game.Weapons.WeaponType;

import Game.Bullets.BulletType;
import Game.Weapons.WeaponType.FireMode.iFireMode;
import Game.Weapons.WeaponType.Shoot.WeaponShoot;
import GameMath.Vector2D;
import Game.Weapons.WeaponType.Shoot.Shoot;
import Game.Weapons.WeaponType.Shoot.ShootResult;

public abstract class WeaponComport{

    protected WeaponStats stats;
    protected iFireMode fireMode;   
    protected Shoot shoot;

    protected int chargerSize;
    protected int currentAmmo;
    protected int fireWait;
    protected int burstCount;

    protected int reloadTime;
    protected int reloadTimer;
    protected boolean reloading;
    protected String shootSound;

    public WeaponComport(WeaponStats stats, iFireMode fireMode, int chargerSize, int reloadTime, String shootSound) {
        this.stats = stats;
        this.fireMode = fireMode;
        this.shoot = new WeaponShoot(this);

        this.chargerSize = chargerSize;
        this.currentAmmo = chargerSize;
        this.reloadTime = reloadTime;
        this.shootSound = shootSound;
    }

    public void consumeAmmo() {
        currentAmmo--;
        if (currentAmmo <= 0) {
            startReload();
        }
    }

    public void startReload() {
        if (reloading || currentAmmo == chargerSize)
            return;
        reloading = true;
        reloadTimer = reloadTime;
    }

    public void update() {
        if (fireWait > 0) fireWait--;
        if (reloading) {
            reloadTimer--;
            if (reloadTimer <= 0) {
                reloading = false;
                currentAmmo = chargerSize;
            }
        }
    }
    public ShootResult shoot(double spawnX, double spawnY, boolean right, Vector2D direction, BulletType type, double damageFireModeMultiplier, double speedFireModeMultiplier) {
        return shoot.shoot(
            spawnX,
            spawnY,
            right,
            direction,
            type,
            damageFireModeMultiplier,
            speedFireModeMultiplier
        );
    }

    public void resetBurst() { burstCount = 0; }
    public int getFireWait() { return fireWait; }
    public int getCooldown() { return stats.getCooldown(); }
    public int getCurrentAmmo() { return currentAmmo; }
    public int getChargerSize() { return chargerSize; }
    public boolean isReloading() { return reloading; }
    public boolean canShoot() { return !reloading && currentAmmo > 0; }
    public void triggerCooldown() { fireWait = stats.getCooldown(); }
    public void incrementBurst() { burstCount++; }
    public String getShootSound() { return shootSound; }

    public WeaponStats getStats() { return stats; }
    public iFireMode getFireMode() { return fireMode;}
} 