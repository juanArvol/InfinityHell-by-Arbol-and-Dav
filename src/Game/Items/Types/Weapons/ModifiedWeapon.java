package Game.Items.Types.Weapons;

import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Ammulets.AmuletRegistry;
import Game.Items.Types.Ammulets.PlayerAmulets;
import Game.Items.Types.Bullets.BulletComport.BulletBehavior;
import Game.Items.Types.Bullets.BulletFactory;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Bullets.Definition.ProjectilePool;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Sprites.Source.Sounds;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma equipada con el pipeline de disparo completo.
 *
 * ── PIPELINE DE DISPARO ───────────────────────────────────────────────────
 *
 *   1. FireMode.handleInput()            → ¿dispara este frame?
 *   2. copyStats(comport.getStats())     → copia mutable de WeaponStats
 *   3. bulletType.create()               → BulletBehavior base
 *   4. AmuletRegistry.applyAll()         → modifica stats + envuelve behavior
 *   5. ProjectileBlueprint.from()        → definición resuelta del proyectil
 *   6. pool.acquire() / BulletFactory.build() → instancia Bullet
 *
 * ── POOL OPCIONAL ─────────────────────────────────────────────────────────
 *
 * ModifiedWeapon acepta un ProjectilePool opcional en construcción.
 *
 * Si se provee un pool:
 *   → bullets.acquire(blueprint, position, direction, owner)
 *   → El pool decide si reutiliza una instancia o crea una nueva via la Factory.
 *   → Esto es el camino recomendado para proyectiles del jugador en combate.
 *
 * Si no se provee pool (pool == null):
 *   → BulletFactory.build(blueprint, position, direction, owner)
 *   → Camino directo, sin pooling. Útil en tests, cutscenes, o sistemas
 *     donde el lifecycle del proyectil no justifica pooling.
 *
 * Ambos caminos usan el mismo Blueprint y producen Bullets idénticas en
 * comportamiento. La única diferencia es si se reutilizan instancias.
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   - No conoce World ni WorldManager.
 *   - No conoce Player, Enemy ni ninguna entidad concreta.
 *   - No gestiona el ciclo de vida de los proyectiles.
 *   - No llama setCollisionProfile() manualmente.
 */
public class ModifiedWeapon {

    private final WeaponComport  comport;
    private final BulletType     bulletType;
    private final PlayerAmulets  amulets;

    /**
     * Pool de proyectiles para reutilización de instancias.
     *
     * null = sin pooling — usa BulletFactory directamente.
     * Inyectado en construcción por quien crea el arma (Player, Turret, etc.).
     */
    private final ProjectilePool pool;

    /**
     * Propietario de esta arma. Se propaga al evento OnProjectileSpawn.
     */
    private final Object owner;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor completo con pool y owner explícitos.
     *
     * @param comport    comportamiento del arma (cadencia, cooldown, munición)
     * @param bulletType tipo de proyectil base
     * @param amulets    amuletos del portador del arma
     * @param pool       pool de proyectiles para reutilización (null = sin pooling)
     * @param owner      el objeto portador del arma (Player, Turret, etc.), o null
     */
    public ModifiedWeapon(WeaponComport comport,
                          BulletType bulletType,
                          PlayerAmulets amulets,
                          ProjectilePool pool,
                          Object owner) {
        this.comport    = comport;
        this.bulletType = bulletType;
        this.amulets    = amulets;
        this.pool       = pool;
        this.owner      = owner;
    }

    /**
     * Constructor con owner sin pool — sin reutilización de instancias.
     */
    public ModifiedWeapon(WeaponComport comport,
                          BulletType bulletType,
                          PlayerAmulets amulets,
                          Object owner) {
        this(comport, bulletType, amulets, null, owner);
    }

    /**
     * Constructor sin owner ni pool — compatibilidad con código existente.
     */
    public ModifiedWeapon(WeaponComport comport,
                          BulletType bulletType,
                          PlayerAmulets amulets) {
        this(comport, bulletType, amulets, null, null);
    }

    // ── Input ─────────────────────────────────────────────────────────────

    /**
     * Procesa el input del frame y retorna los proyectiles a spawnear.
     * Lista vacía = no disparó este frame.
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

        // 3. Aplicar amuletos del jugador
        behavior = AmuletRegistry.applyAll(amulets.getIds(), effectiveStats, behavior);

        // 4. Construir proyectiles con el pipeline Blueprint → Pool/Factory
        List<Bullet> bullets = new ArrayList<>(effectiveStats.getBulletsPerShot());

        for (int i = 0; i < effectiveStats.getBulletsPerShot(); i++) {

            Vector2D spreadDir = direction
                    .applySpread(direction, effectiveStats.getSpread())
                    .normalize();

            double finalSpeed  = effectiveStats.getBulletSpeedBase() * speedMult
                                 * behavior.getDefaultData().speedFactor();
            double finalDamage = effectiveStats.getDamageBonusByWeapon() * damageMult
                                 + behavior.getDefaultData().damage();

            ProjectileBlueprint blueprint = ProjectileBlueprint.from(
                    behavior, finalSpeed, finalDamage);

            // ── Adquisición unificada ─────────────────────────────────────
            // Si hay pool: acquire() → reutiliza o delega a BulletFactory.
            // Si no hay pool: BulletFactory.build() → instancia directa.
            // Ambos caminos usan el mismo Blueprint y producen Bullets idénticas.
            Bullet bullet;
            if (pool != null) {
                bullet = pool.acquire(blueprint, new Vector2D(x, y), spreadDir, owner);
            } else {
                bullet = BulletFactory.build(blueprint, new Vector2D(x, y), spreadDir, owner);
            }
            bullets.add(bullet);
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

        // 7. Evento de disparo
        if (GameEventBus.GLOBAL.hasListeners(WeaponEvents.OnWeaponFired.class)) {
            GameEventBus.GLOBAL.post(new WeaponEvents.OnWeaponFired(this, bullets.size()));
        }

        return bullets;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    public void update() { comport.update(); }

    public void reload() { comport.startReload(); }

    public void resetBurst() { comport.resetBurst(); }

    // ── Consultas de estado ───────────────────────────────────────────────

    public int     getCurrentAmmo()     { return comport.getCurrentAmmo();   }
    public int     getMaxAmmo()         { return comport.getChargerSize();   }
    public boolean isReloading()        { return comport.isReloading();      }
    public boolean isFullyLoaded()      { return comport.isFullyLoaded();    }
    public int     getFireWait()        { return comport.getFireWait();      }
    public int     getCooldown()        { return comport.getCooldown();      }
    public double  getBulletSpeedBase() { return comport.getStats().getBulletSpeedBase(); }

    // ── Acceso a subcomponentes ────────────────────────────────────────────

    public WeaponComport getComport()    { return comport;    }
    public BulletType    getBulletType() { return bulletType; }
    public PlayerAmulets getAmulets()    { return amulets;    }
    public WeaponStats   getStats()      { return comport.getStats(); }
    public Object        getOwner()      { return owner; }
    public ProjectilePool getPool()      { return pool; }

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
