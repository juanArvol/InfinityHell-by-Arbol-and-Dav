package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Ammulets.AmuletRegistry;
import Game.Items.Types.Ammulets.Effects.BounceAmuletWrapper;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;

/**
 * Echo Stone — permite que las balas reboten.
 * Acumulable: cada copia añade +1 rebote.
 */
public final class EchoStoneEffect implements AmuletEffect {

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return new BounceAmuletWrapper(
            base,
            1,
            AmuletRegistry.getEntityProvider()
        );
    }
}
