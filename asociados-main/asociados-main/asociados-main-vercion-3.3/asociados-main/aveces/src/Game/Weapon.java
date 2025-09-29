package Game;

import graficos.Assets;
import java.util.ArrayList;
import math.Vector2D;

public class Weapon {

    private int weaponType;
    private int typeBullet;
    private double bulletY;
    private double bulletX;
    private double bulletA;
    // 🔹 parámetros de disparo
    private int fireCooldown;      // ticks restantes para disparar de nuevo
    private int baseCooldown;      // disparos rápidos
    private int maxCooldown;      // disparos lentos
    private int burstCount;        // cuántas balas seguidas
    private int burstLimit;        // límite de ráfaga rápida
    private bulletType bullet; // velocidad de la bala
    // 🔹 lista de balas
    private ArrayList<Bullet> balas = new ArrayList<>();

    public Weapon(int weaponType, int typeBullet) {
        this.weaponType = weaponType;
        this.typeBullet = typeBullet;
        // configuración según tipo de arma
        switch (weaponType) {
            case 1 -> {
                // pistola
                baseCooldown = 15;
                maxCooldown = 30;
                burstLimit = 3;
            }
            case 2 -> {
                // rifle
                baseCooldown = 1;
                maxCooldown = 20000;
                burstLimit = 1000000;
            }
            default -> {
                // por defecto, pistola
                baseCooldown = 15;
                maxCooldown = 30;
            }
        }
    }

    public void setBulletA(double bulletA){
        this.bulletA = bulletA;
    }
    public void setBulletDirY(double bulletY){
        this.bulletY = bulletY;
    }
    public void setBulletDirX(double bulletX){
        this.bulletX = bulletX;
    }

    public void update() {
        // disminuir cooldown
        if (fireCooldown > 0) fireCooldown--;

        // actualizar balas
        for (Bullet b : balas) {
            b.update();
        }
    }

    public ArrayList<Bullet> getBullets() {
        return balas;
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
        bulletType bullet = new bulletType(typeBullet, bulletY, bulletX);
        if (fireCooldown == 0) {
            shoot(x, y, mirandoDerecha,bullet);
            burstCount++;

            if (burstCount < burstLimit) {
                fireCooldown = baseCooldown;
            } else {
                fireCooldown = Math.min(maxCooldown,
                        baseCooldown + (burstCount - burstLimit));
            }
        }
    }

    private void shoot(double x, double y, boolean mirandoDerecha, bulletType bullet) {
        double dir = mirandoDerecha ? 1 : -1;

        balas.add(new Bullet(
                new Vector2D(x, y), // posición inicial
                Assets.bala,
                bullet.getVelocidadX() * dir, // velocidad X
                bullet.getVelocidadY() * bulletA
        ));
    }
}
