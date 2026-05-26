package Game.Weapons.Modifiers;

import Game.Bullets.Bullet;
import Game.Bullets.BulletComport.BulletBehavior;
import Game.Enemys.Enemy;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import Game.World.WorldObjects.WorldObjectsContainer;
import GameMath.Vector2D;

import java.util.List;

/**
 * Modificador explosivo — las balas del arma explotan al impactar.
 *
 * Efecto:
 *   - Al golpear un enemigo, aplica daño en área de radio configurable.
 *   - Reduce levemente el cooldown del arma (las explosiones tardan más).
 *   - Wrappea el BulletBehavior para añadir el splash damage.
 *
 * Uso:
 *   WeaponModifier mod = new ExplosiveModifier(60.0, 1.5); // radio=60, daño x1.5
 *   ModifiedWeapon mw = new ModifiedWeapon(baseWeapon, List.of(mod));
 */
public class ExplosiveModifier extends WeaponModifier {

    private final double blastRadius;
    private final double damageMultiplier;

    /** Radio 60 unidades, daño splash x1.0 */
    public ExplosiveModifier() {
        this(60.0, 1.0);
    }

    public ExplosiveModifier(double blastRadius, double damageMultiplier) {
        this.blastRadius = blastRadius;
        this.damageMultiplier = damageMultiplier;
    }

    @Override
    public String getId() {
        return "explosive";
    }

    @Override
    public void applyToStats(Game.Weapons.WeaponType.WeaponStats stats) {
        // Las explosiones tienen cooldown ligeramente mayor (+5 ticks)
        stats.setCooldown(stats.getCooldown() + 5);
        // Ligero bonus de daño directo
        stats.setDamageBonusByWeapon(stats.getDamageBonusByWeapon() * 1.1);
    }

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return new ExplosiveBulletWrapper(base, blastRadius, damageMultiplier);
    }

    @Override
    public int priority() {
        return 50; // se aplica antes que modificadores de tipo "utility"
    }

    // ── Wrapper del comportamiento de bala ────────────────────────────────

    private static class ExplosiveBulletWrapper extends BulletBehaviorWrapper {

        private final double radius;
        private final double mult;

        ExplosiveBulletWrapper(BulletBehavior inner, double radius, double mult) {
            super(inner);
            this.radius = radius;
            this.mult   = mult;
        }

        @Override
        protected void onHitEnemy(Bullet bullet, Enemy enemy) {
            // Splash damage a todos los enemigos en radio
            applyAreaDamage(bullet.getTransform().getPosition(), bullet.getDamage() * mult);
        }

        @Override
        protected void onHitBlock(Bullet bullet, Game.World.WorldObjects.BlockWorld block) {
            // Explosión en pared también hace splash
            applyAreaDamage(bullet.getTransform().getPosition(), bullet.getDamage() * mult * 0.5);
        }

        private void applyAreaDamage(Vector2D origin, double damage) {
            World world = WorldManager.getInstance().getCurrentWorld();
            if (world == null) return;

            List<Game.Engine.GameObjects> objects =
                world.getObjectsContainer().getObjects();

            for (Game.Engine.GameObjects obj : objects) {
                if (!(obj instanceof Enemy target)) continue;

                Vector2D pos = target.getTransform().getPosition();
                double dx = pos.getX() - origin.getX();
                double dy = pos.getY() - origin.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist <= radius) {
                    // Daño decae con distancia (lineal desde centro)
                    double falloff = 1.0 - (dist / radius);
                    target.damage((int)(damage * falloff));
                }
            }
        }
    }
}
