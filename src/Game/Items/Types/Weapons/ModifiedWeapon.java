package Game.Items.Types.Weapons;

import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Gameplay.Events.WeaponEvents;
import Game.Items.Types.Ammulets.AmuletInventory;
import Game.Items.Types.Bullets.BulletComport.BulletStats;
import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.Definition.BulletFactory;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Bullets.Definition.ProjectilePool;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileResolver;
import Game.Items.Types.Weapons.WeaponType.FireMode.FireModeResolution;
import Game.Items.Types.Weapons.WeaponType.WeaponComport;
import Game.Items.Types.Weapons.WeaponType.WeaponStats;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
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
 *   6. pool.acquire()                    → instancia Bullet con ProjectileContext
 *
 * ── HRFC — ProjectilePool Integration Consolidation ──────────────────────
 *
 * ProjectilePool es ahora OBLIGATORIO para ModifiedWeapon. El fallback legacy
 * a BulletFactory.build() ha sido eliminado.
 *
 * RAZONES:
 *   1. BulletFactory.build() NO asigna ProjectileContext
 *   2. Proyectiles sin contexto no pueden acceder a capacidades (SpatialQuery, etc.)
 *   3. No existían casos de uso legítimos para armas sin pool en gameplay
 *   4. El fallback ocultaba silenciosamente errores de configuración
 *
 * ARQUITECTURA ACTUAL:
 *   - ModifiedWeapon REQUIERE ProjectilePool en construcción
 *   - El pool viene pre-configurado desde GameWorldBootstrap con ProjectileContextResolver
 *   - Todos los proyectiles pasan por pool.acquire() que resuelve capacidades
 *   - No hay rutas alternativas que produzcan proyectiles incompletamente inicializados
 *
 * ── LO QUE NO HACE ────────────────────────────────────────────────────────
 *
 *   - No conoce World ni WorldManager.
 *   - No conoce Player, Enemy ni ninguna entidad concreta.
 *   - No gestiona el ciclo de vida de los proyectiles.
 *   - No llama setCollisionProfile() manualmente.
 *   - No resuelve ProjectileContext (eso es responsabilidad del ProjectilePool).
 *
 * ── HRFC — Weapon Type Runtime Identity ──────────────────────────────────
 *
 * ModifiedWeapon conserva la identidad de su WeaponType para permitir:
 *   - WeaponInventory.hasWeapon(WeaponType)
 *   - PlayerRuntime.selectWeapon(WeaponType)
 *   - Selección correcta de arma por tipo en runtime
 *
 * La identidad se establece durante la construcción y permanece inmutable.
 */
public class ModifiedWeapon {

    /**
     * Identidad declarativa de este arma — tipo del arma que representa.
     * Inmutable después de la construcción.
     *
     * ── HRFC — Weapon Type Runtime Identity ──────────────────────────────
     *
     * Este campo resuelve la pérdida de identidad que impedía:
     *   - hasWeapon(WeaponType) — no podía determinar el tipo
     *   - selectWeapon(WeaponType) — no podía encontrar el arma correcta
     *
     * La identidad runtime conserva el WeaponType que creó esta instancia,
     * sin necesidad de registries externos, reflexión o comparación de clases.
     */
    private final WeaponType weaponType;

    private final WeaponComport    comport;
    private final AmuletInventory  amulets;

    /**
     * Pool de proyectiles para reutilización de instancias.
     * 
     * ── HRFC — ProjectilePool Integration Consolidation ──────────────────
     * 
     * Ya NO es nullable. Obligatorio desde construcción.
     * El pool viene pre-configurado con ProjectileContextResolver.
     */
    private final ProjectilePool pool;

    /**
     * Propietario de esta arma. Se propaga al evento OnProjectileSpawn.
     */
    private final Object owner;

    /**
     * Bus de eventos para emitir OnWeaponFired y propagar a los proyectiles.
     * null = no se emiten eventos de arma.
     */
    private final GameEventBus eventBus;

    // ── Constructores ─────────────────────────────────────────────────────

    /**
     * Constructor completo con pool, owner y bus explícitos.
     *
     * ── HRFC — Weapon Type Runtime Identity ──────────────────────────────
     *
     * WeaponType es obligatorio — toda arma runtime debe tener identidad.
     *
     * @param weaponType tipo declarativo del arma (requerido)
     * @param comport    comportamiento del arma (cadencia, cooldown, munición)
     * @param amulets    amuletos del portador del arma
     * ── HRFC — ProjectilePool Integration Consolidation ──────────────
     *
     * ProjectilePool es ahora obligatorio. Los constructores legacy que permitían
     * pool=null han sido eliminados porque:
     *   1. Permitían el fallback a BulletFactory.build() sin contexto
     *   2. Ocultaban silenciosamente la falta de ProjectileContext
     *   3. No había casos de uso legítimos en gameplay para armas sin pool
     *
     * @param weaponType identidad declarativa del arma (obligatorio)
     * @param comport    comportamiento del arma (obligatorio)
     * @param amulets    inventario de amuletos para modificar proyectiles (obligatorio)
     * @param pool       pool de proyectiles configurado (obligatorio)
     * @param owner      el objeto portador del arma (Player, Turret, etc.), o null
     * @param eventBus   bus de eventos (null = sin eventos de arma)
     */
    public ModifiedWeapon(WeaponType weaponType,
                          WeaponComport comport,
                          AmuletInventory amulets,
                          ProjectilePool pool,
                          Object owner,
                          GameEventBus eventBus) {
        if (weaponType == null) throw new IllegalArgumentException("weaponType es requerido");
        if (comport == null) throw new IllegalArgumentException("comport es requerido");
        if (amulets == null) throw new IllegalArgumentException("amulets es requerido");
        if (pool == null) throw new IllegalArgumentException("pool es requerido");

        this.weaponType = weaponType;
        this.comport    = comport;
        this.amulets    = amulets;
        this.pool       = pool;
        this.owner      = owner;
        this.eventBus   = eventBus;
    }

    /**
     * Constructor legacy con BulletType fijo — ELIMINADO.
     * 
     * ── HRFC — Player Inventory & Domain Ownership Consolidation ─────────
     * 
     * Los constructores legacy con BulletType fijo han sido eliminados porque
     * las armas ya no tienen BulletType fijo. El tipo de bala se pasa como
     * parámetro a handleInput() en runtime.
     *
     * ── HRFC — ProjectilePool Integration Consolidation ──────────────────
     *
     * Los constructores que permitían pool=null también han sido eliminados:
     *   - ModifiedWeapon(weaponType, comport, amulets, owner, eventBus)
     *   - ModifiedWeapon(weaponType, comport, amulets)
     *
     * Razón: No existen casos de uso legítimos para armas sin ProjectilePool
     * en gameplay. El fallback a BulletFactory.build() produce proyectiles sin
     * ProjectileContext, lo cual es arquitectónicamente incorrecto.
     * 
     * Migrar a:
     *   ModifiedWeapon(weaponType, comport, amulets, pool, owner, eventBus)
     * 
     * Y usar:
     *   weapon.handleInput(bulletType, held, pressed, x, y, right, direction)
     */

    // ── Input ─────────────────────────────────────────────────────────────

    /**
     * Procesa el input del frame y retorna los proyectiles a spawnear.
     * Lista vacía = no disparó este frame.
     * 
     * @param bulletType tipo de bala a usar para este disparo
     */
    public List<Bullet> handleInput(
            BulletType bulletType,
            boolean held,
            boolean pressed,
            double x, double y,
            boolean right,
            Vector2D direction) {

        var fireModeResult = comport.getFireMode().handleInput(held, pressed, comport);
        if (!fireModeResult.shouldShoot()) {
            return List.of();
        }

        return tryShoot(bulletType, x, y, direction,
                fireModeResult.getDamageMultiplier(),
                fireModeResult.getSpeedMultiplier());
    }

    /**
     * Método legacy que usa el BulletType fijo (ELIMINADO).
     * 
     * ── HRFC — Player Inventory & Domain Ownership Consolidation ─────────
     * 
     * El método handleInput() legacy sin BulletType ha sido eliminado porque
     * las armas ya no tienen BulletType fijo. 
     * 
     * Migrar a:
     *   weapon.handleInput(bulletType, held, pressed, x, y, right, direction)
     */

    // ── Disparo ───────────────────────────────────────────────────────────

    private List<Bullet> tryShoot(
            BulletType bulletType,
            double x, double y,
            Vector2D direction,
            double damageMult,
            double speedMult) {

        if (comport.getFireWait() > 0 || !comport.canShoot()) {
            return List.of();
        }

        // ── Resolución unificada del proyectil ────────────────────────────
        // Usar la misma fuente de resolución que el preview para garantizar consistencia
        ProjectileResolver.ResolvedProjectile resolved = ProjectileResolver.resolveComplete(
                comport.getStats(),      // WeaponStats base (inmutable)
                bulletType,              // BulletType seleccionado
                amulets.getAll(),        // Lista de amuletos del jugador
                damageMult,              // Multiplicador de daño (FireMode)
                speedMult                // Multiplicador de velocidad (FireMode)
        );

        // ── Construir proyectiles usando el blueprint resuelto ───────────
        List<Bullet> bullets = new ArrayList<>(resolved.stats().getBulletsPerShot());

        for (int i = 0; i < resolved.stats().getBulletsPerShot(); i++) {

            Vector2D spreadDir = direction
                    .applySpread(direction, resolved.stats().getSpread())
                    .normalize();

            // ── Adquisición unificada ─────────────────────────────────────
            // Owner se pasa como parámetro runtime, no como parte del blueprint
            //
            // ── HRFC — ProjectilePool Integration Consolidation ──────────
            // Pool es ahora obligatorio (validado en constructor).
            // Ruta única: pool.acquire() que resuelve ProjectileContext correctamente.
            Bullet bullet = pool.acquire(resolved.blueprint(), new Vector2D(x, y), spreadDir, owner);
            bullets.add(bullet);
        }

        // ── Avanzar estado del arma ───────────────────────────────────────
        comport.triggerCooldown();
        comport.incrementBurst();
        comport.consumeAmmo();

        // ── Sonido de disparo ─────────────────────────────────────────────
        String sound = comport.getShootSound();
        if (sound != null) {
            Sounds.playSound(sound);
        }

        // ── Evento de disparo ─────────────────────────────────────────────
        if (eventBus != null && eventBus.hasListeners(WeaponEvents.OnWeaponFired.class)) {
            eventBus.post(new WeaponEvents.OnWeaponFired(this, bullets.size()));
        }

        return bullets;
    }

    // ── Projectile Preview API ───────────────────────────────────────────

    /**
     * Preview de proyectil para UI — encapsula la resolución completa del disparo.
     *
     * ── HRFC — Mini-HRFC: Desacoplar PlayerCombat de la resolución interna del arma ──────────────
     *
     * Este método encapsula todo el conocimiento interno del arma que PlayerCombat
     * no debería tener:
     * - Acceso a WeaponComport y FireMode
     * - Diferencia entre queryResolution() vs handleInput()  
     * - Uso de ProjectileResolver.resolve() con multiplicadores explícitos
     * - Derivación de BulletStats via BulletFactory.statsFrom()
     *
     * PlayerCombat simplemente llama este método y recibe el preview listo para usar.
     * La resolución interna usa exactamente el mismo pipeline que el disparo real,
     * garantizando consistencia entre preview y gameplay.
     *
     * ── SEPARACIÓN QUERY/EXECUTION ────────────────────────────────────────
     *
     * Este método usa fireMode.queryResolution() que es idempotente:
     * - No procesa input ni avanza timers
     * - No muta el estado del FireMode
     * - Solo consulta los multiplicadores actuales
     * 
     * Mientras que handleInput() usa fireMode.handleInput() que muta estado:
     * - Procesa input y puede avanzar timers (ChargeMode)
     * - Puede cambiar estado interno del FireMode
     * - Retorna decisión de disparo además de multiplicadores
     *
     * ── MISMA RESOLUCIÓN, DIFERENTE PROPÓSITO ─────────────────────────────
     *
     *          misma resolución
     *               │
     *       ┌───────┴────────┐
     *       ▼                ▼
     *   handleInput()    getProjectilePreview()
     *   materializa      representa
     *   gameplay         trayectoria
     *
     * @param bulletType tipo de bala a usar para la resolución
     * @param held true si el botón está siendo mantenido (para ChargeMode, etc.)
     * @param spawnPosition posición donde aparecería el proyectil
     * @return ProjectilePreview con stats calculados para UI
     */
    public ProjectilePreview getProjectilePreview(BulletType bulletType, boolean held, Vector2D spawnPosition) {
        if (bulletType == null) {
            return null;
        }

        // ── Consulta idempotente de resolución ────────────────────────────
        // queryResolution() no procesa input ni muta estado del FireMode
        FireModeResolution resolution = comport.getFireMode().queryResolution(held, comport);
        
        // ── Resolución sin side-effects ───────────────────────────────────
        // Usar exactamente la misma fuente de resolución que el disparo real
        ProjectileBlueprint blueprint = ProjectileResolver.resolve(
                comport.getStats(),              // WeaponStats base
                bulletType,                      // BulletType seleccionado  
                amulets.getAll(),                // Amuletos del jugador
                resolution.damageMultiplier(),   // Multiplicador de daño
                resolution.speedMultiplier()     // Multiplicador de velocidad
        );

        // ── Derivar BulletStats para UI ───────────────────────────────────
        BulletStats stats = BulletFactory.statsFrom(blueprint);

        return new ProjectilePreview(
            stats.getSpeed(),
            stats.getDamage(), 
            stats.getLifeTime(),
            stats.hasGravity(),
            spawnPosition
        );
    }

    /**
     * Preview de proyectil para UI — encapsula stats calculados.
     *
     * ── HRFC — Mini-HRFC: Desacoplar PlayerCombat de la resolución interna del arma ──────────────
     *
     * Reemplaza el uso directo de ProjectileResolver y BulletFactory desde PlayerCombat.
     * PlayerCombat consulta este record inmutable en lugar de reconstruir manualmente 
     * el pipeline de disparo accediendo a weapon internals.
     *
     * @param speed        velocidad del proyectil (unidades/frame)
     * @param damage       daño del proyectil
     * @param lifeTime     frames de vida máximos
     * @param hasGravity   true si el proyectil tiene gravedad
     * @param spawnPosition posición de spawn del proyectil (mundo)
     */
    public record ProjectilePreview(
        double speed,
        double damage,
        int lifeTime,
        boolean hasGravity,
        Vector2D spawnPosition
    ) {}

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    /**
     * Actualiza los timers del arma (cooldown, recarga).
     *
     * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────
     *
     * PROPAGACIÓN TEMPORAL:
     *   ModifiedWeapon recibe deltaTime de PlayerCombat y lo propaga a:
     *     - WeaponComport.update(deltaTime) → para cooldowns y recargas temporales
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void update(double deltaTime) { 
        comport.update(deltaTime); 
    }

    public void reload() { comport.startReload(); }

    public void resetBurst() { comport.resetBurst(); }

    // ── Consultas de estado ───────────────────────────────────────────────

    public int     getCurrentAmmo()     { return comport.getCurrentAmmo();   }
    public int     getMaxAmmo()         { return comport.getChargerSize();   }
    
    /**
     * Estado interno de recarga del arma (mecánica) — DEPRECATED para UI.
     * 
     * ── HRFC — Player Reengineering v2 ────────────────────────────────────
     * 
     * Para UI y Renderer, usar PlayerState.isReloading() en su lugar.
     * Este método retorna únicamente el estado de la mecánica interna del arma.
     * 
     * @deprecated Para UI usar PlayerState.isReloading()
     */
    @Deprecated
    public boolean isReloading()        { return comport.isReloading();      }
    
    public boolean isFullyLoaded()      { return comport.isFullyLoaded();    }
    public double     getFireWait()        { return comport.getFireWait();      }
    public double     getCooldown()        { return comport.getCooldown();      }
    public double  getBulletSpeedBase() { return comport.getStats().getBulletSpeedBase(); }

    // ── Acceso a subcomponentes ────────────────────────────────────────────

    /**
     * Tipo declarativo de este arma.
     *
     * ── HRFC — Weapon Type Runtime Identity ──────────────────────────────
     *
     * Retorna el WeaponType que define la identidad de esta instancia runtime.
     * La identidad permanece inmutable independientemente de las modificaciones
     * aplicadas al arma (amuletos, stats modificados, etc.).
     *
     * Permite:
     *   - WeaponInventory.hasWeapon(WeaponType) — verificar posesión por tipo
     *   - PlayerRuntime.selectWeapon(WeaponType) — seleccionar arma correcta
     *   - Comparación directa sin reflexión, IDs String o registries
     *
     * @return WeaponType de esta arma. Nunca null.
     */
    public WeaponType getWeaponType() {
        return weaponType;
    }

    public WeaponComport    getComport()    { return comport;    }
    public AmuletInventory  getAmulets()    { return amulets;    }
    public WeaponStats      getStats()      { return comport.getStats(); }
    public Object           getOwner()      { return owner; }
    public ProjectilePool getPool()      { return pool; }
    
    /**
     * Método legacy para obtener BulletType fijo (ELIMINADO).
     * 
     * ── HRFC — Player Inventory & Domain Ownership Consolidation ─────────
     * 
     * getBulletType() ha sido eliminado porque las armas ya no tienen BulletType fijo.
     * El tipo de bala se resuelve en runtime desde PlayerRuntime.getCurrentBullet().
     * 
     * Migrar a:
     *   player.getRuntime().getCurrentBullet()  // desde Player
     *   playerRuntime.getCurrentBullet()        // desde PlayerRuntime
     */

}
