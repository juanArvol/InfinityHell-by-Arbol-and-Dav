package Game.Items.Types.Weapons.WeaponType;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletType;
import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;
import Game.Items.Types.Weapons.WeaponType.Shoot.Shoot;
import Game.Items.Types.Weapons.WeaponType.Shoot.ShootResult;
import Game.Items.Types.Weapons.WeaponType.Shoot.WeaponShoot;

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

    /**
     * True si el cargador está completamente lleno.
     *
     * CONTRATO RESTAURADO: PlayerCombat llamaba currentWeapon.isFullyLoaded()
     * para decidir si iniciar una recarga. El método no existía en WeaponSelected
     * ni en WeaponComport, pero la lógica equivalente ya estaba implícita en
     * startReload() (que hace guard con currentAmmo == chargerSize).
     *
     * Que el concepto existiera pero sin API pública lo hacía inaccesible
     * para sistemas externos (PlayerCombat, UI de munición, autorecarga).
     * Se restaura como método explícito.
     */
    public boolean isFullyLoaded() { return currentAmmo >= chargerSize; }
    public void triggerCooldown() { fireWait = stats.getCooldown(); }
    public void incrementBurst() { burstCount++; }
    public String getShootSound() { return shootSound; }

    public WeaponStats getStats() { return stats; }
    public iFireMode getFireMode() { return fireMode;}
} 