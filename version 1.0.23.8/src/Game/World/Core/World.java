 package Game.World.Core;

import Game.Engine.GameObjects;
import Game.Engine.Systems.RenderSystem;
import Game.Engine.Systems.DebugRenderSystem;
import Game.Render.Camera;
import Game.Render.RenderContext;
import Game.World.WorldObjects.WorldObjectsContainer;

import java.awt.Graphics;

public class World {
    private int width;
    private int height;
    private final WorldCoordinator coordinate;

    private final WorldObjectsContainer objects = new WorldObjectsContainer();
    private final Camera camera = new Camera();

    private final RenderSystem renderSystem = new RenderSystem();
    private final DebugRenderSystem debugRenderSystem = new DebugRenderSystem();

    public World(int width, int height, WorldCoordinator coordinate) {
        this.width = width;
        this.height = height;
        this.coordinate = coordinate;
    }

    public void update() {
        objects.update();
    }

    public void draw(Graphics g) {
        RenderContext ctx = new RenderContext(g);
        renderSystem.render(objects.getObjects(), ctx, camera);
        debugRenderSystem.render(objects.getObjects(), ctx, camera);
    }

    public void add(GameObjects obj) {
        objects.add(obj);
    }

    public void remove(GameObjects obj) {
        objects.remove(obj);
    }

    public WorldObjectsContainer getObjectsContainer() {
        return objects;
    }

    public void centerCameraOn(GameObjects obj, int screenWidth, int screenHeight) {
        var pos = obj.getTransform().getPosition();
        camera.centerOn(pos.getX(), pos.getY(), screenWidth, screenHeight);
    }

    public Camera getCamera() {
        return camera;
    }

    public void resize(int newWidth, int newHeight, double scaleX, double scaleY) {
        this.width = newWidth;
        this.height = newHeight;

        for (GameObjects obj : objects.getObjects()) {
            obj.scale(scaleX, scaleY);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public WorldCoordinator getCoordinate() {
        return coordinate;
    }
}