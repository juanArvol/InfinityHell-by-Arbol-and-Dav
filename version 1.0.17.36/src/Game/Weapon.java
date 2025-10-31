package Game;

import Game.Bullets.*;
import Source.Sounds;
import java.util.ArrayList;
public class Weapon {

    private int weaponType;
    private int typeBullet;
    private boolean isShooting = false;
    // parámetros de disparo
    private int bulletCount;
    private int fireReset;      // ticks restantes para disparar de nuevo
    private int fireRate;        // disparos por segundo
    private int fireWait;       // ticks entre disparos
    private int baseCooldown;      // tiempo base entre disparos
    private int maxCooldown;      // maximo tiempo entre disparos
    private int burstCount;        // cuántas balas seguidas
    private int burstLimit;        // límite de ráfaga rápida
    private int bulletsPerShot;  // balas por disparo;

    private Player owner;
    private ArrayList<GameObjects> gameObjects;
    private ArrayList<EnimyNormal> oEnemys;
    
    public Weapon(Player owner, int weaponType, int typeBullet, ArrayList<EnimyNormal> enemies) {
        this.owner = owner;
        this.weaponType = weaponType;
        this.typeBullet = typeBullet;
        this.oEnemys = enemies;
        // configuración según tipo de arma
        switch (weaponType) {
            case 1:{
                // pistola
                baseCooldown = 1;
                maxCooldown = 35;
                burstLimit = 20;
                bulletsPerShot = 1;
            }
            break;
            case 2:{
                // rifle
                baseCooldown = 1;
                maxCooldown = 20;
                burstLimit = 100;
                bulletsPerShot = 2;
            }
            break;
            case 3:{
                // escopeta
                baseCooldown = 15;
                maxCooldown = 185;
                burstLimit = 3;
                bulletsPerShot = 22;
            }
            break;
            case 4:{
                // ametralladora
                baseCooldown = 200;
                maxCooldown = 20;
                burstLimit = 50;
                bulletsPerShot = 1;
            }
        }
    }

    public void setGameObjects(ArrayList<GameObjects> gameObjects) {
    this.gameObjects = gameObjects;
}
    public void update() {
        // disminuir cooldown
        if(fireWait > 0){
            fireWait--;
        }
        // actualizar balas
        }

    public void resetBurst() {
        burstCount = 0;
    }

    /**
     * Intenta disparar una bala desde la posición x,y
     * @param x posición inicial X
     * @param y posición inicial Y
     * @param mirandoDerecha true si el personaje mira a la derecha
     */
    public void tryShoot(double x, double y, boolean mirandoDerecha) {
        if(fireWait >0) return;
        
        double spawnX = mirandoDerecha ? x -11.5 : x - 29;
        double spawnY = y;

        bulletType bullet = new bulletType(typeBullet);
        owner.setWait(true);

        
            shoot(spawnX, spawnY, mirandoDerecha,bullet);
            burstCount++;
            if(burstCount < burstLimit){
                fireWait = baseCooldown;
            } else{
                bulletCount+= (maxCooldown/10)+5;
                fireWait = Math.min(maxCooldown, baseCooldown + ((burstCount+bulletCount)/burstLimit)*2);
            }
    }
    private void shoot(double x, double y, boolean mirandoDerecha, bulletType bullet) {
        
    double spawnX = mirandoDerecha ? x + 8 : x - 8;
    double spawnY = y;

    Sounds.playSound("Gun.wav");
    for (int i = 0; i < bulletsPerShot; i++) {
        Bullet nuevaBala = BulletFactory.createBullet(
            bulletsPerShot,
            spawnX,
            spawnY,
            bullet,
            owner,
            oEnemys
        );
            owner.addBullet(nuevaBala);
        }
    }
}
