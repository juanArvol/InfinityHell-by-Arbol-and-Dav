package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;

/**
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 * 
 * MIGRACIÓN: cooldown ahora en segundos (no frames).
 * 
 * ANTES: 20 frames @ 60 FPS = 0.333 segundos
 * AHORA: 0.333 segundos (independiente del FPS)
 */
public class WeaponPistola extends WeaponComport {

    public WeaponPistola() {
        super(new WeaponStats(
            0.333, // cooldown en segundos (20 frames @ 60 FPS)
            1, // balas por disparo
            0, // spread
            15, // daño
            10 // velocidad
            ), 
        new AutoMode(), 
        10, 
        20, 
        "Gun.wav");
    }
} 