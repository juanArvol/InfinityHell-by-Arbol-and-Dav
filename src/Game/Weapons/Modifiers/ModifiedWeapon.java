package Game.Weapons.Modifiers;

import Game.Bullets.Bullet;
import Game.Bullets.BulletComport.BulletBehavior;
import Game.Bullets.BulletFactory;
import Game.Bullets.BulletType;
import Game.Weapons.WeaponType.WeaponComport;
import Game.Weapons.WeaponType.WeaponStats;
import Game.Weapons.WeaponType.Shoot.ShootResult;
import GameMath.Vector2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Arma con modificadores activos — composición runtime sin herencia.
 *
 * ── CÓMO FUNCIONA ────────────────────────────────────────────────────────
 * ModifiedWeapon envuelve un WeaponComport existente y aplica un pipeline
 * de WeaponModifiers en dos etapas:
 *
 *   1. applyToStats():   los modificadores ajustan una COPIA de WeaponStats
 *                        (cooldown, spread, damage, speed). El original no cambia.
 *
 *   2. wrapBehavior():   los modificadores envuelven el BulletBehavior en cadena,
 *                        añadiendo efectos on-hit (poison, explosive, piercing…).
 *
 * ── NO ROMPE NADA ────────────────────────────────────────────────────────
 * - WeaponComport, WeaponSelected, PlayerCombat funcionan igual.
 * - Solo cambia cómo se construye la bala en tryShoot().
 * - Si no hay modificadores, el comportamiento es idéntico al original.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *   // En PlayerCombat o en un sistema de equipamiento:
 *   WeaponComport base = new WeaponEscopeta();
 *   ModifiedWeapon mw = new ModifiedWeapon(base, BulletType.NORMAL);
 *   mw.addModifier(new ExplosiveModifier());
 *   mw.addModifier(new PoisonModifier());
 *
 *   // Después de esto, cada disparo produce balas explosivas + envenenadas.
 *   List<Bullet> bullets = mw.handleInput(held, pressed, x, y, right, dir);
 */
public class ModifiedWeapon {

    private final WeaponComport comport;
    private final BulletType bulletType;
    private final List<WeaponModifier> modifiers = new ArrayList<>();

    public ModifiedWeapon(WeaponComport comport, BulletType bulletType) {
        this.comport    = comport;
        this.bulletType = bulletType;
    }

    // ── Gestión de modificadores ──────────────────────────────────────────

    public ModifiedWeapon addModifier(WeaponModifier modifier) {
        // Verificar compatibilidad con los ya instalados
        for (WeaponModifier existing : modifiers) {
            if (!existing.isCompatibleWith(modifier) || !modifier.isCompatibleWith(existing)) {
                throw new IllegalArgumentException(
                    "Modificador " + modifier.getId() + " no compatible con " + existing.getId()
                );
            }
        }
        modifiers.add(modifier);
        modifiers.sort(Comparator.comparingInt(WeaponModifier::priority));
        return this;
    }

    public ModifiedWeapon removeModifier(String id) {
        modifiers.removeIf(m -> m.getId().equals(id));
        return this;
    }

    public boolean hasModifier(String id) {
        return modifiers.stream().anyMatch(m -> m.getId().equals(id));
    }

    public List<WeaponModifier> getModifiers() {
        return List.copyOf(modifiers);
    }

    // ── Disparo con modificadores aplicados ───────────────────────────────

    public List<Bullet> handleInput(
            boolean held,
            boolean pressed,
            double x, double y,
            boolean right,
            Vector2D direction) {

        var fireModeResult = comport.getFireMode().handleInput(held, pressed, comport);

        if (!fireModeResult.shouldShoot()) {
            return List.of();
        }

        return tryShoot(x, y, right, direction,
            fireModeResult.getDamageMultiplier(),
            fireModeResult.getSpeedMultiplier());
    }

    private List<Bullet> tryShoot(
            double x, double y,
            boolean right,
            Vector2D direction,
            double damageMult,
            double speedMult) {

        if (comport.getFireWait() > 0 || !comport.canShoot()) {
            return List.of();
        }

        // 1. Copia mutable de stats para que los modificadores la alteren
        WeaponStats effectiveStats = copyStats(comport.getStats());
        for (WeaponModifier mod : modifiers) {
            mod.applyToStats(effectiveStats);
        }

        // 2. Construir bullets con el behavior modificado
        List<Bullet> bullets = new ArrayList<>();
        for (int i = 0; i < effectiveStats.getBulletsPerShot(); i++) {
            Vector2D spreadDir = direction
                .applySpread(direction, effectiveStats.getSpread())
                .normalize();

            // Behavior base desde el BulletType
            BulletBehavior behavior = bulletType.create();

            // Pipeline de wrappers
            for (WeaponModifier mod : modifiers) {
                behavior = mod.wrapBehavior(behavior);
            }

            double finalSpeed  = effectiveStats.getBulletSpeedBase() * speedMult
                                 * behavior.getSpeedFactor();
            double finalDamage = effectiveStats.getDamageBonusByWeapon() * damageMult
                                 + behavior.getBulletBaseDamage();

            // Reutilizar BulletFactory pero con el behavior ya compuesto
            bullets.add(BulletFactory.createBulletWithBehavior(
                x, y, spreadDir, behavior, finalSpeed, finalDamage
            ));
        }

        comport.triggerCooldown();
        comport.incrementBurst();
        comport.consumeAmmo();

        return bullets;
    }

    public void update() {
        comport.update();
    }

    public void reload() {
        comport.startReload();
    }

    public WeaponComport getComport() { return comport; }
    public BulletType getBulletType() { return bulletType; }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Crea una copia mutable de WeaponStats para que los modificadores
     * no toquen el original del WeaponComport.
     */
    private static WeaponStats copyStats(WeaponStats src) {
        return new WeaponStats(
            src.getCooldown(),
            src.getBulletsPerShot(),
            src.getSpread(),
            src.getDamageBonusByWeapon(),
            src.getBulletSpeedBase()
        );
    }
}
