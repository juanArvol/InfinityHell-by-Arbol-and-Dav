package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.ProjectileData;

/**
 * Proyectil estándar — daña a cualquier AbstractEntity y luego muere.
 *
 * El tipo de proyectil más simple: impacto → daño → destrucción.
 * Sirve como base para la mayoría de armas sin efecto especial.
 *
 * El daño real del proyectil NO se lee de getDefaultData().damage() en
 * gameplay — ModifiedWeapon calcula el daño final (WeaponStats + amuletos)
 * y lo pasa a BulletFactory. getDefaultData() define los valores de referencia
 * usados cuando se crea el proyectil sin pasar por ModifiedWeapon.
 */
public class BulletNormal extends BulletBehavior {

    private static final ProjectileData DEFAULT_DATA =
            ProjectileData.flat(15, 1.0, 10);
    @Override
    public ProjectileData getDefaultData() {
        return DEFAULT_DATA;
    }

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        // Dañar a cualquier entidad con vida
        if (other instanceof AbstractEntity entity) {
            entity.damage((int) bullet.getDamage());
        }
        // Morir al impactar contra cualquier cosa
        bullet.getBulletLife().kill();
    }
}
