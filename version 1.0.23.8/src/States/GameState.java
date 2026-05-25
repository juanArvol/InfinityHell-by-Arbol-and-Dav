package States;

import Game.Player.Player;
import Game.UI.AmmoHUD;
import Game.UI.CrossHairHUD;
import Game.UI.LifeHUD;
import Game.UI.UIManager;
import Game.World.Core.World;
import Game.World.Core.WorldManager;
import Game.Settings.GameSettings;
import GameMath.Vector2D;
import Game.Spawner.EnemySpawner;
import Graficos.Player.PlayerAssets;

import java.awt.Graphics;

public class GameState {

    private WorldManager worldManager;
    private final UIManager uiManager;

    private Player player;
    private EnemySpawner spawner;

    private int screenWidth;
    private int screenHeight;

    private boolean initialized = false;

    public GameState() {
        uiManager = new UIManager();
        GameSettings.getInstance().setDebugEnabled(true);
    }

    private void init(int width, int height) {

        this.screenWidth = width;
        this.screenHeight = height;

        WorldManager.init(width, height);
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
        world.centerCameraOn(player, width, height);

        // FIX BUG-13: usar count > 0 para que haya enemigos
        spawner = new EnemySpawner(player);
        spawner.spawn(world, 0);

        uiManager.add(new LifeHUD(player.getStats(), width, height));
        uiManager.add(new AmmoHUD(player.getCombat().getInventory(), width, height));
        uiManager.add(new CrossHairHUD(player, width, height));

        initialized = true;
    }

    public void update(int width, int height) {

        if (!initialized) {
            if (width > 0 && height > 0) {
                init(width, height);
            } else {
                return;
            }
        }

        if (width != screenWidth || height != screenHeight) {
            screenWidth = width;
            screenHeight = height;

            worldManager.resize(width, height);
            uiManager.updateUI(width, height);
        }

        worldManager.update(width, height);
        uiManager.update();

        // FIX BUG-04: la camara debe seguir al player CADA FRAME, no solo en init().
        // Llamar DESPUES de worldManager.update() para usar la posicion ya actualizada.
        worldManager.getCurrentWorld().centerCameraOn(player, screenWidth, screenHeight);
    }

    public void draw(Graphics g) {
        if (!initialized) return;

        worldManager.draw(g);
        uiManager.draw(g);
    }
}
