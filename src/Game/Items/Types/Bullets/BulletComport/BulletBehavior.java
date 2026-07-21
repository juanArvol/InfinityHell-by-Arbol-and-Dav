package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;

/**
 * Comportamiento de un proyectil — define su ciclo de vida y reacción a colisiones.
 *
 * ── HRFC-014 — GAP-2 / GAP-8: Desacoplamiento de tipos concretos ────────
 *
 * PROBLEMA ANTERIOR:
 *   BulletBehavior declaraba sobrecargas tipadas por tipo de objeto del Game:
 *
 *     onCollision(Bullet, Player)
 *     onCollision(Bullet, Enemy)
 *     onCollision(Bullet, BackGround)
 *     onCollision(Bullet, Obstacle)
 *     onCollision(Bullet, BlockWorld)
 *
 *   Esto acoplaba el Engine de proyectiles a tipos concretos del Game:
 *   - Cualquier entidad nueva (NPC, invocación, trampa) que recibiera balas
 *     requería añadir una nueva sobrecarga.
 *   - BulletBehavior no podía vivir sin conocer Player, Enemy, BackGround, etc.
 *   - No escalaba con la visión de Infinity Hell.
 *
 * SOLUCIÓN:
 *   Un único método genérico:
 *
 *     onCollision(Bullet bullet, GameObjects other)
 *
 *   Cada BulletBehavior concreto hace instanceof en su implementación
 *   cuando necesita distinguir tipos específicos. El Engine no los conoce.
 *
 *   Este es el mismo patrón que GameObjects.onCollisionWith(GameObjects).
 *
 * ── API ────────────────────────────────────────────────────────────────
 *
 *   update(bullet)              — lógica frame a frame del proyectil.
 *   onCollision(bullet, other)  — reacción al contacto con cualquier objeto.
 *
 * ── Ejemplo de implementación ────────────────────────────────────────────
 *
 *   public class BulletFire extends BulletBehavior {
 *       {@literal @}Override
 *       public void onCollision(Bullet bullet, GameObjects other) {
 *           if (other instanceof AbstractEntity entity) {
 *               entity.damage((int) bullet.getDamage());
 *               entity.addEffect(new BurningEffect(60));
 *           }
 *           bullet.getBulletLife().setDead();
 *       }
 *   }
 */
public abstract class BulletBehavior {

    private final int    bulletBaseDamage;
    private final double speedFactor;
    private final boolean gravity;
    private final double gravityValue;
    private final int    lifeTime;

    protected BulletBehavior(int bulletBaseDamage,
                             double speedFactor,
                             boolean gravity,
                             double gravityValue,
                             int lifeTime) {
        this.bulletBaseDamage = bulletBaseDamage;
        this.speedFactor      = speedFactor;
        this.gravity          = gravity;
        this.gravityValue     = gravityValue;
        this.lifeTime         = lifeTime;
    }

    public int    getBulletBaseDamage() { return bulletBaseDamage; }
    public double getSpeedFactor()      { return speedFactor; }
    public int    getLifeTime()         { return lifeTime; }
    public boolean hasGravity()         { return gravity; }
    public double getGravityValue()     { return gravityValue; }

    /**
     * Lógica de actualización por frame del proyectil.
     * Comportamiento por defecto: sin efecto (subclases sobreescriben si lo necesitan).
     */
    public void update(Bullet bullet) {}

    /**
     * Reacción al contacto con cualquier objeto del mundo.
     *
     * Las subclases usan instanceof para distinguir tipos cuando sea necesario.
     * Si no se sobreescribe, no hay reacción (el proyectil no hace nada al impactar).
     *
     * @param bullet el proyectil que colisionó
     * @param other  el objeto con el que colisionó
     */
    public void onCollision(Bullet bullet, GameObjects other) {}
}
