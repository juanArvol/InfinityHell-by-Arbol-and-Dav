package Game.Weapons.WeaponType.WeaponClass;

import Game.Weapons.WeaponType.WeaponComport;
import Game.Weapons.WeaponType.WeaponStats;
import Game.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;

public class WeaponPistola extends WeaponComport {

    public WeaponPistola() {
        super(new WeaponStats(
            20, // cooldown
            1, // balas por disparo
            0, // spread
            15, // daño
            10 // velocidad
            ), 
        new AutoMode(), 
        10, 
        20, 
        "Pistol.wav");
    }
} 