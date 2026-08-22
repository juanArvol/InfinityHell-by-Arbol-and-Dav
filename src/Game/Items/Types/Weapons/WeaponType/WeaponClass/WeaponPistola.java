package Game.Items.Types.Weapons.WeaponType.WeaponClass;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes.AutoMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;

/**
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────────
 * 
 * MIGRACIÓN: cooldown ahora en segundos (no frames).
 * 
 * CORRECCIÓN CRÍTICA: El sistema legacy operaba a 30 FPS, no 60 FPS.
 * 
 * DERIVACIÓN:
 *   Legacy: 20 frames @ 30 FPS
 *   Conversión: 20 / 30 = 0.667 segundos
 *   
 * Verificación @ 30 FPS (dt=1/30):
 *   30 frames × 0.667s/frame-worth = 20 frames ✓
 */
public class WeaponPistola extends WeaponComport {

    public WeaponPistola() {
        super(new WeaponStats(
            0.667, // cooldown en segundos (20 frames @ 30 FPS)
            1, // balas por disparo
            0, // spread
            15, // daño
            10 // velocidad
            ), 
        new AutoMode(), 
        10, 
        4, 
        "Gun.wav");
    }
} 