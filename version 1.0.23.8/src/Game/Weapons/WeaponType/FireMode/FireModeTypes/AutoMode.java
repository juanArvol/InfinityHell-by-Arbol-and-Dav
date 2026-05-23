package Game.Weapons.WeaponType.FireMode.FireModeTypes;


import Game.Weapons.WeaponType.WeaponComport;
import Game.Weapons.WeaponType.FireMode.iFireMode;
import Game.Weapons.WeaponType.FireMode.FireModeResult;

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