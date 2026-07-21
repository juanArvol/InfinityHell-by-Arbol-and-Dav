package Game.Items.Types.Weapons.Modifiers;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;

/**
 * Decorator base para BulletBehavior.
 *
 * Permite apilar comportamientos de bala sin herencia múltiple.
 * Cada wrapper delega al inner behavior y añade su propio efecto.
 *
 * ── HRFC-014 — GAP-2: Migración a API genérica ────────────────────────────
 *
 * ANTES:
 *   Los wrappers sobreescribían sobrecargas tipadas concretas:
 *     onCollision(Bullet, Enemy)
 *     onCollision(Bullet, Player)
 *     onCollision(Bullet, BlockWorld)
 *     onCollision(Bullet, Obstacle)
 *     onCollision(Bullet, BackGround)
 *
 *   Eso acoplaba el sistema de decorators a tipos del Game directamente
 *   en el nivel base, y requería una sobrecarga por cada tipo nuevo.
 *
 * AHORA:
 *   Un único método genérico onCollision(Bullet, GameObjects), del que
 *   derivan dos hooks semánticos:
 *
 *   onHitEntity(Bullet, AbstractEntity) — impacto con cualquier entidad viva.
 *   onHitWorld(Bullet, GameObjects)     — impacto con cualquier objeto del mundo.
 *
 *   Los wrappers concretos que necesitan distinguir entre tipos de entidad
 *   (p.ej. "solo aplicar veneno a Enemy, no a Player") hacen instanceof
 *   dentro de onHitEntity(). Los que necesitan distinguir objetos del mundo
 *   hacen instanceof dentro de onHitWorld().
 *
 *   El Engine no conoce ningún tipo concreto. La distinción es responsabilidad
 *   del Gameplay que implementa el wrapper.
 *
 * Uso típico:
 *   BulletBehavior base   = new BulletNormal();
 *   BulletBehavior poison = new PoisonBulletWrapper(base);
 *   BulletBehavior pierce = new PiercingBulletWrapper(poison, 3);
 *   // pierce aplica normal + poison + piercing en cadena
 */
public abstract class BulletBehaviorWrapper extends BulletBehavior {

    protected final BulletBehavior inner;

    protected BulletBehaviorWrapper(BulletBehavior inner) {
        super(
            inner.getBulletBaseDamage(),
            inner.getSpeedFactor(),
            inner.hasGravity(),
            inner.getGravityValue(),
            inner.getLifeTime()
        );
        this.inner = inner;
    }

    // ── Delegación base ───────────────────────────────────────────────────

    @Override
    public void update(Bullet bullet) {
        inner.update(bullet);
        onUpdate(bullet);
    }

    /**
     * Delega al inner y luego llama el hook semántico correcto.
     *
     * onHitEntity → para cualquier objeto que sea AbstractEntity (tiene vida)
     * onHitWorld  → para cualquier otro objeto del mundo (bloques, etc.)
     */
    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        inner.onCollision(bullet, other);

        if (other instanceof AbstractEntity entity) {
            onHitEntity(bullet, entity);
        } else {
            onHitWorld(bullet, other);
        }
    }

    // ── Hooks para subclases ──────────────────────────────────────────────
    // Override solo lo que se necesita. Por defecto no hacen nada.

    /** Llamado cada frame. Override para lógica continua del wrapper. */
    protected void onUpdate(Bullet bullet) {}

    /**
     * Llamado al impactar con cualquier entidad viva (Player, Enemy, NPC…).
     * Hacer instanceof dentro si se necesita distinguir tipos concretos:
     *   if (entity instanceof Enemy e) { ... }
     *   if (entity instanceof Player p) { ... }
     */
    protected void onHitEntity(Bullet bullet, AbstractEntity entity) {}

    /**
     * Llamado al impactar con cualquier objeto del mundo que no es entidad
     * (BlockWorld, Obstacle, BackGround…).
     * Hacer instanceof dentro si se necesita distinguir tipos concretos.
     */
    protected void onHitWorld(Bullet bullet, GameObjects other) {}
}
