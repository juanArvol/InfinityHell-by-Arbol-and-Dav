package Game.World.Core;

import Game.Engine.GameObjects;
import Game.Engine.Systems.RenderSystem;
import Game.Engine.Systems.DepthSortedRenderSystem;
import Game.Engine.Systems.DebugRenderSystem;
import Game.Render.Camera;
import Game.Render.RenderContext;
import Game.World.WorldObjects.WorldObjectsContainer;

import java.awt.Graphics;

/**
 * Mundo del juego.
 * NUEVO: usa DepthSortedRenderSystem para render correcto en 2.5D.
 * El flag USE_DEPTH_SORT permite alternar entre el sistema original y el nuevo.
 */
public class World {

    private int width;
    private int height;
    private final WorldCoordinator coordinate;

    private final WorldObjectsContainer objects = new WorldObjectsContainer();
    private final Camera camera = new Camera();

    // FIX BUG-04: cámara sigue al player cada frame
    private Game.Engine.GameObjects cameraTarget;
    private int lastScreenW;
    private int lastScreenH;

    // NUEVO 2.5D: sistema de render con depth sorting
    private static final boolean USE_DEPTH_SORT = true;

    private final RenderSystem renderSystem = new RenderSystem();
    private final DepthSortedRenderSystem depthRenderSystem = new DepthSortedRenderSystem();
    private final DebugRenderSystem debugRenderSystem = new DebugRenderSystem();

    public World(int width, int height, WorldCoordinator coordinate) {
        this.width = width;
        this.height = height;
        this.coordinate = coordinate;
    }

    public void update() {
        objects.update();

        // FIX BUG-04: actualizar cámara cada frame si hay un target definido
        if (cameraTarget != null) {
            centerCameraOn(cameraTarget, lastScreenW, lastScreenH);
        }
    }

    public void draw(Graphics g) {
        RenderContext ctx = new RenderContext(g);

        if (USE_DEPTH_SORT) {
            depthRenderSystem.render(objects.getObjects(), ctx, camera);
        } else {
            renderSystem.render(objects.getObjects(), ctx, camera);
        }

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

    /**
     * Centra la cámara en un objeto, con clamp al área del mundo.
     * FIX DESIGN-009: usa la versión con límites para no mostrar área fuera del mundo.
     */
    /**
     * Centra la cámara en un objeto y configura ese objeto como target de seguimiento.
     * FIX BUG-04: llamar esto una vez en init() configura el follow permanente.
     */
    public void centerCameraOn(GameObjects obj, int screenWidth, int screenHeight) {
        this.cameraTarget = obj;
        this.lastScreenW  = screenWidth;
        this.lastScreenH  = screenHeight;

        var pos = obj.getTransform().getPosition();
        camera.centerOn(
            pos.getX(), pos.getY(),
            screenWidth, screenHeight,
            width, height
        );
    }

    public Camera getCamera() {
        return camera;
    }

    public void resize(int newWidth, int newHeight, double scaleX, double scaleY) {
        this.width = newWidth;
        this.height = newHeight;
        // NOTA: NO escalamos las posiciones de los objetos aquí.
        // El escalado de posiciones (BUG-007) debe resolverse con coordenadas lógicas.
        // Por ahora desactivamos el scale para no corromper posiciones.
    }

    public int getWidth()  { return width; }
    public int getHeight() { return height; }
    public WorldCoordinator getCoordinate() { return coordinate; }
}
