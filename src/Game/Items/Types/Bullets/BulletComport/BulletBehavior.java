package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.ProjectileData;
import Game.Items.Types.Bullets.ProjectileMovement;
import Game.Items.Types.Bullets.Movement.LinearMovement;

/**
 * Comportamiento de un proyectil — qué hace al impactar y cada frame.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   BulletBehavior  → QUÉ HACE al impactar y en cada frame (comportamiento puro)
 *   ProjectileData  → QUÉ VALORES tiene el proyectil (datos inmutables de spawn)
 *   ProjectileMovement → CÓMO SE MUEVE cada frame (estrategia de movimiento)
 *
 * ── CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR ───────────────────────────────
 *
 *   ELIMINADO: 5 métodos @Deprecated (getBulletBaseDamage, getSpeedFactor,
 *              getLifeTime, hasGravity, getGravityValue, método bridge update()).
 *              Eran deuda técnica activa — métodos deprecated en producción
 *              que ningún caller usaba ya. El código que los llamaba fue
 *              migrado a getDefaultData() directamente.
 *
 * ── API ────────────────────────────────────────────────────────────────────
 *
 *   getDefaultData()     — datos de referencia de este tipo de proyectil.
 *                          ProjectileData es un record inmutable.
 *   getDefaultMovement() — estrategia de movimiento por defecto.
 *   onUpdate(Bullet)     — lógica por frame (efectos continuos, etc.).
 *   onCollision(Bullet, GameObjects) — reacción al impacto con cualquier objeto.
 *
 * ── EJEMPLO DE IMPLEMENTACIÓN ────────────────────────────────────────────
 *
 *   public class BulletFire extends BulletBehavior {
 *       private static final ProjectileData DATA =
 *           ProjectileData.flat(20, 1.0, 12);
 *
 *       {@literal @}Override
 *       public ProjectileData getDefaultData() { return DATA; }
 *
 *       {@literal @}Override
 *       public void onCollision(Bullet bullet, GameObjects other) {
 *           if (other instanceof AbstractEntity e) {
 *               e.damage((int) bullet.getDamage());
 *               e.addEffect(new BurningEffect(120));
 *           }
 *           bullet.getBulletLife().kill();
 *       }
 *   }
 */
public abstract class BulletBehavior {

    // ── Datos y movimiento por defecto ────────────────────────────────────

    /**
     * Datos de configuración por defecto de este tipo de proyectil.
     *
     * Sobreescribir en cada BulletBehavior concreto con los valores
     * apropiados para ese tipo. BulletFactory lee estos datos cuando
     * crea proyectiles desde un BulletType.
     *
     * ModifiedWeapon calcula sus propios datos finales (WeaponStats + amuletos)
     * y los pasa directamente a BulletFactory — en ese flujo estos defaults
     * solo se usan para lifeTime, width/height y assetKey.
     *
     * Default base: 10 daño, x1 speed, 10 ticks, sin gravedad, 8×8px.
     * Las subclases deben sobreescribir esto con sus valores reales.
     */
    public ProjectileData getDefaultData() {
        return ProjectileData.flat(10, 1.0, 10);
    }

    /**
     * Estrategia de movimiento por defecto de este tipo de proyectil.
     *
     * Default: LinearMovement.INSTANCE (movimiento recto, velocidad constante).
     * Sobreescribir para tipos con movimiento especial (gravedad, homing, orbital…).
     *
     * Si getDefaultData().gravityValue() != 0, considerar retornar un
     * GravityMovement con ese valor aquí — BulletFactory lo usa automáticamente.
     */
    public ProjectileMovement getDefaultMovement() {
        return LinearMovement.INSTANCE;
    }

    // ── Comportamiento ────────────────────────────────────────────────────

    /**
     * Lógica de actualización por frame del proyectil.
     *
     * Llamado desde Bullet.update() cada frame que el proyectil está vivo,
     * ANTES de que ProjectileMovement.tick() mueva el proyectil.
     *
     * Usar para efectos continuos: rastro de partículas, cambio de color,
     * emisión de luz, timers internos del comportamiento, etc.
     *
     * Default: sin efecto. Sobreescribir solo cuando se necesite.
     */
    public void onUpdate(Bullet bullet) {}

    /**
     * Reacción al contacto con cualquier objeto del mundo.
     *
     * Las subclases usan instanceof para distinguir tipos cuando sea necesario.
     * Si no se sobreescribe, no hay reacción al impactar.
     *
     * Patrones típicos:
     *   if (other instanceof AbstractEntity e) { e.damage(...); }
     *   bullet.getBulletLife().kill();    // destruir el proyectil
     *   bullet.getBulletLife().revive();  // ignorar el impacto (piercing)
     *
     * @param bullet el proyectil que colisionó
     * @param other  el objeto con el que colisionó
     */
    public void onCollision(Bullet bullet, GameObjects other) {}
}
