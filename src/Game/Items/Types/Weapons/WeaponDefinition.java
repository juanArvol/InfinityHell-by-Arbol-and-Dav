package Game.Items.Types.Weapons;

import Game.Items.Creation.ItemRarity;
import Game.Items.ItemDefinition;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;

/**
 * Definición de datos de un arma — su "plantilla" estática por run.
 *
 * ── JERARQUÍA ────────────────────────────────────────────────────────────
 * Extiende ItemDefinition para heredar la estructura común de metadata
 * (id, displayName, description, defaultRarity).
 *
 * ── DISEÑO ───────────────────────────────────────────────────────────────
 * Separa los DATOS del arma (nombre, descripción, rareza) de su
 * COMPORTAMIENTO (WeaponComport). Esto permite que el sistema de loot
 * y la UI accedan a la metadata sin necesitar instanciar el arma.
 *
 * ── UNICIDAD POR RUN ─────────────────────────────────────────────────────
 * Igual que BulletType, un arma solo puede obtenerse una vez por run.
 * El WeaponRegistry filtra las ya obtenidas antes de ofrecerlas.
 *
 * ── RAREZA CONFIGURABLE ──────────────────────────────────────────────────
 * La rareza por defecto está en el enum WeaponId. Puede sobreescribirse
 * externamente (desde un archivo de configuración de balance) sin tocar
 * el código fuente.
 *
 * Uso:
 *   WeaponDefinition def = WeaponRegistry.get(WeaponId.ETHEREAL_REVOLVER);
 *   WeaponComport comport = def.createComport();
 *   Weapon weapon = new Weapon(comport, player.getEquippedBulletType());
 *
 * @see Game.Items.ItemDefinition         clase base con metadata común
 * @see Game.Items.Types.Weapons.WeaponRegistry sistema de loot de armas
 */
public final class WeaponDefinition extends ItemDefinition {

    /** Factory que produce el WeaponComport para esta arma. */
    private final java.util.function.Supplier<WeaponComport> comportFactory;

    public WeaponDefinition(String id,
                            String displayName,
                            String description,
                            ItemRarity defaultRarity,
                            java.util.function.Supplier<WeaponComport> comportFactory) {
        super(id, displayName, description, defaultRarity);
        if (comportFactory == null)
            throw new IllegalArgumentException("comportFactory no puede ser null");
        this.comportFactory = comportFactory;
    }

    /**
     * Crea una nueva instancia del WeaponComport para esta definición.
     * Cada arma equipada por el jugador tiene su propia instancia (con cooldown, etc.).
     */
    public WeaponComport createComport() {
        return comportFactory.get();
    }
}
