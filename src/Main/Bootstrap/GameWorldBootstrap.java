package Main.Bootstrap;

import Game.Enemys.Core.EnemySpawner;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Creation.ItemRegistry;
import Game.Items.Types.Bullets.Definition.ProjectileContext;
import Game.Items.Types.Bullets.ProjectileRegistry;
import Game.Player.Player;
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
 *   7. Inyecta WorldProjectileContext en el pool de ProjectileRegistry.
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
        ItemRegistry.init();

        // ── Posición de spawn del jugador ────────────────────────────────
        Vector2D spawnPos = new Vector2D(
            (double) virtualWidth  / 2.0,
            (double) virtualHeight / 2.0 - 200
        );

        // ── Player ───────────────────────────────────────────────────────
        // PlayerAssembler.assemble() construye y conecta todos los módulos.
        // El bus se inyecta en PlayerAssembler → PlayerCombat → WeaponInventory → ModifiedWeapon
        player = Player.create(spawnPos,
            obj -> worldManager.addDynamic(obj),
            eventBus
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
        projectileRegistry = ProjectileRegistry.getInstance();
        projectileRegistry.installListener(
            eventBus,
            bullet -> worldManager.addDynamic(bullet)
        );

        // ── ProjectileContext — contexto real del mundo ───────────────────
        ProjectileContext worldContext = new Game.World.Systems.WorldProjectileContext(
            worldManager,
            projectileRegistry.getPool()
        );
        projectileRegistry.setProjectileContext(worldContext);

        // ── Bus en el pool de proyectiles ─────────────────────────────────
        // Los Bullets adquiridos desde el pool recibirán el bus para emitir
        // eventos de colisión, expiración y destrucción.
        projectileRegistry.getPool().setEventBus(eventBus);

        // ── Spawn inicial de enemigos — con bus inyectado ─────────────────
        new EnemySpawner().spawn(worldManager.getCurrentWorld(), 2, eventBus);
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
