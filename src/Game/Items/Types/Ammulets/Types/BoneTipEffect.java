package Game.Items.Types.Ammulets.Types;

import Game.Items.Types.Ammulets.AmuletEffect;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Bone Tip — incrementa el daño base del arma.
 * Acumulable: cada copia añade +8.0 de daño.
 */
public final class BoneTipEffect implements AmuletEffect {

    @Override
    public void applyToStats(WeaponStats stats) {
        stats.setDamageBonusByWeapon(stats.getDamageBonusByWeapon() + 8.0);
    }
}
