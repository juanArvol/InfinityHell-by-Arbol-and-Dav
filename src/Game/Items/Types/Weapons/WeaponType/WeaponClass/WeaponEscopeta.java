package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

public class WeaponEscopeta extends WeaponComport {

    public WeaponEscopeta() {
        super(new WeaponStats(
            0, // cooldown
            50, // balas por disparo
            80, // spread
            17, // daño
            2 // velocidad
            ),
        new AutoMode(),
        600, 
        5,
        "Gun.wav");
    }
} 