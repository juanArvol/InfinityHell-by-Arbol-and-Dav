package Game.Weapons.WeaponType;

import Game.Weapons.WeaponType.WeaponClass.WeaponPistola;

public class weaponComportFactory {
    public static weaponComport getComportamiento(int tipo) {
        return switch (tipo) {
            case 1 -> new WeaponPistola();
            //case 2 -> new WeaponRifle();
            //case 3 -> new WeaponEscopeta();
            //case 4 -> new WeaponAmetralladora();
            default -> new WeaponPistola(); // fallback
        };
    }
}
