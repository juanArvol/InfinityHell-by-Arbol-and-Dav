package Game.Items.Types.Bullets.BulletComport.BulletClass;

import Game.Engine.AbstractEntity;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;

/**
 * Proyectil con rebote en el suelo — impacta entidades y rebota en objetos del mundo.
 *
 * ── HRFC-014 — GAP-2: Migrado a onCollision(Bullet, GameObjects) ─────────
 */
public class BulletJump extends BulletBehavior {

    private final double jumpBoost = -10;

    public BulletJump() {
        super(5, 0.9, true, 1, 20);
    }

    @Override
    public void update(Bullet bullet) {}

    @Override
    public void onCollision(Bullet bullet, GameObjects other) {
        if (other instanceof AbstractEntity) {
            // Impacto con entidad viva — destruir bala
            bullet.getBulletLife().setDead();
        } else {
            // Impacto con objeto del mundo — rebotar si va hacia abajo
            if (bullet.getPhysics().getYspeed() > 0) {
                bullet.getPhysics().setYspeed(jumpBoost);
                bullet.getBulletLife().reset(1);
            }
            bullet.getPhysics().setXspeed(bullet.getPhysics().getXspeed() / 1.01);
        }
    }
}
