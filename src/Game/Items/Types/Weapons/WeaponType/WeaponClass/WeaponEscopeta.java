package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

public class WeaponEscopeta extends WeaponComport {

    public WeaponEscopeta() {
        super(new WeaponStats(
            30, // cooldown
            8, // balas por disparo
            35, // spread
            17, // daño
            20 // velocidad
            ),
        new AutoMode(),
        600, 
        5,
        "Gun.wav");
    }
} 