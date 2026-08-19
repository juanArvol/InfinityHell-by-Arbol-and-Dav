package Game.Items.Types.Weapons.WeaponType;

import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;

/**
 * Comportamiento base de un arma.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   WeaponComport ahora usa tiempo real en segundos en lugar de ticks discretos
 *   para cooldowns y recarga. Esto garantiza que las cadencias de fuego son
 *   independientes del framerate.
 *
 *   ANTES (frame-based):
 *     cooldown = 60 ticks → 1 segundo a 60 FPS, 15 segundos a 4 FPS
 *
 *   AHORA (time-based):
 *     cooldown = 1.0 segundos → 1 segundo real independientemente del FPS
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
 *   - Estado de disparo: fireWait (cooldown restante en segundos), burstCount.
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
 *           super(new WeaponStats(2.0, 1, 0, 80, 25),  // cooldown en segundos
 *                 new SemiAutoMode(),
 *                 5, 1.5, "Sniper.wav");  // reloadTime en segundos
 *       }
 *   }
 */
public abstract class WeaponComport {

    protected final WeaponStats stats;
    protected final iFireMode   fireMode;

    protected int    chargerSize;
    protected int    currentAmmo;
    protected double fireWait;      // cooldown restante en segundos
    protected int    burstCount;

    protected double  reloadTime;   // duración total de recarga en segundos
    protected double  reloadTimer;  // tiempo restante de recarga en segundos
    protected boolean reloading;
    protected String  shootSound;

    /**
     * @param stats       configuración estática del arma
     * @param fireMode    modo de fuego (auto, semiAuto, carga, ráfaga…)
     * @param chargerSize capacidad del cargador
     * @param reloadTime  duración de la recarga en segundos
     * @param shootSound  nombre del clip de sonido de disparo (puede ser null)
     */
    public WeaponComport(WeaponStats stats,
                         iFireMode fireMode,
                         int chargerSize,
                         double reloadTime,
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
     * Avanza los timers internos del arma (cooldown, recarga y fireMode).
     * Llamar una vez por frame desde ModifiedWeapon.update().
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime del simulation step y decrementa
     * los timers en tiempo real, no en ticks discretos.
     *
     * También propaga deltaTime al FireMode para que pueda gestionar su
     * propio estado temporal (ej: ChargeMode acumula tiempo de carga).
     *
     * @param deltaTime tiempo transcurrido en el simulation step (segundos)
     */
    public void update(double deltaTime) {
        // Cooldown entre disparos
        if (fireWait > 0.0) {
            fireWait -= deltaTime;
            if (fireWait < 0.0) fireWait = 0.0; // clampeo
        }

        // Recarga
        if (reloading) {
            reloadTimer -= deltaTime;
            if (reloadTimer <= 0.0) {
                reloading    = false;
                currentAmmo  = chargerSize;
                reloadTimer  = 0.0; // clampeo
            }
        }

        // Propagar deltaTime al FireMode
        fireMode.update(deltaTime);
    }

    // ── Control de disparo ────────────────────────────────────────────────

    /** Activa el cooldown entre disparos. */
    public void triggerCooldown()  { fireWait = stats.getCooldown(); }

    /** Incrementa el contador de proyectiles en la ráfaga actual. */
    public void incrementBurst()   { burstCount++; }

    /** Resetea el contador de ráfaga (al soltar el gatillo en BurstMode). */
    public void resetBurst()       { burstCount = 0; }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** @return tiempo de cooldown restante en segundos */
    public double getFireWait()    { return fireWait; }
    
    /** @return tiempo de cooldown configurado en segundos */
    public double getCooldown()    { return stats.getCooldown(); }
    
    public int     getCurrentAmmo() { return currentAmmo; }
    public int     getChargerSize() { return chargerSize; }
    
    /**
     * Estado interno de recarga del arma (mecánica).
     * 
     * ── HRFC — Player Reengineering v2 ────────────────────────────────────
     * 
     * IMPORTANTE: Para el estado lógico del Player ("El Player está recargando"),
     * consultar PlayerState.isReloading(). Este método retorna únicamente el
     * estado de la mecánica interna del arma.
     * 
     * Flujo correcto:
     *   - UI/Renderer → PlayerState.isReloading()
     *   - PlayerCombat → coordina entre PlayerState y WeaponComport
     *   - WeaponComport → gestiona su mecánica interna
     */
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
