package Game.Items.Types.Weapons;

import Game.Engine.GameEventBus;
import Game.Items.Savement.Types.SelectableInventory;
import Game.Items.Types.Weapons.WeaponType.WeaponType;

/**
 * Inventario seleccionable especializado para armas.
 *
 * La lógica de selección pertenece a SelectableInventory.
 * Esta clase únicamente proporciona una API específica del dominio de armas.
 */
public class WeaponInventory
        extends SelectableInventory<ModifiedWeapon> {

    public WeaponInventory(GameEventBus eventBus) {
        super(eventBus);
    }

    public WeaponInventory() {
        super();
    }

    // ── Gestión de armas ─────────────────────────────────────────────────

    public boolean addWeapon(ModifiedWeapon weapon) {
        return addItem(weapon);
    }

    public boolean removeWeapon(ModifiedWeapon weapon) {
        return removeItem(weapon);
    }

    public ModifiedWeapon removeWeaponAt(int index) {
        return removeItemAt(index);
    }

    public ModifiedWeapon getWeaponBy(int index) {
        return getItem(index);
    }

    public boolean hasWeapon(WeaponType weaponType) {

        if (weaponType == null) {
            return false;
        }

        for (ModifiedWeapon weapon : inventoryItem) {

            if (weapon.getWeaponType() == weaponType) {
                return true;
            }
        }

        return false;
    }

    // ── Selección ────────────────────────────────────────────────────────

    public ModifiedWeapon getCurrentWeapon() {
        return getCurrent();
    }

    public void nextWeapon() {
        next();
    }

    public void previousWeapon() {
        previous();
    }

    public void selectWeaponAt(int index) {
        selectAt(index);
    }

    public int getCurrentWeaponIndex() {
        return getCurrentIndex();
    }

    // ── Inventario ───────────────────────────────────────────────────────

    public void clearWeaponsInventory() {
        clear();
    }
}