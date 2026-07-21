package Game.Items.Types.Weapons.Modifiers;

import Game.Enemys.Core.Enemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import java.util.List;
import java.util.function.Supplier;

/**
 * Modificador explosivo — las balas del arma explotan al impactar.
 *
 * Efecto:
 *   - Al golpear un enemigo o un bloque, aplica daño en área de radio configurable.
 *   - El daño cae linealmente desde el centro hacia el borde del radio.
 *   - Reduce levemente el cooldown del arma (las explosiones tardan más).
 *
 * ── DESACOPLAMIENTO DE WorldManager ──────────────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   La implementación anterior llamaba directamente a:
 *     WorldManager.getInstance().getCurrentWorld().getObjectsContainer().getObjects()
 *
 *   Esto creaba tres problemas:
 *   (a) ExplosiveModifier dependía de WorldManager — un singleton de ciclo de
 *       vida complejo. El modificador solo necesita "una lista de objetos en
 *       el área de explosión". No necesita saber nada de World ni WorldManager.
 *   (b) Imposible testear la explosión sin un World real.
 *   (c) Acoplamiento idéntico al que fue eliminado en PlayerCombat → bulletSpawner.
 *       Es el mismo antipatrón que ya se identificó y resolvió en otro refactor.
 *
 * SOLUCIÓN:
 *   Inyectar un Supplier<List<Enemy>> en el constructor — el proveedor de
 *   enemigos que el sistema de mundo entrega en el momento de construir el arma.
 *   ExplosiveModifier no conoce World, WorldManager ni ningún contenedor.
 *   Solo sabe "cuando exploto, aquí está la lista de targets".
 *
 *   Ejemplo de construcción desacoplada:
 *     new ExplosiveModifier(60.0, 1.5, world::getEnemies)
 *     new ExplosiveModifier(60.0, 1.5, () -> combatContext.getNearbyEnemies(origin, radius))
 *
 *   Si el sistema de mundo no puede proveer ese Supplier en el momento de
 *   construcción, se puede pasar al nivel del ModifiedWeapon en lugar del
 *   modificador — véase el comentario en wrapBehavior().
 *
 * Uso:
 *   WeaponModifier mod = new ExplosiveModifier(60.0, 1.5, world::getEnemies);
 *   ModifiedWeapon mw = new ModifiedWeapon(baseComport, bulletType, amulets);
 */
public class ExplosiveModifier extends WeaponModifier {

    private final double blastRadius;
    private final double damageMultiplier;

    /**
     * Proveedor de la lista de enemigos activos en el mundo.
     * Inyectado en construcción para evitar el singleton WorldManager.
     * Lazy por diseño — solo se llama al momento de la explosión.
     */
    private final Supplier<List<Enemy>> enemyProvider;

    /** Radio 60 unidades, daño splash x1.0, sin proveedor (no hace splash). */
    public ExplosiveModifier() {
        this(60.0, 1.0, List::of);
    }

    public ExplosiveModifier(double blastRadius, double damageMultiplier,
                             Supplier<List<Enemy>> enemyProvider) {
        this.blastRadius      = blastRadius;
        this.damageMultiplier = damageMultiplier;
        this.enemyProvider    = enemyProvider;
    }

    @Override
    public String getId() {
        return "explosive";
    }

    @Override
    public void applyToStats(Game.Items.Types.Weapons.WeaponType.WeaponStats stats) {
        // Las explosiones tienen cooldown ligeramente mayor (+5 ticks)
        stats.setCooldown(stats.getCooldown() + 5);
        // Ligero bonus de daño directo
        stats.setDamageBonusByWeapon(stats.getDamageBonusByWeapon() * 1.1);
    }

    @Override
    public BulletBehavior wrapBehavior(BulletBehavior base) {
        return new ExplosiveBulletWrapper(base, blastRadius, damageMultiplier, enemyProvider);
    }

    @Override
    public int priority() {
        return 50;
    }

    // ── Wrapper del comportamiento de bala ────────────────────────────────

    private static class ExplosiveBulletWrapper extends BulletBehaviorWrapper {

        private final double radius;
        private final double mult;
        private final Supplier<List<Enemy>> enemyProvider;

        ExplosiveBulletWrapper(BulletBehavior inner, double radius, double mult,
                               Supplier<List<Enemy>> enemyProvider) {
            super(inner);
            this.radius        = radius;
            this.mult          = mult;
            this.enemyProvider = enemyProvider;
        }

        @Override
        protected void onHitEntity(Bullet bullet, Game.Engine.AbstractEntity entity) {
            applyAreaDamage(bullet.getTransform().getPosition(), bullet.getDamage() * mult);
        }

        @Override
        protected void onHitWorld(Bullet bullet, GameObjects other) {
            // Explosión en objeto del mundo hace splash con daño reducido
            applyAreaDamage(bullet.getTransform().getPosition(), bullet.getDamage() * mult * 0.5);
        }

        private void applyAreaDamage(Vector2D origin, double damage) {
            // Obtener enemigos desde el proveedor inyectado — sin WorldManager
            List<Enemy> targets = enemyProvider.get();
            if (targets == null || targets.isEmpty()) return;

            for (Enemy target : targets) {
                Vector2D pos = target.getTransform().getPosition();
                double dx   = pos.getX() - origin.getX();
                double dy   = pos.getY() - origin.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist <= radius) {
                    // Daño cae linealmente desde el centro hacia el borde
                    double falloff = 1.0 - (dist / radius);
                    target.damage((int)(damage * falloff));
                }
            }
        }
    }
}
