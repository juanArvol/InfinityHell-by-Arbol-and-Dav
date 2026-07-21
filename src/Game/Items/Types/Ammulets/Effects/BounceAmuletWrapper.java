package Game.Items.Types.Ammulets.Effects;

import Game.Engine.AbstractEntity;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.Modifiers.BulletBehaviorWrapper;

/**
 * Wrapper de rebote para amuletos — "Piedra del Eco".
 *
 * Al impactar una entidad, busca la entidad más cercana dentro de un radio
 * y redirige la bala hacia ella. Cada copia del amuleto añade +1 salto.
 *
 * ── HRFC-014 — GAP-2: Migrado a onHitEntity(Bullet, AbstractEntity) ──────
 */
public class BounceAmuletWrapper extends BulletBehaviorWrapper {

    private final int maxBounces;
    private int bounceCount = 0;

    public BounceAmuletWrapper(BulletBehavior inner, int maxBounces) {
        super(inner);
        this.maxBounces = maxBounces;
    }

    @Override
    protected void onHitEntity(Bullet bullet, AbstractEntity entity) {
        if (bounceCount >= maxBounces) return;
        bounceCount++;

        // TODO: buscar entidad más cercana distinta a la actual y redirigir la bala.
        // Ejemplo:
        //   AbstractEntity target = world.findNearest(bullet.getPosition(), entity, RADIUS);
        //   if (target != null) {
        //       Vector2D dir = target.getCenter().subtract(bullet.getPosition()).normalize();
        //       bullet.getPhysics().setXspeed(dir.getX() * speed);
        //       bullet.getPhysics().setYspeed(dir.getY() * speed);
        //       bullet.getBulletLife().revive();
        //   }
    }
}
