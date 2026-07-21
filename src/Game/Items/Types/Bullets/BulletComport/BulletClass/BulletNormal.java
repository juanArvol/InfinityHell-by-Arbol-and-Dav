package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;

/**
 * Proyectil estándar — daña a cualquier AbstractEntity y luego muere.
 *
 * ── HRFC-014 — GAP-2: usa onCollision(Bullet, GameObjects) ─────────────
 * Ya no distingue Player vs Enemy con sobrecargas tipadas.
 * Daña a cualquier AbstractEntity que tenga HealthComponent,
 * sin que BulletNormal importe Player ni Enemy.
 */
public class BulletNormal extends BulletBehavior {

    public BulletNormal() {
        super(15, 1.0, false, 0, 10);
    }

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        // Dañar a cualquier entidad que tenga vida
        if (other instanceof AbstractEntity entity) {
            entity.damage((int) bullet.getDamage());
        }
        // Morir al impactar contra cualquier cosa sólida
        bullet.getBulletLife().setDead();
    }
}
