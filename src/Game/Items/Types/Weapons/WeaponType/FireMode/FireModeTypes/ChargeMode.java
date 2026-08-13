package Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes;

import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResolution;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;

public class ChargeMode implements iFireMode {

    private int chargeTime = 0;
    private final int maxCharge = 60;

    @Override
    public FireModeResult handleInput(
            boolean held,
            boolean pressed,
            WeaponComport weapon) {

        if (held) {
            chargeTime++;
            return new FireModeResult(false, 1, 1);
        }

        if (!held && chargeTime > 0) {

            double multiplier =
                    1.0 + ((double) chargeTime / maxCharge);

            chargeTime = 0;

            return new FireModeResult(
                    true,
                    multiplier,
                    multiplier
            );
        }

        return new FireModeResult(false, 1, 1);
    }

    @Override
    public FireModeResolution queryResolution(
            boolean held,
            WeaponComport weapon) {
        
        // ── CONSULTA IDEMPOTENTE DE MULTIPLICADORES ───────────────────────
        // 
        // ChargeMode calcula multiplicadores basándose en el tiempo de carga actual.
        // Esta operación NO muta chargeTime, solo lee su valor actual.
        // 
        // COMPORTAMIENTO:
        // - Si held=false y chargeTime=0: multiplicadores neutros (1.0, 1.0)
        // - Si held=false y chargeTime>0: multiplicadores de la carga acumulada
        // - Si held=true: multiplicadores de la carga actual (sin incrementar chargeTime)
        // 
        // DIFERENCIA CON handleInput():
        // - handleInput(): procesa held → incrementa chargeTime → calcula
        // - queryResolution(): lee chargeTime actual → calcula (sin incrementar)
        
        if (!held && chargeTime == 0) {
            // Sin carga acumulada y no presionando → multiplicadores neutros
            return FireModeResolution.NEUTRAL;
        }
        
        // Calcular multiplicadores basándose en carga actual
        // (misma fórmula que handleInput, pero sin mutar estado)
        double multiplier = 1.0 + ((double) chargeTime / maxCharge);
        
        return new FireModeResolution(multiplier, multiplier);
    }

    @Override
    public void update() {}
}
