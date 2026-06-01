package Game.Items.Types.Weapons.WeaponType.FireMode;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;

public interface iFireMode {

    FireModeResult handleInput(
        boolean held,
        boolean pressed,
        WeaponComport weapon
    );

    void update();
} 