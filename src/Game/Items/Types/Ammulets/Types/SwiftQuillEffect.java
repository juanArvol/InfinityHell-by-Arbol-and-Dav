package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Swift Quill — incrementa la velocidad de las balas.
 * Acumulable: cada copia multiplica la velocidad base por 1.15 (15% más).
 */
public final class SwiftQuillEffect implements AmuletEffect {

    @Override
    public void applyToStats(WeaponStats stats) {
        stats.setBulletSpeedBase(stats.getBulletSpeedBase() * 1.15);
    }
}
