package Game.Weapons.Modifiers;

import Game.Bullets.Bullet;
import Game.Bullets.BulletComport.BulletBehavior;
import Game.Enemys.Enemy;

/**
 * Modificador de veneno — las balas aplican daño continuo (DoT) al impactar.
 *
 * Efecto:
 *   - El enemigo recibe `tickDamage` cada `tickInterval` ticks durante `duration` ticks.
 *   - No modifica WeaponStats (el arma dispara igual).
 *   - Solo wrappea el BulletBehavior para añadir el efecto de veneno on-hit.
 *
 * Uso:
 *   new PoisonModifier()           // 3 daño, cada 20 ticks, por 120 ticks
 *   new PoisonModifier(5, 15, 90)  // configurable
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

    @Override
    public String getId() { return "poison"; }

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return new PoisonBulletWrapper(base, tickDamage, tickInterval, duration);
    }

    // ── Wrapper ───────────────────────────────────────────────────────────

    private static class PoisonBulletWrapper extends BulletBehaviorWrapper {

        private final int dmg;
        private final int interval;
        private final int totalDuration;

        PoisonBulletWrapper(BulletBehavior inner, int dmg, int interval, int totalDuration) {
            super(inner);
            this.dmg           = dmg;
            this.interval      = interval;
            this.totalDuration = totalDuration;
        }

        @Override
        protected void onHitEnemy(Bullet bullet, Enemy enemy) {
            // Aplicar efecto de veneno al enemigo via PoisonEffect
            enemy.addEffect(new PoisonEffect(dmg, interval, totalDuration));
        }
    }

    // ── Efecto de veneno (aplicado al Enemy) ──────────────────────────────

    /**
     * Efecto DoT que se aplica a un Enemy.
     * Se procesa desde Enemy.update() si el sistema de efectos está activo.
     * Compatible con cualquier subclase de Enemy sin modificarlas.
     */
    public static class PoisonEffect {

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

        /**
         * Llamado cada tick desde Enemy.
         * @return true si el efecto sigue activo, false si expiró.
         */
        public boolean tick(Enemy enemy) {
            if (remaining <= 0) return false;
            timer--;
            remaining--;
            if (timer <= 0) {
                enemy.damage(dmgPerTick);
                timer = interval;
            }
            return remaining > 0;
        }

        public boolean isExpired() { return remaining <= 0; }
    }
}
