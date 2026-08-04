package Main.Bootstrap;

import Game.Enemys.Core.EnemySpawner;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Creation.ItemRegistry;
import Game.Player.Player;
import Game.World.Core.World;
import Game.World.Core.WorldManager;

/**
 * Bootstrap del mundo de juego.
 *
 * Responsabilidad única: construir y conectar los actores iniciales del mundo.
 *
 * Hace exactamente tres cosas:
 *   1. Crea el Player en la posición de spawn.
 *   2. Lo añade al World.
 *   3. Lo registra en WorldManager como tracked object (configura cámara y EnemyUpdater).
 *
 * Lo que NO hace:
 *   - No conoce la UI.
 *   - No coordina update() ni draw().
 *   - No toma decisiones de gameplay.
 *   - No gestiona la cámara directamente (eso lo hace WorldManager).
 *
 * ── HRFC-001 ─────────────────────────────────────────────────────────────
 *
 * centerCameraOn() fue eliminado de World. La cámara ya no pertenece al World.
 * Ahora se llama worldManager.setTrackedObject(player), que:
 *   1. Registra el player como target de seguimiento en World.
 *   2. Configura el FollowCameraController en WorldManager.
 *   3. Hace snap inicial de la cámara sobre el player.
 */
public final class GameWorldBootstrap {

    private final Player player;

    public GameWorldBootstrap(WorldManager worldManager,
                              int virtualWidth,
                              int virtualHeight) {

        // ── Registros globales ───────────────────────────────────────────────
        ItemRegistry.init();

        World world = worldManager.getCurrentWorld();

        // ── Spawn position ───────────────────────────────────────────────────
        Vector2D spawnPos = new Vector2D(
            world.getWidth()  / 2.0,
            world.getHeight() / 2.0 - 200
        );

        // ── Player ───────────────────────────────────────────────────────────
        // HRFC-002: Player ya no recibe BufferedImage — obtiene su SpriteHandle
        // de PlayerAssets internamente. El Gameplay nunca conoce la imagen.
        player = new Player(spawnPos, world::add);
        world.add(player);

        // ── Cámara y tracking ─────────────────────────────────────────────────
        // WorldManager configura el FollowCameraController, hace snap inicial
        // de la GameCamera sobre el player y registra el EnemyUpdater en World.
        worldManager.setTrackedObject(player);

        // ── Initial enemy spawn ──────────────────────────────────────────────
        new EnemySpawner().spawn(world, 1);
    }

    /** El Player creado durante el bootstrap. */
    public Player getPlayer() {
        return player;
    }
}
