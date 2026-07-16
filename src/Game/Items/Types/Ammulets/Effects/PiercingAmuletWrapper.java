package Game.Items.Types.Ammulets.Effects;

import Game.Enemys.Core.Enemy;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.Modifiers.BulletBehaviorWrapper;

import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper de perforación para amuletos — cada copia de "Esquirla de Fase"
 * añade un nivel de perforación acumulativo.
 *
 * ── DIFERENCIA CON PiercingModifier ──────────────────────────────────────
 * PiercingModifier era único (no podía stackear porque era un WeaponModifier
 * con ID deduplicado). Este wrapper es instanciado POR CADA COPIA del amuleto,
 * y cada uno añade +1 perforación de forma independiente.
 *
 * Con 1 amuleto:  la bala perfora 1 enemigo adicional
 * Con 3 amuletos: la bala perfora 3 enemigos adicionales (wrappers anidados)
 *
 * ── CÓMO FUNCIONA EL ANIDAMIENTO ─────────────────────────────────────────
 * AmuletRegistry.applyAll() llama wrapBehavior() una vez por copia:
 *   base → Wrapper(1) → Wrapper(1) → Wrapper(1)
 *
 * Cada wrapper revive la bala una vez. La bala muere cuando todos los
 * wrappers han agotado su cupo de perforación.
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
    protected void onHitEnemy(Bullet bullet, Enemy enemy) {
        int id = System.identityHashCode(enemy);
        if (hitIds.contains(id)) return;

        hitIds.add(id);
        pierceCount++;

        if (pierceCount < maxPierces) {
            bullet.getBulletLife().revive();
        }
    }
}
