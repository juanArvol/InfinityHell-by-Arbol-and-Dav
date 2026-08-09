package Main.Bootstrap;

import Game.Enemys.Core.EnemySpawner;
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
 * ── CORRECCIÓN DEL BUG DEL PLAYER ─────────────────────────────────────────
 *
 * ANTES (buggy):
 *   player se añadía a worldManager.getCurrentWorld().addDynamic(player)
 *   Al cruzar un chunk, getCurrentWorld() devolvía un World diferente con
 *   un DynamicEntityRegistry vacío. El Player desaparecía de la simulación.
 *
 * AHORA (correcto):
 *   Player, bullets y enemies se añaden mediante worldManager.addDynamic()
 *   que registra directamente en el globalDynamicRegistry del universo.
 *   El globalDynamicRegistry es invariante ante cambios de sector.
 *
 * ── OWNERSHIP DE LISTENERS / SUBSCRIPTIONS ────────────────────────────────
 *
 * Este bootstrap crea componentes con lifecycle de World (ProjectileRegistry,
 * LootSystem, AmuletRegistry entity provider) que instalan listeners en
 * GameEventBus.GLOBAL. Para evitar fugas de memoria cuando el World se
 * destruye, esos listeners DEBEN cancelarse.
 *
 * Clasificación por scope:
 *
 *   SCOPE DE APLICACIÓN (viven toda la vida del proceso):
 *     → No necesitan Subscription activa; se limpian solos al terminar la JVM.
 *
 *   SCOPE DE WORLD/SESSION (deben liberarse al destruir el World):
 *     → ProjectileRegistry.listener  (installListener → uninstallListener)
 *     → LootSystem.listener          (register → subscription.cancel())
 *     → AmuletRegistry.entityProvider (setEntityProvider → setEntityProvider(null))
 *
 * La responsabilidad de limpieza recae en quien llama a shutdown().
 * El caller del bootstrap (GameState, SceneManager, etc.) debe llamar
 * bootstrap.shutdown() cuando destruye el World.
 *
 * ── LO QUE HACE ───────────────────────────────────────────────────────────
 *   1. Crea el Player en la posición de spawn del sector inicial.
 *   2. Lo registra en el globalDynamicRegistry via worldManager.addDynamic().
 *   3. Lo registra como tracked object (cámara).
 *   4. Configura el WorldController predicate en TransitionService.
 *   5. Instala el listener de bullets con referencia al globalDynamicRegistry.
 *   6. Inyecta WorldProjectileContext en el pool de ProjectileRegistry.
 *   7. Configura AmuletRegistry con proveedor de entidades dinámico.
 *   8. Spawna los enemigos iniciales en el globalDynamicRegistry.
 */
public final class GameWorldBootstrap {

    private final Player player;

    /**
     * Referencia al registry para poder hacer shutdown() cuando el World muera.
     * El registry tiene lifecycle de World — no es un singleton de aplicación.
     */
    private final ProjectileRegistry projectileRegistry;

    public GameWorldBootstrap(WorldManager worldManager,
                              int virtualWidth,
                              int virtualHeight) {

        // ── Registros globales de ítems ──────────────────────────────────────
        ItemRegistry.init();

        // ── Posición de spawn del jugador ────────────────────────────────────
        Vector2D spawnPos = new Vector2D(
            (double) virtualWidth  / 2.0,
            (double) virtualHeight / 2.0 - 200
        );

        // ── Player ───────────────────────────────────────────────────────────
        // bulletSpawner usa worldManager.addDynamic() → globalDynamicRegistry.
        // Invariante ante cambios de sector.
        player = new Player(spawnPos,
            obj -> worldManager.addDynamic(obj)
        );

        // Registrar Player en el globalDynamicRegistry — no en un World concreto.
        worldManager.addDynamic(player);

        // ── Cámara y tracking ─────────────────────────────────────────────────
        worldManager.setTrackedObject(player);

        // ── Configurar el WorldController predicate ───────────────────────────
        worldManager.getTransitionService()
            .setWorldControllerPredicate(obj -> obj == player);

        // ── AmuletRegistry — proveedor de entidades dinámico ─────────────────
        // Scope: WORLD. Si el World se destruye, limpiar con:
        //   AmuletRegistry.setEntityProvider(null);
        // El provider no instala listener en el bus — es una referencia directa,
        // no una Subscription. Se limpia sobreescribiendo con null en shutdown().
        Game.Items.Types.Ammulets.AmuletRegistry.setEntityProvider(() ->
            worldManager.getGlobalDynamicRegistry()
                .getAll()
                .stream()
                .filter(o -> o instanceof Game.Engine.AbstractEntity)
                .map(o -> (Game.Engine.AbstractEntity) o)
                .toList()
        );

        // ── ProjectileRegistry — listener con globalDynamicRegistry ──────────
        // Scope: WORLD. Listener registrado en GameEventBus.GLOBAL.
        // Se cancela llamando projectileRegistry.uninstallListener() o
        // ProjectileRegistry.shutdown() desde este bootstrap.shutdown().
        //
        // Las balas se añaden al globalDynamicRegistry, no al World del sector.
        // Una bala disparada desde Chunk(0,0) sigue siendo simulada cuando
        // entra en Chunk(1,0) porque vive en el registry global.
        projectileRegistry = ProjectileRegistry.getInstance();
        projectileRegistry.installListener(
            bullet -> worldManager.addDynamic(bullet)
        );

        // ── ProjectileContext — contexto real del mundo ───────────────────────
        // Inyectar WorldProjectileContext en el pool del registry para que
        // behaviors puedan llamar ctx.spawnProjectile() en onExpire sin obtener
        // un no-op silencioso desde ProjectileContext.NULL.
        //
        // Se pasa también el pool del registry para que los proyectiles secundarios
        // generados desde onExpire pasen por el mismo lifecycle (pool → reutilización
        // o creación → configuración → uso → release) que los proyectiles normales.
        // Sin esto existían dos caminos paralelos de creación:
        //   - Proyectiles normales:     ProjectileRegistry → pool.acquire()
        //   - Proyectiles secundarios:  BulletFactory.build() directamente
        ProjectileContext worldContext = new Game.World.Systems.WorldProjectileContext(
            worldManager,
            projectileRegistry.getPool()
        );
        projectileRegistry.setProjectileContext(worldContext);

        // ── Spawn inicial de enemigos ─────────────────────────────────────────
        // EnemySpawner.spawn() llama world.addDynamic() → externalRegistry
        // → globalDynamicRegistry. Los enemigos quedan en el registry global.
        new EnemySpawner().spawn(worldManager.getCurrentWorld(), 2);
    }

    /** El Player creado durante el bootstrap. */
    public Player getPlayer() {
        return player;
    }

    /**
     * Libera todos los listeners y referencias de scope World instalados
     * por este bootstrap en GameEventBus.GLOBAL y en registros globales.
     *
     * Llamar cuando el World se destruye (reinicio de partida, cambio de escena,
     * reconstrucción del WorldManager). Después de llamar shutdown(), este
     * bootstrap ya no está activo y no debe usarse.
     *
     * Limpia:
     *   - ProjectileRegistry: cancela listener en bus + destruye el pool
     *   - AmuletRegistry: elimina el entity provider del World destruido
     *
     * No limpia:
     *   - WeaponRegistry, AmuletRegistry.definitions: son singletons de aplicación
     *   - ItemRegistry: singleton de aplicación
     *   - GameEventBus.GLOBAL: no se hace clear() global — solo se cancelan
     *     las subscriptions específicas de este World
     */
    public void shutdown() {
        // Cancelar listener del ProjectileRegistry en GameEventBus.GLOBAL.
        // Esto libera la referencia al bulletSpawner (worldManager::addDynamic)
        // y al projectileRegistry mismo que quedarían atrapados en GLOBAL.
        ProjectileRegistry.shutdown();

        // Limpiar entity provider del AmuletRegistry para no retener
        // una referencia al worldManager del World destruido.
        Game.Items.Types.Ammulets.AmuletRegistry.setEntityProvider(null);
    }
}
