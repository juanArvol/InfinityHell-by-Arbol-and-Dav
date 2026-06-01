package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;

public class WeaponEscopeta extends WeaponComport {

    public WeaponEscopeta() {
        super(new WeaponStats(
            35, // cooldown
            8, // balas por disparo
            35, // spread
            17, // daño
            2 // velocidad
            ),
        new AutoMode(),
        6, 
        35,
        "Pistol.wav");
    }
} 