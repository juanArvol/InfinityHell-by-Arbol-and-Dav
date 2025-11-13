package Game.Weapons.WeaponType;

public abstract class weaponComport {
    protected int bulletCount;
    protected int fireReset;      // ticks restantes para disparar de nuevo
    protected int fireRate;        // disparos por segundo
    protected int fireWait;       // ticks entre disparos
    protected int baseCooldown;      // tiempo base entre disparos
    protected int maxCooldown;      // maximo tiempo entre disparos
    protected int burstCount;        // cuántas balas seguidas
    protected int burstLimit;        // límite de ráfaga rápida
    protected int bulletsPerShot;  // balas por disparo;
}
