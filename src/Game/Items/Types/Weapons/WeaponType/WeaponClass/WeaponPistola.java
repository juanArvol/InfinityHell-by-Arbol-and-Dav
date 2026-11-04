package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Arma: Pistola
 * 
 * MIGRACIÓN TEMPORAL COMPLETA:
 *   - cooldown: 0.667 segundos (equivalente a 20 frames @ 30 FPS legacy)
 *   - velocidad: 300 units/s (equivalente a 10 units/frame @ 30 FPS legacy)
 */
public class WeaponPistola extends WeaponComport {

    public WeaponPistola() {
        super(new WeaponStats(
        0, // cooldown en segundos
            5, // balas por disparo
            2.975, // spread
            15, // daño
            1060 // velocidad en units/s (10 × 30)
            ), 
        new AutoMode(), 
        1500, 
    5, 
        "Gun.wav");
    }
} 