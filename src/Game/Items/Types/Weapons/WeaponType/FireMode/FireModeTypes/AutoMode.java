package Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes;


import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;

public class AutoMode implements iFireMode {

    @Override
    public FireModeResult handleInput(
            boolean held,
            boolean pressed,
            WeaponComport weapon) {

        return new FireModeResult(
                held,
                1.0,
                1.0
        );
    }

    @Override
    public void update() {}
} 