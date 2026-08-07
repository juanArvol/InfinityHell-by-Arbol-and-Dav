package Main.Bootstrap;

import Game.Enemys.Core.EnemySpawner;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Creation.ItemRegistry;
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
 * ── LO QUE HACE ───────────────────────────────────────────────────────────
 *   1. Crea el Player en la posición de spawn del sector inicial.
 *   2. Lo registra en el globalDynamicRegistry via worldManager.addDynamic().
 *   3. Lo registra como tracked object (cámara).
 *   4. Configura el WorldController predicate en TransitionService.
 *   5. Instala el listener de bullets con referencia al globalDynamicRegistry.
 *   6. Configura AmuletRegistry con proveedor de entidades dinámico.
 *   7. Spawna los enemigos iniciales en el globalDynamicRegistry.
 */
public final class GameWorldBootstrap {

    private final Player player;

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
        // Consulta el globalDynamicRegistry via worldManager para siempre
        // encontrar entidades independientemente del sector activo.
        Game.Items.Types.Ammulets.AmuletRegistry.setEntityProvider(() ->
            worldManager.getGlobalDynamicRegistry()
                .getAll()
                .stream()
                .filter(o -> o instanceof Game.Engine.AbstractEntity)
                .map(o -> (Game.Engine.AbstractEntity) o)
                .toList()
        );

        // ── ProjectileRegistry — listener con globalDynamicRegistry ──────────
        // Las balas se añaden al globalDynamicRegistry, no al World del sector.
        // Una bala disparada desde Chunk(0,0) sigue siendo simulada cuando
        // entra en Chunk(1,0) porque vive en el registry global.
        ProjectileRegistry registry = ProjectileRegistry.getInstance();
        registry.installListener(
            bullet -> worldManager.addDynamic(bullet)
        );

        // ── Spawn inicial de enemigos ─────────────────────────────────────────
        // EnemySpawner.spawn() llama world.addDynamic() → externalRegistry
        // → globalDynamicRegistry. Los enemigos quedan en el registry global.
        new EnemySpawner().spawn(worldManager.getCurrentWorld(), 1);
    }

    /** El Player creado durante el bootstrap. */
    public Player getPlayer() {
        return player;
    }
}
