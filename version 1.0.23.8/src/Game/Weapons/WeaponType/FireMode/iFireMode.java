package Game.Weapons.WeaponType.FireMode;

import Game.Weapons.WeaponType.WeaponComport;

public interface iFireMode {

    FireModeResult handleInput(
        boolean held,
        boolean pressed,
        WeaponComport weapon
    );

    void update();
} 