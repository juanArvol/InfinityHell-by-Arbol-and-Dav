package Game.Items.Types.Weapons.WeaponType;

/**
 * Estadísticas base de un arma.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   cooldown ahora se expresa en segundos en lugar de ticks para independencia
 *   del framerate.
 *
 *   ANTES: cooldown = 60 (ticks) → 1 segundo a 60 FPS
 *   AHORA: cooldown = 1.0 (segundos) → 1 segundo independiente del FPS
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * AÑADIDO: recoilForce
 *   - Magnitud del impulso de retroceso aplicado al shooter cuando dispara
 *   - Clasificado como IMPULSE (no Motion Intent) porque es reacción física directa
 *   - Se aplica en dirección opuesta al disparo
 */
public class WeaponStats {

    private double cooldown;  // cooldown entre disparos en segundos
    private int bulletsPerShot;
    private double spread;
    private double bulletSpeedBase;
    private double weaponDamageBulletBonus;

    /**
     * Fuerza de retroceso (recoil) aplicada al shooter al disparar.
     * 
     * Magnitud del impulso en unidades de fuerza. Se aplica en dirección
     * opuesta a la dirección del disparo.
     * 
     * Valores típicos:
     *   0.0   = sin retroceso (armas de energía, láser)
     *   5.0   = retroceso ligero (pistola pequeña)
     *   15.0  = retroceso moderado (rifle)
     *   30.0  = retroceso fuerte (shotgun)
     *   50.0+ = retroceso extremo (cañón, arma pesada)
     */
    private double recoilForce;

    public WeaponStats(double cooldown,
                       int bulletsPerShot,
                       double spread,
                       double weaponDamageBulletBonus,
                       double bulletSpeedBase
                    ) {
        this(cooldown, bulletsPerShot, spread, weaponDamageBulletBonus, bulletSpeedBase, 0.0);
    }

    /**
     * Constructor con recoilForce explícito.
     * 
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     * 
     * @param cooldown tiempo entre disparos en segundos (ej: 1.0 = 1 disparo/segundo)
     * @param recoilForce magnitud del impulso de retroceso (0 = sin retroceso)
     */
    public WeaponStats(double cooldown,
                       int bulletsPerShot,
                       double spread,
                       double weaponDamageBulletBonus,
                       double bulletSpeedBase,
                       double recoilForce
                    ) {

        this.cooldown = cooldown;
        this.bulletsPerShot = bulletsPerShot;
        this.spread = spread;
        this.weaponDamageBulletBonus = weaponDamageBulletBonus;
        this.bulletSpeedBase = bulletSpeedBase;
        this.recoilForce = recoilForce;
    }
    // getters
    /** @return cooldown entre disparos en segundos */
    public double getCooldown() { return cooldown; }
    public int getBulletsPerShot() { return bulletsPerShot; }
    public double getSpread() { return spread; }
    public double getDamageBonusByWeapon() { return weaponDamageBulletBonus; }
    public double getBulletSpeedBase() { return bulletSpeedBase; }

    /**
     * Magnitud del impulso de retroceso aplicado al shooter cuando dispara.
     * 
     * @return fuerza de retroceso (0 = sin retroceso)
     */
    public double getRecoilForce() { return recoilForce; }

    // setters (para mejoras)
    /** @param cooldown tiempo entre disparos en segundos */
    public void setCooldown(double cooldown) { this.cooldown = cooldown; }
    public void setBulletsPerShot(int bulletsPerShot) { this.bulletsPerShot = bulletsPerShot; }
    public void setSpread(double spread) { this.spread = spread; }
    public void setDamageBonusByWeapon(double damage) { this.weaponDamageBulletBonus = damage; }
    public void setBulletSpeedBase(double bulletSpeed) { this.bulletSpeedBase = bulletSpeed; }

    /**
     * Establece la fuerza de retroceso.
     * 
     * @param recoilForce magnitud del impulso (debe ser >= 0)
     */
    public void setRecoilForce(double recoilForce) {
        if (recoilForce < 0) {
            throw new IllegalArgumentException("recoilForce debe ser >= 0");
        }
        this.recoilForce = recoilForce;
    }
}