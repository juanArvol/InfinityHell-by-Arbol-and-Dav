package Game.Items.Types.Weapons;

import Game.Items.Creation.ItemRarity;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;

/**
 * Definición de datos de un arma — su "plantilla" estática por run.
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
 */
public final class WeaponDefinition {

    /** ID único del arma (referenciado por WeaponId). */
    public final String id;

    /** Nombre visible al jugador. */
    public final String displayName;

    /** Descripción del comportamiento para UI de selección. */
    public final String description;

    /** Rareza por defecto (configurable externamente). */
    public final ItemRarity defaultRarity;

    /** Factory que produce el WeaponComport para esta arma. */
    private final java.util.function.Supplier<WeaponComport> comportFactory;

    public WeaponDefinition(String id,
                            String displayName,
                            String description,
                            ItemRarity defaultRarity,
                            java.util.function.Supplier<WeaponComport> comportFactory) {
        this.id              = id;
        this.displayName     = displayName;
        this.description     = description;
        this.defaultRarity   = defaultRarity;
        this.comportFactory  = comportFactory;
    }

    /**
     * Crea una nueva instancia del WeaponComport para esta definición.
     * Cada arma equipada por el jugador tiene su propia instancia (con cooldown, etc.).
     */
    public WeaponComport createComport() {
        return comportFactory.get();
    }

    @Override
    public String toString() {
        return "WeaponDefinition{id='" + id + "', rarity=" + defaultRarity + "}";
    }
}
