package Game.Items.Types.Weapons;

import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Ammulets.AmuletRegistry;
import Game.Items.Types.Ammulets.PlayerAmulets;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Bullets.BulletType;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Sprites.Source.Sounds;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma equipada con el pipeline de disparo completo.
 *
 * ── HRFC — Refactor Weapon & Projectile System ───────────────────────────
 *
 * ModifiedWeapon es ahora la ÚNICA abstracción de arma equipada.
 * Reemplaza la cadena WeaponSelected → Weapon → WeaponShoot que existía
 * antes. Esa cadena era puro boilerplate de delegación sin valor añadido.
 *
 * ── RESPONSABILIDADES ────────────────────────────────────────────────────
 *
 *   1. Traducir input (held/pressed) a disparo via FireMode.
 *   2. Aplicar los amuletos del jugador sobre una COPIA de WeaponStats
 *      (el original del WeaponComport nunca se muta).
 *   3. Componer el BulletBehavior con los wrappers de amuletos.
 *   4. Delegar la creación de proyectiles a BulletFactory.
 *   5. Gestionar ammo/cooldown/recarga a través de WeaponComport.
 *   6. Emitir el sonido de disparo.
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   - No conoce World ni WorldManager.
 *   - No conoce Player, Enemy ni ninguna entidad concreta.
 *   - No gestiona el ciclo de vida de los proyectiles.
 *
 * ── CÓMO SE USA ────────────────────────────────────────────────────────────
 *
 *   // En Player (constructor o loadout):
 *   ModifiedWeapon weapon = new ModifiedWeapon(
 *       new WeaponEscopeta(),
 *       BulletType.SPRINGBULLET,
 *       player.getAmulets()
 *   );
 *   combat.addWeapon(weapon);
 *
 *   // En PlayerCombat.update():
 *   List<Bullet> bullets = currentWeapon.handleInput(held, pressed, x, y, right, dir);
 *   bullets.forEach(bulletSpawner);
 */
public class ModifiedWeapon {

    private final WeaponComport  comport;
    private final BulletType     bulletType;
    private final PlayerAmulets  amulets;

    public ModifiedWeapon(WeaponComport comport,
                          BulletType bulletType,
                          PlayerAmulets amulets) {
        this.comport    = comport;
        this.bulletType = bulletType;
        this.amulets    = amulets;
    }

    // ── Input ─────────────────────────────────────────────────────────────

    /**
     * Procesa el input del frame y retorna los proyectiles a spawnear.
     * Lista vacía = no disparó este frame.
     *
     * @param held      botón de disparo mantenido pulsado
     * @param pressed   borde de activación (click puntual)
     * @param x         posición X de spawn del proyectil
     * @param y         posición Y de spawn del proyectil
     * @param right     el portador mira a la derecha (para flip de sprite)
     * @param direction dirección normalizada de apuntado
     */
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

        return tryShoot(x, y, direction,
                fireModeResult.getDamageMultiplier(),
                fireModeResult.getSpeedMultiplier());
    }

    // ── Disparo ───────────────────────────────────────────────────────────

    private List<Bullet> tryShoot(
            double x, double y,
            Vector2D direction,
            double damageMult,
            double speedMult) {

        if (comport.getFireWait() > 0 || !comport.canShoot()) {
            return List.of();
        }

        // 1. Copia mutable de stats — el original del comport no se toca
        WeaponStats effectiveStats = copyStats(comport.getStats());

        // 2. Behavior base del tipo de bala equipado
        BulletBehavior behavior = bulletType.create();

        // 3. Aplicar amuletos del jugador (acumulativos):
        //    applyToStats() modifica la copia de WeaponStats
        //    wrapBehavior() envuelve el behavior con efectos on-hit
        behavior = AmuletRegistry.applyAll(amulets.getIds(), effectiveStats, behavior);

        // 4. Crear proyectiles con el behavior compuesto
        List<Bullet> bullets = new ArrayList<>(effectiveStats.getBulletsPerShot());

        for (int i = 0; i < effectiveStats.getBulletsPerShot(); i++) {

            Vector2D spreadDir = direction
                    .applySpread(direction, effectiveStats.getSpread())
                    .normalize();

            double finalSpeed  = effectiveStats.getBulletSpeedBase() * speedMult
                                 * behavior.getDefaultData().speedFactor();
            double finalDamage = effectiveStats.getDamageBonusByWeapon() * damageMult
                                 + behavior.getDefaultData().damage();

            bullets.add(BulletFactory.createBulletWithBehavior(
                    x, y, spreadDir, behavior, finalSpeed, finalDamage));
        }

        // 5. Avanzar estado del arma
        comport.triggerCooldown();
        comport.incrementBurst();
        comport.consumeAmmo();

        // 6. Sonido de disparo
        String sound = comport.getShootSound();
        if (sound != null) {
            Sounds.playSound(sound);
        }

        // 7. Evento de disparo (bus global — suscriptores opcionales)
        if (GameEventBus.GLOBAL.hasListeners(WeaponEvents.OnWeaponFired.class)) {
            GameEventBus.GLOBAL.post(new WeaponEvents.OnWeaponFired(this, bullets.size()));
        }

        return bullets;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Actualiza timers del arma (cooldown, recarga).
     * Llamar una vez por frame desde PlayerCombat.
     */
    public void update() {
        comport.update();
    }

    /**
     * Inicia la recarga manual si el arma no está llena.
     * WeaponComport.startReload() tiene guard interno.
     */
    public void reload() {
        comport.startReload();
    }

    /** Resetea el contador de ráfaga (llamar cuando se suelta el gatillo en BurstMode). */
    public void resetBurst() {
        comport.resetBurst();
    }

    // ── Consultas de estado ───────────────────────────────────────────────

    /** Munición actual en el cargador. */
    public int getCurrentAmmo() { return comport.getCurrentAmmo(); }

    /** Capacidad máxima del cargador. */
    public int getMaxAmmo() { return comport.getChargerSize(); }

    /** True si el arma está recargando actualmente. */
    public boolean isReloading() { return comport.isReloading(); }

    /** True si el cargador está completamente lleno. */
    public boolean isFullyLoaded() { return comport.isFullyLoaded(); }

    /** Cooldown restante entre disparos. */
    public int getFireWait() { return comport.getFireWait(); }

    /** Cooldown total configurado. */
    public int getCooldown() { return comport.getCooldown(); }

    /** Velocidad base de los proyectiles de esta arma. */
    public double getBulletSpeedBase() { return comport.getStats().getBulletSpeedBase(); }

    // ── Acceso a subcomponentes ────────────────────────────────────────────

    public WeaponComport getComport()    { return comport; }
    public BulletType    getBulletType() { return bulletType; }
    public PlayerAmulets getAmulets()    { return amulets; }
    public WeaponStats   getStats()      { return comport.getStats(); }

    // ── Helper ────────────────────────────────────────────────────────────

    private static WeaponStats copyStats(WeaponStats src) {
        return new WeaponStats(
                src.getCooldown(),
                src.getBulletsPerShot(),
                src.getSpread(),
                src.getDamageBonusByWeapon(),
                src.getBulletSpeedBase());
    }
}
