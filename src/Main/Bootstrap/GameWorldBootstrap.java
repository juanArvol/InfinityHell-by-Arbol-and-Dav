package Main.Bootstrap;

import Game.Enemys.Spawner.EnemySpawner;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Player.Player;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import Graficos.Player.PlayerAssets;

/**
 * Bootstrap del mundo de juego.
 *
 * Responsabilidad única: construir y conectar los actores iniciales del mundo.
 *
 * Hace exactamente tres cosas:
 *   1. Crea el Player en la posición de spawn.
 *   2. Lo registra en el World y configura la cámara inicial.
 *   3. Registra el Player como tracked object y lanza el spawn inicial de enemigos.
 *
 * Lo que NO hace:
 *   - No conoce la UI.
 *   - No coordina update() ni draw().
 *   - No toma decisiones de gameplay.
 *
 * Por qué esta clase y no un método privado en GameState:
 *   Si en el futuro aparecen múltiples modos de juego (tutorial, boss rush,
 *   multiplayer), cada modo puede tener su propio Bootstrap sin modificar GameState.
 *   Un método privado no ofrece ese punto de extensión.
 */
public final class GameWorldBootstrap {

    private final Player      player;
    private final EnemySpawner spawner;

    public GameWorldBootstrap(WorldManager worldManager,
                              int virtualWidth,
                              int virtualHeight) {

        World world = worldManager.getCurrentWorld();

        // ── Spawn position ───────────────────────────────────────────────────
        Vector2D spawnPos = new Vector2D(
            world.getWidth()  / 2.0,
            world.getHeight() / 2.0 - 200
        );

        // ── Player ───────────────────────────────────────────────────────────
        player = new Player(spawnPos, PlayerAssets.idle.getSprite(), world::add);
        world.add(player);
        world.centerCameraOn(player, virtualWidth, virtualHeight);

        // El conocimiento de "quién es el jugador" vive en la capa de composición,
        // no dentro de WorldManager. Aquí es donde se establece ese vínculo.
        worldManager.setTrackedObject(player);

        // ── Initial enemy spawn ──────────────────────────────────────────────
        spawner = new EnemySpawner(player);
        spawner.spawn(world, 0);
    }

    /** El Player creado durante el bootstrap. GameState lo necesita para la UI y la cámara en update(). */
    public Player getPlayer() {
        return player;
    }
}
