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
 *   Legacy: 30 frames @ 30 FPS
 *   Conversión: 30 / 30 = 1.0 segundos
 *   
 * Verificación @ 30 FPS (dt=1/30):
 *   30 frames × 1.0s = 30 frames ✓
 */
public class WeaponEscopeta extends WeaponComport {

    public WeaponEscopeta() {
        super(new WeaponStats(
            1.0, // cooldown en segundos (30 frames @ 30 FPS)
            12, // balas por disparo
            35, // spread
            17, // daño
            20 // velocidad
            ),
        new AutoMode(),
        12, 
        5,
        "Gun.wav");
    }
} 