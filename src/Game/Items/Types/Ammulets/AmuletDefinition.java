package Game.Items.Types.Ammulets;

import Game.Items.VisualDefinition;
import Game.Items.Creation.ItemDefinition;
import Game.Items.Creation.ItemID;
import Game.Items.Creation.ItemRarity;

/**
 * Definición de un amuleto — sistema legacy AmuletRegistry.
 *
 * @see Game.Items.Creation.ItemDefinition
 * @see Game.Items.Types.Ammulets.AmuletRegistry
 */
public final class AmuletDefinition extends ItemDefinition {

    /** Efecto que aplica este amuleto sobre las stats de bala. */
    public final AmuletEffect effect;

    /**
     * Constructor legacy para AmuletRegistry.
     */
    public AmuletDefinition(String id,
                            String displayName,
                            String description,
                            ItemRarity rarity,
                            AmuletEffect effect) {
        super(
            createLegacyID(id),
            new VisualDefinition(displayName, description, rarity)
        );
        
        if (effect == null)
            throw new IllegalArgumentException("effect no puede ser null");
        this.effect = effect;
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
