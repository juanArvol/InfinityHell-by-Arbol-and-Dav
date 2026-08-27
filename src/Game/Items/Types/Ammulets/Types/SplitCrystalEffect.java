package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Split Crystal — incrementa el número de balas por disparo.
 * Acumulable: cada copia añade +1 bala por disparo.
 */
public final class SplitCrystalEffect implements AmuletEffect {

    @Override
    public void applyToStats(WeaponStats stats) {
        stats.setBulletsPerShot(stats.getBulletsPerShot() + 1);
    }
}
