package Game.Items.Types.Bullets;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletStats;
import Game.Items.Types.Bullets.Movement.GravityMovement;
import Sprites.Entity.Bullets.BulletAssets;
import java.awt.image.BufferedImage;

/**
 * Factory de proyectiles.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   ASSET RESOLUTION por ProjectileData.assetKey():
 *     Si data.assetKey() != null, la factory busca el asset por clave.
 *     Si null, usa el sprite por defecto de BulletAssets.
 *     Cada tipo de proyectil puede tener su propio sprite sin modificar
 *     la factory.
 *
 *   GRAVITY desde ProjectileData.gravityValue():
 *     Si data.gravityValue() != 0, la factory compone el movement del
 *     behavior con un GravityMovement. Esto garantiza que los behaviors
 *     que declaran gravityValue > 0 en ProjectileData la tengan activa,
 *     incluso si getDefaultMovement() retorna LinearMovement.
 *
 *     Si getDefaultMovement() ya incluye GravityMovement (como BulletJump),
 *     no se añade doble gravedad — BulletJump declara su propio
 *     getDefaultMovement() que retorna GravityMovement y NO declara
 *     gravityValue en ProjectileData (usa 0.0).
 *
 *     Regla: usar gravityValue en ProjectileData solo si getDefaultMovement()
 *     no incluye ya una GravityMovement. Si el behavior sobreescribe
 *     getDefaultMovement() con GravityMovement, poner gravityValue=0.0.
 *
 * ── MÉTODOS ───────────────────────────────────────────────────────────────
 *
 *   createBullet(...)             — ruta directa desde BulletType (sin amuletos)
 *   createBulletWithBehavior(...) — ruta desde ModifiedWeapon (con amuletos)
 *   getStats(...)                 — preview de stats sin crear Bullet (para UI)
 */
public class BulletFactory {

    // ── Creación desde BulletType ─────────────────────────────────────────

    /**
     * Crea un proyectil desde un BulletType con sus valores por defecto.
     * ModifiedWeapon NO usa este método — tiene su propio pipeline con amuletos.
     */
    public static Bullet createBullet(
            double     startX,
            double     startY,
            Vector2D   direction,
            BulletType type,
            double     weaponBaseSpeed,
            double     damage
    ) {
        BulletBehavior behavior = type.create();
        ProjectileData data     = behavior.getDefaultData();

        double finalSpeed  = weaponBaseSpeed * data.speedFactor();
        double finalDamage = damage + data.damage();

        return build(startX, startY, direction, behavior, finalSpeed, finalDamage, data);
    }

    // ── Creación desde ModifiedWeapon (valores finales ya calculados) ─────

    /**
     * Crea un proyectil con behavior compuesto y valores finales calculados.
     * Usado por ModifiedWeapon después de aplicar amuletos sobre WeaponStats.
     */
    public static Bullet createBulletWithBehavior(
            double         startX,
            double         startY,
            Vector2D       direction,
            BulletBehavior behavior,
            double         finalSpeed,
            double         finalDamage
    ) {
        ProjectileData data = behavior.getDefaultData();
        return build(startX, startY, direction, behavior, finalSpeed, finalDamage, data);
    }

    // ── Stats para UI (sin instanciar Bullet) ────────────────────────────

    /**
     * Calcula los BulletStats sin crear un Bullet.
     * Útil para tooltips y previews en CrossHairHUD.
     */
    public static BulletStats getStats(
            BulletType type,
            double weaponBaseSpeed,
            double weaponDamageBonus
    ) {
        BulletBehavior behavior = type.create();
        ProjectileData data     = behavior.getDefaultData();
        double finalSpeed  = weaponBaseSpeed * data.speedFactor();
        double finalDamage = weaponDamageBonus + data.damage();
        return new BulletStats(finalSpeed, finalDamage, data.lifeTime(), data.hasGravity());
    }

    // ── Builder interno ───────────────────────────────────────────────────

    private static Bullet build(
            double         startX,
            double         startY,
            Vector2D       direction,
            BulletBehavior behavior,
            double         speed,
            double         damage,
            ProjectileData data
    ) {
        Vector2D spawn  = new Vector2D(startX, startY);
        double   xSpeed = direction.getX() * speed;
        double   ySpeed = direction.getY() * speed;

        // ── Resolver movimiento ───────────────────────────────────────────
        ProjectileMovement movement = behavior.getDefaultMovement();
        if (data.hasGravity() && movement.isStateless()) {
            movement = movement.andThen(new GravityMovement(data.gravityValue()));
        }

        // ── Resolver sprite ───────────────────────────────────────────────
        // BulletAssets.balaHandle es inicializado por Assets.init() en GameOrquester.
        // Si assetKey está definido en ProjectileData se usará en el futuro
        // para sprites específicos por tipo — por ahora todos usan el default.
        BufferedImage texture = BulletAssets.balaHandle.resolveDefault().getImage();

        return new Bullet(spawn, texture, behavior, movement,
                          xSpeed, ySpeed, data.lifeTime(), damage,
                          data.width(), data.height());
    }
}
