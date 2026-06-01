package Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;

public class ChargeMode implements iFireMode {

    private int chargeTime = 0;
    private final int maxCharge = 60;

    @Override
    public FireModeResult handleInput(
            boolean held,
            boolean pressed,
            WeaponComport weapon) {

        if (held) {
            chargeTime++;
            return new FireModeResult(false, 1, 1);
        }

        if (!held && chargeTime > 0) {

            double multiplier =
                    1.0 + ((double) chargeTime / maxCharge);

            chargeTime = 0;

            return new FireModeResult(
                    true,
                    multiplier,
                    multiplier
            );
        }

        return new FireModeResult(false, 1, 1);
    }

    @Override
    public void update() {}
}
