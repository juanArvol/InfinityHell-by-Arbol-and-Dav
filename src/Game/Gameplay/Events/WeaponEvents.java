package Game.Gameplay.Events;

import Game.Items.Types.Weapons.ModifiedWeapon;

/**
 * Catálogo de eventos del sistema de armas.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * Centraliza todos los eventos emitidos por el sistema de armas en un único
 * archivo. Suscriptores opcionales (UI de munición, audio, achievement system,
 * analytics) se registran en el bus de eventos del mundo.
 *
 * Ninguno de estos eventos es obligatorio para el funcionamiento del sistema.
 * Si no hay suscriptores, el bus no invoca nada (coste cero).
 *
 * ── USO ────────────────────────────────────────────────────────────────────
 *
 *   // Suscribir desde UIBootstrap, AudioSystem, etc.:
 *   bus.subscribe(WeaponEvents.OnWeaponFired.class, e -> {
 *       ammoHud.onFired(e.weapon().getCurrentAmmo(), e.weapon().getMaxAmmo());
 *   });
 *
 *   bus.subscribe(WeaponEvents.OnReloadComplete.class, e -> {
 *       AudioSystem.play("reload_complete.wav");
 *   });
 */
public final class WeaponEvents {

    private WeaponEvents() {}

    // ── Disparo ────────────────────────────────────────────────────────────

    /**
     * Emitido cada vez que el arma dispara con éxito.
     *
     * @param weapon        el arma que disparó
     * @param projectileCount cuántos proyectiles se spawnearon este disparo
     */
    public record OnWeaponFired(ModifiedWeapon weapon, int projectileCount) {}

    /**
     * Emitido cuando el jugador pulsa disparar pero el cargador está vacío.
     * Útil para el sonido de "clic en vacío" y animaciones.
     *
     * @param weapon el arma que intentó disparar
     */
    public record OnEmptyMagazine(ModifiedWeapon weapon) {}

    // ── Recarga ────────────────────────────────────────────────────────────

    /**
     * Emitido cuando comienza la recarga.
     *
     * @param weapon          el arma que recarga
     * @param reloadTimeTicks duración total de la recarga en ticks
     */
    public record OnReloadStart(ModifiedWeapon weapon, int reloadTimeTicks) {}

    /**
     * Emitido cuando la recarga se completa y el cargador está lleno.
     *
     * @param weapon el arma recargada
     */
    public record OnReloadComplete(ModifiedWeapon weapon) {}

    // ── Cambio de arma ─────────────────────────────────────────────────────

    /**
     * Emitido cuando el jugador cambia al arma siguiente o anterior.
     *
     * @param previousWeapon arma anterior (puede ser null si no había ninguna)
     * @param currentWeapon  nueva arma activa
     */
    public record OnWeaponSwitch(ModifiedWeapon previousWeapon, ModifiedWeapon currentWeapon) {}
}
