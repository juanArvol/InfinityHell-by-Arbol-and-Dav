package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Tempo Ring — reduce el cooldown del arma.
 * Acumulable: cada copia reduce el cooldown al 90% del valor anterior.
 */
public final class TempoRingEffect implements AmuletEffect {

    @Override
    public void applyToStats(WeaponStats stats) {
        stats.setCooldown(stats.getCooldown() * 0.90);
    }
}
