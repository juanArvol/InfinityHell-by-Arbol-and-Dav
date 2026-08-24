package Game.Items.Types.Weapons;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.VisualDefinition;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemID;
import Game.Items.Creation.ItemRarity;

/**
 * Definición de datos de un arma — sistema legacy WeaponRegistry.
 *
 * @see Game.Items.Creation.ItemDefinition
 * @see Game.Items.Types.Weapons.WeaponRegistry
 */
public final class WeaponDefinition extends ItemDefinition {

    /** Factory que produce el WeaponComport para esta arma. */
    private final java.util.function.Supplier<WeaponComport> comportFactory;

    /**
     * Constructor legacy para WeaponRegistry.
     */
    public WeaponDefinition(String id,
                            String displayName,
                            String description,
                            ItemRarity rarity,
                            java.util.function.Supplier<WeaponComport> comportFactory) {
        super(
            createLegacyID(id),
            new VisualDefinition(displayName, description, rarity)
        );
        
        if (comportFactory == null)
            throw new IllegalArgumentException("comportFactory no puede ser null");
        this.comportFactory = comportFactory;
    }

    /**
     * Crea una nueva instancia del WeaponComport.
     */
    public WeaponComport createComport() {
        return comportFactory.get();
    }

    /**
     * Crea un ItemID legacy desde un string.
     */
    private static ItemID createLegacyID(final String id) {
        return new ItemID() {
            @Override
            public String asString() { return id; }
        };
    }
}
