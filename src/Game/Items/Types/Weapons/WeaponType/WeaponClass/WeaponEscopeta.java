package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 * 
 * MIGRACIÓN: cooldown ahora en segundos (no frames).
 * 
 * ANTES: 30 frames @ 60 FPS = 0.5 segundos
 * AHORA: 0.5 segundos (independiente del FPS)
 */
public class WeaponEscopeta extends WeaponComport {

    public WeaponEscopeta() {
        super(new WeaponStats(
            0.5, // cooldown en segundos (30 frames @ 60 FPS)
            124, // balas por disparo
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