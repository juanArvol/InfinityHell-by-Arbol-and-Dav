package Game.Items.Types.Ammulets.Effects;

import Game.Engine.AbstractEntity;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.Modifiers.BulletBehaviorWrapper;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper de perforación para amuletos — "Esquirla de Fase".
 *
 * Cada copia del amuleto añade +1 perforación. Con 3 amuletos la bala
 * perfora 3 entidades adicionales (wrappers anidados).
 *
 * ── HRFC-014 — GAP-2: Migrado a onHitEntity(Bullet, AbstractEntity) ──────
 */
public class PiercingAmuletWrapper extends BulletBehaviorWrapper {

    private final int maxPierces;
    private int pierceCount = 0;
    private final Set<Integer> hitIds = new HashSet<>();

    public PiercingAmuletWrapper(BulletBehavior inner, int maxPierces) {
        super(inner);
        this.maxPierces = maxPierces;
    }

    @Override
    protected void onHitEntity(Bullet bullet, AbstractEntity entity) {
        int id = System.identityHashCode(entity);
        if (hitIds.contains(id)) return;

        hitIds.add(id);
        pierceCount++;

        if (pierceCount < maxPierces) {
            bullet.getBulletLife().revive();
        }
    }
}
