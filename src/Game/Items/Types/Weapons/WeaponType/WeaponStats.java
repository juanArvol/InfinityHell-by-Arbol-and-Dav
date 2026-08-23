package Game.Items.Types.Weapons.WeaponType;

/**
 * Estadísticas base de un arma.
 *
 * ── MIGRACIÓN TEMPORAL COMPLETA ───────────────────────────────────────────
 *
 * TODOS los parámetros temporales/cinemáticos están en unidades del sistema
 * temporal puro:
 *
 *   cooldown       → segundos (s)
 *   bulletSpeedBase → unidades/segundo (units/s)
 *   recoilForce    → unidades de fuerza
 *
 * NO hay referencias a frames, FPS, ni conversiones hardcodeadas.
 * El sistema es independiente del framerate del juego.
 *
 * ── HRFC — Kinetic Physics: Forces, Impulses & Motion Intent ─────────────
 *
 * recoilForce: Magnitud del impulso de retroceso aplicado al shooter cuando
 * dispara. Clasificado como IMPULSE (no Motion Intent) porque es reacción
 * física directa aplicada en dirección opuesta al disparo.
 */
public class WeaponStats {

    private double cooldown;  // cooldown entre disparos en segundos
    private int bulletsPerShot;
    private double spread;
    private double bulletSpeedBase;  // velocidad del proyectil en units/s
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
     * @param cooldown tiempo entre disparos en segundos (ej: 1.0 = 1 disparo/segundo)
     * @param bulletsPerShot número de proyectiles por disparo
     * @param spread ángulo de dispersión en grados
     * @param weaponDamageBulletBonus daño bonus del arma
     * @param bulletSpeedBase velocidad del proyectil en units/s
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