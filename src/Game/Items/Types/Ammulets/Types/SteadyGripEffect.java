package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Steady Grip — reduce la dispersión del arma.
 * Acumulable: cada copia reduce el spread al 80% del valor anterior.
 */
public final class SteadyGripEffect implements AmuletEffect {

    @Override
    public void applyToStats(WeaponStats stats) {
        stats.setSpread(stats.getSpread() * 0.80);
    }
}
