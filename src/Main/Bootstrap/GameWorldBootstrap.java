package Main.Bootstrap;

import Game.Enemys.Core.EnemySpawner;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Ammulets.AmuletType;
import Game.Items.Types.Bullets.Capability.ProjectileContextResolver;
import Game.Items.Types.Bullets.Capability.ProjectileSpawningCapability;
import Game.Items.Types.Bullets.Capability.SpatialQueryCapability;
import Game.Items.Types.Bullets.Capability.WorldProjectileSpawningProvider;
import Game.Items.Types.Bullets.Capability.WorldSpatialCapabilityProvider;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Bullets.ProjectileRegistry;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import Game.Player.Player;
import Game.Player.PlayerLoadout;
import Game.World.Core.WorldManager;

/**
 * Bootstrap del mundo de juego.
 *
 * Responsabilidad única: construir y conectar los actores iniciales del mundo.
 *
 * ── BUS DE EVENTOS ────────────────────────────────────────────────────────
 *
 * GameWorldBootstrap crea la instancia de GameEventBus del mundo y la
 * inyecta en todos los sistemas que la necesitan:
 *
 *   - Player (→ PlayerCombat → WeaponInventory → ModifiedWeapon)
 *   - EnemySpawner (→ Enemy, EnemyAssembler → BoneBarragePattern)
 *   - ProjectileRegistry (→ ProjectilePool → Bullet)
 *
 * No existe ningún bus global estático. El bus tiene el mismo lifecycle
 * que el World: se crea en el constructor y se libera en shutdown().
 *
 * ── OWNERSHIP DE LISTENERS / SUBSCRIPTIONS ────────────────────────────────
 *
 * Los sistemas con lifecycle de World (ProjectileRegistry, LootSystem)
 * instalan listeners en el bus de instancia. Para liberar esas referencias
 * cuando el World se destruye, llamar shutdown().
 *
 * Clasificación por scope:
 *
 *   SCOPE DE WORLD/SESSION (deben liberarse al destruir el World):
 *     → ProjectileRegistry.listener  (installListener → uninstallListener)
 *     → LootSystem.listener          (register → subscription.cancel())
 *     → AmuletRegistry.entityProvider (setEntityProvider → setEntityProvider(null))
 *
 * ── LO QUE HACE ───────────────────────────────────────────────────────────
 *   1. Crea el GameEventBus del mundo.
 *   2. Crea el Player con el bus inyectado.
 *   3. Lo registra en el globalDynamicRegistry via worldManager.addDynamic().
 *   4. Lo registra como tracked object (cámara).
 *   5. Configura el WorldController predicate en TransitionService.
 *   6. Instala el listener de bullets con referencia al globalDynamicRegistry.
 *   7. Registra capability providers en ProjectileContextResolver.
 *   8. Inyecta el bus en el ProjectilePool para eventos de proyectil.
 *   9. Configura AmuletRegistry con proveedor de entidades dinámico.
 *  10. Spawna los enemigos iniciales con el bus inyectado.
 */
public final class GameWorldBootstrap {

    private final Player player;
    private final ProjectileRegistry projectileRegistry;

    /** Bus de eventos del mundo. Lifecycle: desde construcción hasta shutdown(). */
    private final GameEventBus eventBus;

    public GameWorldBootstrap(WorldManager worldManager,
                              int virtualWidth,
                              int virtualHeight) {

        // ── Bus de eventos del mundo ─────────────────────────────────────
        // Usar el bus que WorldManager ya creó internamente, para que los
        // TransitionEvents y los eventos de gameplay compartan el mismo bus.
        eventBus = worldManager.getEventBus();

        // ── Registros globales de ítems ──────────────────────────────────

        // ── ProjectileContext — ANTES de crear el Player ─────────────────
        // 
        // BUG FIX: El contexto debe existir ANTES de que el Player pueda disparar.
        // 
        // Problema anterior:
        //   1. Player.create() → weapon.shoot() puede disparar inmediatamente
        //   2. ProjectilePool.acquire() inyecta ProjectileContext.NULL
        //   3. Proyectiles entraban al mundo con contexto NULL
        //   4. [TARDE] setProjectileContext(worldContext)
        // 
        // Solución:
        //   1. Crear WorldProjectileContext primero
        //   2. Inyectar en ProjectileRegistry/Pool
        //   3. AHORA Player.create() (disparos tienen contexto correcto)
        // 
        // ── ProjectileContext Resolver — ANTES de crear el Player ────────────
        // 
        // BUG FIX: El contexto debe existir ANTES de que el Player pueda disparar.
        // 
        // ARQUITECTURA COMPOSABLE:
        //   1. Crear ProjectileContextResolver
        //   2. Registrar capability providers (World-backed)
        //   3. Inyectar resolver en ProjectilePool
        //   4. AHORA Player.create() (disparos resuelven contexto correcto)
        // 
        // Este reordenamiento garantiza que NINGÚN proyectil de gameplay pueda
        // nacer con ProjectileContext.NULL cuando existe infraestructura real.
        projectileRegistry = ProjectileRegistry.getInstance();
        
        // Create resolver and register capability providers
        ProjectileContextResolver resolver = new ProjectileContextResolver();
        
        // Register WorldManager-backed capability providers
        resolver.registerProvider(
            SpatialQueryCapability.class,
            new WorldSpatialCapabilityProvider(worldManager)
        );
        
        resolver.registerProvider(
            ProjectileSpawningCapability.class,
            new WorldProjectileSpawningProvider(worldManager, projectileRegistry.getPool())
        );
        
        // Inject resolver into pool
        projectileRegistry.getPool().setContextResolver(resolver);
        
        // Bus en el pool — también antes del Player
        projectileRegistry.getPool().setEventBus(eventBus);

        // ── Posición de spawn del jugador ────────────────────────────────
        Vector2D spawnPos = new Vector2D(
            (double) virtualWidth  / 2.0,
            (double) virtualHeight / 2.0 - 200
        );

        // ── Player ───────────────────────────────────────────────────────
        // PlayerAssembler.assemble() construye y conecta todos los módulos.
        // El bus se inyecta en PlayerAssembler → PlayerCombat → WeaponInventory → ModifiedWeapon
        //
        // ── MINI-HRFC — Corrección de Arquitectura del Loadout Inicial ───
        // 
        // El loadout se construye directamente usando la API declarativa de PlayerLoadout.
        // Esta sintaxis demuestra que la capacidad de construcción pertenece
        // al contexto (GameWorldBootstrap), no a PlayerLoadout.
        //
        // Loadout estándar para inicio de partida:
        PlayerLoadout loadout = PlayerLoadout
            .initialWeapons(WeaponType.PISTOLA)
            .initialBullets(BulletType.METEOR_BULLET)
            .initialAmulets(AmuletType.SPLIT_CRYSTAL)
            .build();
        
        // ── EJEMPLO DE CONFIGURACIÓN DECLARATIVA ─────────────────────────
        // Para testing/desarrollo, descomente la línea siguiente:
        // loadout = createDevelopmentLoadout();
        
        // ── HRFC — ProjectilePool Integration Consolidation ──────────────
        // Pasar el pool configurado al Player para que las armas lo usen.
        // El pool ya tiene el ProjectileContextResolver con los providers registrados.
        player = Player.create(spawnPos,
            obj -> worldManager.addDynamic(obj),
            eventBus,
            loadout,
            projectileRegistry.getPool()  // Pool configurado con resolver
        );

        worldManager.addDynamic(player);
        worldManager.setTrackedObject(player);

        worldManager.getTransitionService()
            .setWorldControllerPredicate(obj -> obj == player);

        // ── AmuletRegistry — proveedor de entidades dinámico ─────────────
        Game.Items.Types.Ammulets.AmuletRegistry.setEntityProvider(() ->
            worldManager.getGlobalDynamicRegistry()
                .getAll()
                .stream()
                .filter(o -> o instanceof Game.Engine.AbstractEntity)
                .map(o -> (Game.Engine.AbstractEntity) o)
                .toList()
        );

        // ── ProjectileRegistry — listener en el bus del mundo ────────────
        // Scope: WORLD. Se cancela llamando projectileRegistry.uninstallListener()
        // o ProjectileRegistry.shutdown() desde este bootstrap.shutdown().
        projectileRegistry.installListener(
            eventBus,
            bullet -> worldManager.addDynamic(bullet)
        );

        // ── Spawn inicial de enemigos — con bus inyectado ─────────────────
        new EnemySpawner().spawn(worldManager.getCurrentWorld(), 10, eventBus);
    }

    /** El Player creado durante el bootstrap. */
    public Player getPlayer() {
        return player;
    }

    /**
     * El bus de eventos del mundo.
     * Disponible para que otros sistemas (LootSystem, UIBootstrap, AudioSystem)
     * puedan registrarse sin necesitar una instancia global.
     */
    public GameEventBus getEventBus() {
        return eventBus;
    }

    /**
     * Libera todos los listeners y referencias de scope World.
     *
     * Llamar cuando el World se destruye (reinicio de partida, cambio de escena,
     * reconstrucción del WorldManager).
     *
     * Limpia:
     *   - ProjectileRegistry: cancela listener en bus + destruye el pool
     *   - AmuletRegistry: elimina el entity provider del World destruido
     */
    public void shutdown() {
        ProjectileRegistry.shutdown();
        Game.Items.Types.Ammulets.AmuletRegistry.setEntityProvider(null);
    }
}
