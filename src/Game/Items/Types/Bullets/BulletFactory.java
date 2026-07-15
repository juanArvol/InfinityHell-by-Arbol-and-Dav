package Game.Items.Types.Bullets;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletComport.BulletStats;
import Sprites.Bullets.BulletAssets;

/**
 * Extensión de BulletFactory — añade createBulletWithBehavior() para
 * el sistema de ModifiedWeapon que necesita pasar un behavior ya compuesto.
 *
 * RETRO-COMPATIBLE: no toca los métodos originales. Solo añade uno nuevo.
 *
 * Este archivo REEMPLAZA al BulletFactory.java original, que sigue siendo
 * válido. Si el proyecto tiene BulletFactory en un solo archivo, agrega
 * createBulletWithBehavior() al final del original.
 */
public class BulletFactory {

    // ── Métodos ORIGINALES (no modificados) ───────────────────────────────

    public static Bullet createBullet(
            double startX,
            double startY,
            Vector2D direction,
            BulletType type,
            double weaponBaseSpeed,
            double damage
    ) {
        Vector2D spawn = new Vector2D(startX, startY);
        BulletBehavior comport = type.create();

        double finalSpeed  = weaponBaseSpeed * comport.getSpeedFactor();
        double finalDamage = damage + comport.getBulletBaseDamage();

        double xSpeed = direction.getX() * finalSpeed;
        double ySpeed = direction.getY() * finalSpeed;

        return new Bullet(
            spawn,
            BulletAssets.balaHandle.resolveDefault().getImage(),
            comport,
            xSpeed,
            ySpeed,
            comport.getLifeTime(),
            finalDamage
        );
    }

    public static BulletStats getStats(
            BulletType type,
            double weaponBaseSpeed,
            double weaponDamageBonus
    ) {
        BulletBehavior comport = type.create();
        double finalSpeed  = weaponBaseSpeed * comport.getSpeedFactor();
        double finalDamage = weaponDamageBonus + comport.getBulletBaseDamage();
        return new BulletStats(finalSpeed, finalDamage, comport.getLifeTime(), comport.hasGravity());
    }

    // ── MÉTODO NUEVO para ModifiedWeapon ──────────────────────────────────

    /**
     * Crea una Bullet con un BulletBehavior ya compuesto (pipeline de modifiers).
     *
     * A diferencia de createBullet(), recibe el behavior FINAL en lugar del BulletType,
     * porque ModifiedWeapon ya compuso el wrapper chain externamente.
     *
     * @param startX         posición X de spawn
     * @param startY         posición Y de spawn
     * @param direction      dirección normalizada (puede tener spread aplicado)
     * @param behavior       behavior final compuesto (puede ser un BulletBehaviorWrapper)
     * @param finalSpeed     velocidad total ya calculada (con speedFactor incluido)
     * @param finalDamage    daño total ya calculado (con bonuses incluidos)
     */
    public static Bullet createBulletWithBehavior(
            double startX,
            double startY,
            Vector2D direction,
            BulletBehavior behavior,
            double finalSpeed,
            double finalDamage
    ) {
        Vector2D spawn = new Vector2D(startX, startY);

        double xSpeed = direction.getX() * finalSpeed;
        double ySpeed = direction.getY() * finalSpeed;

        return new Bullet(
            spawn,
            BulletAssets.balaHandle.resolveDefault().getImage(),
            behavior,
            xSpeed,
            ySpeed,
            behavior.getLifeTime(),
            finalDamage
        );
    }
}
