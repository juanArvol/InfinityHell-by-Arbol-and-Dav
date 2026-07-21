package Game.Items.Types.Weapons.Modifiers;

import Game.Engine.Entity.Components.StatusEffectComponent;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;

/**
 * Modificador de veneno — las balas aplican DoT al impactar.
 *
 * ACTUALIZADO: PoisonEffect ahora implementa StatusEffectComponent.StatusEffect
 * directamente, lo que permite que se registre en el componente sin conversión.
 *
 * Uso:
 *   new PoisonModifier()           // 3 daño, cada 20 ticks, 120 duración
 *   new PoisonModifier(5, 15, 90)
 */
public class PoisonModifier extends WeaponModifier {

    private final int tickDamage;
    private final int tickInterval;
    private final int duration;

    public PoisonModifier() {
        this(3, 20, 120);
    }

    public PoisonModifier(int tickDamage, int tickInterval, int duration) {
        this.tickDamage   = tickDamage;
        this.tickInterval = tickInterval;
        this.duration     = duration;
    }

    @Override public String getId() { return "poison"; }

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return new PoisonBulletWrapper(base, tickDamage, tickInterval, duration);
    }

    // ── Wrapper ───────────────────────────────────────────────────────────

    private static class PoisonBulletWrapper extends BulletBehaviorWrapper {
        private final int dmg, interval, totalDuration;

        PoisonBulletWrapper(BulletBehavior inner, int dmg, int interval, int totalDuration) {
            super(inner);
            this.dmg = dmg; this.interval = interval; this.totalDuration = totalDuration;
        }

        @Override
        protected void onHitEntity(Bullet bullet, Game.Engine.AbstractEntity entity) {
            if (!(entity instanceof Game.Enemys.Core.Enemy enemy)) return;
            StatusEffectComponent fx = enemy.getComponent(StatusEffectComponent.class);
            if (fx != null) {
                fx.add(new PoisonEffect(dmg, interval, totalDuration));
            } else {
                enemy.damage(dmg * (totalDuration / interval));
            }
        }
    }

    // ── Efecto — implementa StatusEffectComponent.StatusEffect ────────────

    public static class PoisonEffect implements StatusEffectComponent.StatusEffect {

        private final int dmgPerTick;
        private final int interval;
        private int remaining;
        private int timer;

        public PoisonEffect(int dmgPerTick, int interval, int duration) {
            this.dmgPerTick = dmgPerTick;
            this.interval   = interval;
            this.remaining  = duration;
            this.timer      = interval;
        }

        @Override
        public boolean tick(GameObjects entity) {
            if (remaining <= 0) return false;
            remaining--;
            timer--;
            if (timer <= 0) {
                // Usar AbstractEntity para ser agnóstico al tipo concreto
                if (entity instanceof Game.Engine.AbstractEntity e) {
                    e.damage(dmgPerTick);
                }
                timer = interval;
            }
            return remaining > 0;
        }

        /* @Override
        public String effectId() { return "poison"; } */
    }
}

