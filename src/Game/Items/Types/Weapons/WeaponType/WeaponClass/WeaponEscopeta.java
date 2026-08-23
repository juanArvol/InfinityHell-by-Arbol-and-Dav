package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * Arma: Escopeta
 * 
 * MIGRACIÓN TEMPORAL COMPLETA:
 *   - cooldown: 1.0 segundos (equivalente a 30 frames @ 30 FPS legacy)
 *   - velocidad: 6000 units/s (equivalente a 200 units/frame @ 30 FPS legacy)
 */
public class WeaponEscopeta extends WeaponComport {

    public WeaponEscopeta() {
        super(new WeaponStats(
            1.0, // cooldown en segundos
            12, // balas por disparo
            35, // spread
            17, // daño
            6000 // velocidad en units/s (200 × 30)
            ),
        new AutoMode(),
        12, 
        5,
        "Gun.wav");
    }
} 