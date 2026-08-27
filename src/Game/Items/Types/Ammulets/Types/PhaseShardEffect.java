package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Ammulets.Effects.PiercingAmuletWrapper;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;

/**
 * Phase Shard — otorga perforación a las balas.
 * Acumulable: cada copia añade +1 perforación.
 */
public final class PhaseShardEffect implements AmuletEffect {

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return new PiercingAmuletWrapper(base, 1);
    }
}
