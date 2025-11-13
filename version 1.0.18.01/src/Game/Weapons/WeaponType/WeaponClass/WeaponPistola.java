package Game.Weapons.WeaponType.WeaponClass;

import Game.Weapons.WeaponType.weaponComport;

public class WeaponPistola extends weaponComport {
    public WeaponPistola() {
        this.bulletCount = 12;
        this.fireRate = 3; // 3 disparos por segundo
        this.fireWait = 20; // ticks entre disparos
        this.baseCooldown = 20; // tiempo base entre disparos
        this.maxCooldown = 60; // maximo tiempo entre disparos
        this.burstCount = 0; // cuántas balas seguidas
        this.burstLimit = 3; // límite de ráfaga rápida
        this.bulletsPerShot = 1; // balas por disparo
    }
    
}
