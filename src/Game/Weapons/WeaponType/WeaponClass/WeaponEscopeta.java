package Game.Weapons.WeaponType.WeaponClass;

import Game.Weapons.WeaponType.WeaponComport;
import Game.Weapons.WeaponType.WeaponStats;
import Game.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;

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