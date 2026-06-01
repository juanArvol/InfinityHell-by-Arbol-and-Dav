package Game.Items.Types.Weapons.Modifiers;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Types.Ammulets.AmuletRegistry;
import Game.Items.Types.Ammulets.PlayerAmulets;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Bullets.BulletType;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Game.Items.Types.Weapons.WeaponType.Shoot.ShootResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma con efectos de amuletos aplicados — composición runtime sin herencia.
 *
 * ── CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR ───────────────────────────────
 * La versión anterior tenía una lista de WeaponModifiers para propiedades
 * como piercing, explosive, poison. Esos modificadores eran únicos (no
 * repetibles) y tenían deduplicación hardcodeada.
 *
 * Ahora esas propiedades son AMULETOS (PlayerAmulets), que:
 *   • Son acumulables (múltiples copias suman su efecto)
 *   • Se gestionan externamente por el jugador
 *   • Aparecen de forma aleatoria e infinita en el loot
 *
 * ModifiedWeapon ya no gestiona una lista de modificadores: recibe la
 * referencia a PlayerAmulets y delega en AmuletRegistry.applyAll().
 *
 * ── LO QUE NO CAMBIA ─────────────────────────────────────────────────────
 * - La firma de handleInput() es idéntica.
 * - WeaponComport, WeaponInventory, PlayerCombat funcionan igual.
 * - BulletFactory.createBulletWithBehavior() no se toca.
 *
 * ── USO ──────────────────────────────────────────────────────────────────
 *   // En PlayerCombat, al crear el arma equipada:
 *   ModifiedWeapon weapon = new ModifiedWeapon(
 *       comport, player.getEquippedBulletType(), player.getAmulets()
 *   );
 *   List<Bullet> bullets = weapon.handleInput(held, pressed, x, y, right, dir);
 */
public class ModifiedWeapon {

    private final WeaponComport comport;
    private final BulletType bulletType;
    private final PlayerAmulets amulets;

    public ModifiedWeapon(WeaponComport comport,
                          BulletType bulletType,
                          PlayerAmulets amulets) {
        this.comport    = comport;
        this.bulletType = bulletType;
        this.amulets    = amulets;
    }

    // ── Disparo ───────────────────────────────────────────────────────────

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

        // 1. Copia mutable de stats para que los amuletos la alteren
        WeaponStats effectiveStats = copyStats(comport.getStats());

        // 2. Behavior base desde el BulletType
        BulletBehavior behavior = bulletType.create();

        // 3. Aplicar todos los amuletos del jugador (acumulativos):
        //    - applyToStats() modifica la copia de WeaponStats
        //    - wrapBehavior() envuelve el behavior con efectos on-hit
        behavior = AmuletRegistry.applyAll(amulets.getIds(), effectiveStats, behavior);

        // 4. Construir balas con el behavior compuesto
        List<Bullet> bullets = new ArrayList<>();
        for (int i = 0; i < effectiveStats.getBulletsPerShot(); i++) {

            Vector2D spreadDir = direction
                .applySpread(direction, effectiveStats.getSpread())
                .normalize();

            double finalSpeed  = effectiveStats.getBulletSpeedBase() * speedMult
                                 * behavior.getSpeedFactor();
            double finalDamage = effectiveStats.getDamageBonusByWeapon() * damageMult
                                 + behavior.getBulletBaseDamage();

            bullets.add(BulletFactory.createBulletWithBehavior(
                x, y, spreadDir, behavior, finalSpeed, finalDamage
            ));
        }

        comport.triggerCooldown();
        comport.incrementBurst();
        comport.consumeAmmo();

        return bullets;
    }

    // ── Delegados ─────────────────────────────────────────────────────────

    public void update() { comport.update(); }
    public void reload()  { comport.startReload(); }
    public void resetBurst() { comport.resetBurst(); }

    public WeaponComport getComport()  { return comport; }
    public BulletType    getBulletType() { return bulletType; }
    public PlayerAmulets getAmulets()   { return amulets; }

    // ── Helper ────────────────────────────────────────────────────────────

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
