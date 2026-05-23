package Game.World.Core;

import Game.World.Generator.WorldGenerator;
import Game.Engine.GameObjects;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class WorldManager {

    private static WorldManager instance;

    private final WorldCache cache = new WorldCache();
    private final WorldGenerator generator = new WorldGenerator();

    private WorldCoordinator currentCoord;

    private int width;
    private int height;

    private WorldManager(int width, int height) {
        this.width = width;
        this.height = height;

        currentCoord = new WorldCoordinator(0, 0);

        regenerateAll();
    }

    public static void init(int width, int height) {
        if (instance == null) {
            instance = new WorldManager(width, height);
        }
    }

    public static WorldManager getInstance() {
        return instance;
    }

    public World getCurrentWorld() {
        if (!cache.contains(currentCoord)) {
            World world = generator.generate(width, height, currentCoord);
            cache.put(world);
        }
        return cache.get(currentCoord);
    }

    public void update(int screenWidth, int screenHeight) {

        World world = getCurrentWorld();

        // Escalado
        if (world.getWidth() != screenWidth || world.getHeight() != screenHeight) {
            double scaleX = (double) screenWidth / world.getWidth();
            double scaleY = (double) screenHeight / world.getHeight();
            world.resize(screenWidth, screenHeight, scaleX, scaleY);
        }

        world.update();
        
        handleTransfers(world);
    }

    public void draw(Graphics g) {
        getCurrentWorld().draw(g);
    }

    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;

        double scaleX = (double) newWidth / width;
        double scaleY = (double) newHeight / height;

        this.width = newWidth;
        this.height = newHeight;

        for (World w : cache.getAllWorlds()) {
            w.resize(newWidth, newHeight, scaleX, scaleY);
        }
    }

    private void handleTransfers(World world) {

        List<GameObjects> toTransfer = new ArrayList<>();

        for (var obj : world.getObjectsContainer().getObjects()) {
            var pos = obj.getTransform().getPosition();

            if (pos.getX() < 0 || pos.getX() > width ||
                pos.getY() < 0 || pos.getY() > height) {
                toTransfer.add(obj);
            }
        }

        for (var obj : toTransfer) {

            var pos = obj.getTransform().getPosition();

            int dx = 0, dy = 0;

            if (pos.getX() < 0) dx = -1;
            else if (pos.getX() >= width) dx = 1;

            if (pos.getY() < 0) dy = -1;
            else if (pos.getY() >= height) dy = 1;

            WorldCoordinator nextCoord =
                    new WorldCoordinator(
                            currentCoord.x() + dx,
                            currentCoord.y() + dy
                    );

            if (!cache.contains(nextCoord)) {
                cache.put(generator.generate(width, height, nextCoord));
            }

            World nextWorld = cache.get(nextCoord);

            double newX = pos.getX();
            double newY = pos.getY();

            if (dx != 0)
                newX = (dx > 0) ? newX - width : newX + width;

            if (dy != 0)
                newY = (dy > 0) ? newY - height : newY + height;

            pos.setX(newX);
            pos.setY(newY);

            world.remove(obj);
            nextWorld.add(obj);

            if (obj instanceof Game.Player.Player) {
                currentCoord = nextCoord;
            }
        }
        world.getObjectsContainer().flush();
        getCurrentWorld().getObjectsContainer().flush();
    }

    private void regenerateAll() {
        cache.clear();
        World first = generator.generate(width, height, currentCoord);
        cache.put(first);
    }
}