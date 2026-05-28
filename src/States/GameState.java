package States;

import Display.Managers.DisplayManager;
import Display.ViewportInfo;
import Game.Player.Player;
import Game.UI.AmmoHUD;
import Game.UI.CrossHairHUD;
import Game.UI.LifeHUD;
import Game.UI.UIManager;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import Game.Settings.GameSettings;
import GameMath.Vector2D;
import Game.Enemys.Spawner.EnemySpawner;
import Graficos.Player.PlayerAssets;

import java.awt.Graphics2D;

public class GameState {

    private WorldManager worldManager;
    private final UIManager uiManager;

    private Player player;
    private EnemySpawner spawner;

    private final DisplayManager display;

    private boolean initialized = false;

    /**
     * Constructor adaptado al nuevo pipeline.
     * Recibe DisplayManager para acceder a virtualWidth/virtualHeight
     * y al viewport para transformación de coordenadas de input.
     */
    public GameState(DisplayManager display) {
        this.display = display;
        // UIManager se inicializa con la resolución virtual fija desde el inicio
        uiManager = new UIManager(display.getVirtualWidth(), display.getVirtualHeight());
        GameSettings.getInstance().setDebugEnabled(true);
    }

    private void init(int virtualWidth, int virtualHeight) {

        WorldManager.init(virtualWidth, virtualHeight);
        worldManager = WorldManager.getInstance();

        World world = worldManager.getCurrentWorld();

        Vector2D spawnPos = new Vector2D(
                world.getWidth() / 2.0,
                world.getHeight() / 2.0 - 200   // spawn por encima del suelo
        );

        player = new Player(
                spawnPos,
                PlayerAssets.idle.getSprite(),
                world
        );

        world.add(player);
        // FIX: centerCameraOn ahora recibe dimensiones virtuales, no de pantalla real
        world.centerCameraOn(player, virtualWidth, virtualHeight);

        // FIX BUG-13: usar count > 0 para que haya enemigos
        spawner = new EnemySpawner(player);
        spawner.spawn(world, 0);

        // HUDs ahora reciben virtualWidth/virtualHeight en lugar de screen reales
        uiManager.add(new LifeHUD(player.getStats(), virtualWidth, virtualHeight));
        uiManager.add(new AmmoHUD(player.getCombat().getInventory(), virtualWidth, virtualHeight));
        uiManager.add(new CrossHairHUD(player, virtualWidth, virtualHeight));

        initialized = true;
    }

    /**
     * Update en coordenadas virtuales.
     * GameLoop llama: gameState.update(display.getVirtualWidth(), display.getVirtualHeight())
     */
    public void update(int virtualWidth, int virtualHeight) {

        if (!initialized) {
            if (virtualWidth > 0 && virtualHeight > 0) {
                init(virtualWidth, virtualHeight);
            } else {
                return;
            }
        }

        // En el sistema virtual las dimensiones son constantes,
        // pero se mantiene la notificación por compatibilidad futura.
        worldManager.update(virtualWidth, virtualHeight);
        uiManager.update();

        // FIX BUG-04: la cámara debe seguir al player CADA FRAME.
        // Llamar DESPUÉS de worldManager.update() para usar posición ya actualizada.
        worldManager.getCurrentWorld().centerCameraOn(player, virtualWidth, virtualHeight);
    }

    /**
     * Draw adaptado al nuevo pipeline de framebuffer virtual.
     *
     * @param g        Graphics2D del framebuffer virtual (de DisplayManager.beginFrame())
     * @param viewport ViewportInfo actual — disponible para HUDs que necesiten
     *                 transformar coordenadas de input (p.ej. crosshair de mouse)
     */
    public void draw(Graphics2D g, ViewportInfo viewport) {
        if (!initialized) return;

        worldManager.draw(g);
        uiManager.draw(g);
    }
}
