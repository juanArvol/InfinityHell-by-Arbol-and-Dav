package Game.Gameplay.Events;

import Game.Items.Types.Weapons.ModifiedWeapon;

/**
 * Catálogo de eventos específicos del sistema de armas.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * Estos eventos representan acciones propias del sistema de armas.
 *
 * La selección de armas NO pertenece aquí. El cambio de selección es un
 * comportamiento genérico de SelectableInventory y se comunica mediante
 * InventoryEvents.OnSelectionChange<ModifiedWeapon>.
 */
public final class WeaponEvents {

    private WeaponEvents() {
    }

    // ── Disparo ───────────────────────────────────────────────────────────

    /**
     * Emitido cada vez que el arma dispara con éxito.
     *
     * @param weapon          arma que disparó
     * @param projectileCount cantidad de proyectiles generados
     */
    public record OnWeaponFired(
            ModifiedWeapon weapon,
            int projectileCount
    ) {
    }

    /**
     * Emitido cuando el jugador intenta disparar con el cargador vacío.
     *
     * Útil para sonido de clic, animaciones, feedback, etc.
     *
     * @param weapon arma que intentó disparar
     */
    public record OnEmptyMagazine(
            ModifiedWeapon weapon
    ) {
    }

    // ── Recarga ───────────────────────────────────────────────────────────

    /**
     * Emitido cuando comienza una recarga.
     *
     * @param weapon        arma que está recargando
     * @param reloadTimeTicks duración total de la recarga
     */
    public record OnReloadStart(
            ModifiedWeapon weapon,
            double reloadTimeTicks
    ) {
    }

    /**
     * Emitido cuando la recarga termina.
     *
     * @param weapon arma recargada
     */
    public record OnReloadComplete(
            ModifiedWeapon weapon
    ) {
    }
}