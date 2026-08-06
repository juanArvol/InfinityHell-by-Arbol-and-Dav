package Game.Items.Types.Weapons.WeaponType;

import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;

/**
 * Comportamiento base de un arma.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * WeaponComport representa el estado y los parámetros de configuración de
 * un arma concreta (cadencia, ammo, recarga, modo de fuego). NO contiene
 * lógica de creación de proyectiles — esa responsabilidad pertenece a
 * {@link Game.Items.Types.Weapons.ModifiedWeapon}.
 *
 * ── ELIMINADO ─────────────────────────────────────────────────────────────
 *
 * Se eliminó el campo {@code Shoot shoot} y el método {@code shoot(...)}
 * que delegaban a WeaponShoot. Esa indirección no aportaba valor y duplicaba
 * la lógica ya presente en ModifiedWeapon.tryShoot(). WeaponComport ahora
 * solo gestiona estado interno del arma.
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   - Configuración estática: WeaponStats (cooldown, spread, damage, speed).
 *   - Estado de disparo: fireWait (cooldown restante), burstCount.
 *   - Estado de ammo: currentAmmo, cargador, recarga.
 *   - Modo de fuego: iFireMode (auto, semiAuto, carga).
 *
 * ── EXTENSIÓN ─────────────────────────────────────────────────────────────
 *
 * Para crear un nuevo tipo de arma, extender WeaponComport y configurar
 * los valores en el constructor. No hace falta sobreescribir ningún método.
 *
 *   public class WeaponSniper extends WeaponComport {
 *       public WeaponSniper() {
 *           super(new WeaponStats(120, 1, 0, 80, 25),
 *                 new SemiAutoMode(),
 *                 5, 90, "Sniper.wav");
 *       }
 *   }
 */
public abstract class WeaponComport {

    protected final WeaponStats stats;
    protected final iFireMode   fireMode;

    protected int chargerSize;
    protected int currentAmmo;
    protected int fireWait;
    protected int burstCount;

    protected int     reloadTime;
    protected int     reloadTimer;
    protected boolean reloading;
    protected String  shootSound;

    /**
     * @param stats       configuración estática del arma
     * @param fireMode    modo de fuego (auto, semiAuto, carga, ráfaga…)
     * @param chargerSize capacidad del cargador
     * @param reloadTime  duración de la recarga en ticks
     * @param shootSound  nombre del clip de sonido de disparo (puede ser null)
     */
    public WeaponComport(WeaponStats stats,
                         iFireMode fireMode,
                         int chargerSize,
                         int reloadTime,
                         String shootSound) {
        this.stats       = stats;
        this.fireMode    = fireMode;
        this.chargerSize = chargerSize;
        this.currentAmmo = chargerSize;
        this.reloadTime  = reloadTime;
        this.shootSound  = shootSound;
    }

    // ── Ammo ──────────────────────────────────────────────────────────────

    /**
     * Consume una unidad de munición. Si llega a 0, inicia recarga automática.
     * Llamar desde ModifiedWeapon después de crear los proyectiles.
     */
    public void consumeAmmo() {
        currentAmmo--;
        if (currentAmmo <= 0) {
            startReload();
        }
    }

    /**
     * Inicia la recarga si no está ya recargando y el cargador no está lleno.
     */
    public void startReload() {
        if (reloading || currentAmmo == chargerSize) return;
        reloading    = true;
        reloadTimer  = reloadTime;
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Avanza los timers internos del arma (cooldown y recarga).
     * Llamar una vez por frame desde ModifiedWeapon.update().
     */
    public void update() {
        if (fireWait > 0) fireWait--;

        if (reloading) {
            reloadTimer--;
            if (reloadTimer <= 0) {
                reloading    = false;
                currentAmmo  = chargerSize;
            }
        }
    }

    // ── Control de disparo ────────────────────────────────────────────────

    /** Activa el cooldown entre disparos. */
    public void triggerCooldown()  { fireWait = stats.getCooldown(); }

    /** Incrementa el contador de proyectiles en la ráfaga actual. */
    public void incrementBurst()   { burstCount++; }

    /** Resetea el contador de ráfaga (al soltar el gatillo en BurstMode). */
    public void resetBurst()       { burstCount = 0; }

    // ── Consultas ─────────────────────────────────────────────────────────

    public int     getFireWait()    { return fireWait; }
    public int     getCooldown()    { return stats.getCooldown(); }
    public int     getCurrentAmmo() { return currentAmmo; }
    public int     getChargerSize() { return chargerSize; }
    public boolean isReloading()    { return reloading; }
    public int     getBurstCount()  { return burstCount; }

    /** True si el arma puede disparar ahora mismo (no recargando, tiene ammo). */
    public boolean canShoot()      { return !reloading && currentAmmo > 0; }

    /** True si el cargador está completamente lleno. */
    public boolean isFullyLoaded() { return currentAmmo >= chargerSize; }

    public String     getShootSound() { return shootSound; }
    public WeaponStats getStats()      { return stats; }
    public iFireMode   getFireMode()   { return fireMode; }
}
