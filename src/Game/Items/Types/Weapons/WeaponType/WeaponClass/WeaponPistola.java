package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;

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