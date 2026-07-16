package Game.Items.Types.Weapons.Modifiers;

import Game.Enemys.Core.Enemy;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import java.util.HashSet;
import java.util.Set;

/**
 * Modificador de perforación — la bala atraviesa hasta N enemigos.
 *
 * Efecto:
 *   - La bala NO se destruye al golpear un enemigo (hasta maxPierces veces).
 *   - Cada enemigo solo recibe daño una vez por bala (no se hace spam).
 *   - Al superar el límite de perforaciones, la bala muere normalmente.
 *
 * Uso:
 *   new PiercingModifier()   // 3 perforaciones
 *   new PiercingModifier(5)  // 5 perforaciones
 */
public class PiercingModifier extends WeaponModifier {

    private final int maxPierces;

    public PiercingModifier() {
        this(3);
    }

    public PiercingModifier(int maxPierces) {
        this.maxPierces = maxPierces;
    }

    @Override
    public String getId() { return "piercing"; }

    @Override
    public void applyToStats(Game.Items.Types.Weapons.WeaponType.WeaponStats stats) {
        // Ligera reducción de daño directo para compensar el piercing
        stats.setDamageBonusByWeapon(stats.getDamageBonusByWeapon() * 0.85);
    }

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return new PiercingBulletWrapper(base, maxPierces);
    }

    // ── Wrapper ───────────────────────────────────────────────────────────

    private static class PiercingBulletWrapper extends BulletBehaviorWrapper {

        private final int max;
        private int pierceCount = 0;
        private final Set<Integer> hitIds = new HashSet<>(); // evita multi-hit por frame

        PiercingBulletWrapper(BulletBehavior inner, int max) {
            super(inner);
            this.max = max;
        }

        @Override
        protected void onHitEnemy(Bullet bullet, Enemy enemy) {
            int id = System.identityHashCode(enemy);
            if (hitIds.contains(id)) return; // ya golpeado

            hitIds.add(id);
            pierceCount++;

            // NO llamamos bullet.getBulletLife().setDead() — la bala sigue viva
            // mientras no supere el límite. El inner (BulletNormal etc.) puede
            // haber llamado setDead() — lo revertimos si todavía queda pierce.
            if (pierceCount < max) {
                // Reabrir la bala (cancelar la muerte del inner)
                bullet.getBulletLife().revive();
            }
            // Si pierceCount >= max, la bala ya se murió normalmente via inner
        }
    }
}
