package Game.Items.Types.Weapons.WeaponType.FireMode.FireModeTypes;


import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResolution;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResult;
import Game.Items.Types.Weapons.WeaponType.FireMode.iFireMode;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;

public class AutoMode implements iFireMode {

    @Override
    public FireModeResult handleInput(
            boolean held,
            boolean pressed,
            WeaponComport weapon) {

        return new FireModeResult(
                held,
                1.0,
                1.0
        );
    }

    @Override
    public FireModeResolution queryResolution(
            boolean held,
            WeaponComport weapon) {
        
        // AutoMode no tiene estado interno ni multiplicadores variables.
        // Siempre retorna multiplicadores neutros (1.0, 1.0).
        // El parámetro 'held' no afecta la resolución en modo automático.
        return FireModeResolution.NEUTRAL;
    }

    @Override
    public void update(double deltaTime) {}
} 