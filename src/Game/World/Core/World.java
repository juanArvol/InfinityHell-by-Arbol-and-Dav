package Game.World.Core;

import Game.Engine.GameObjects;
import Game.Engine.Systems.RenderSystem;
import Game.Engine.Systems.DepthSortedRenderSystem;
import Game.Engine.Systems.DebugRenderSystem;
import Game.Render.Camera;
import Game.Render.RenderContext;
import Game.World.WorldObjects.WorldObjectsContainer;

import java.awt.Graphics2D;

/**
 * Mundo del juego.
 * NUEVO: usa DepthSortedRenderSystem para render correcto en 2.5D.
 * El flag USE_DEPTH_SORT permite alternar entre el sistema original y el nuevo.
 *
 * FIX REFACTOR DISPLAY:
 *  - draw() ahora recibe Graphics2D (framebuffer virtual), no Graphics.
 *  - RenderContext se construye con (Graphics2D, virtualWidth, virtualHeight).
 *  - centerCameraOn() usa virtualWidth/virtualHeight (constantes de DisplaySettings),
 *    no las dimensiones reales del monitor.
 */
public class World {

    private int width;
    private int height;
    private final WorldCoordinator coordinate;

    private final WorldObjectsContainer objects = new WorldObjectsContainer();
    private final Camera camera = new Camera();

    // FIX BUG-04: cámara sigue al player cada frame
    private GameObjects cameraTarget;
    // Guardamos las dims virtuales para el follow frame-a-frame
    private int lastVirtualW;
    private int lastVirtualH;

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
            centerCameraOn(cameraTarget, lastVirtualW, lastVirtualH);
        }
    }

    /**
     * FIX REFACTOR DISPLAY: draw() recibe Graphics2D del framebuffer virtual.
     * RenderContext se construye con las dimensiones virtuales del mundo,
     * que coinciden con DisplaySettings.virtualWidth / virtualHeight.
     *
     * @param g Graphics2D del framebuffer virtual (de DisplayManager.beginFrame())
     */
    public void draw(Graphics2D g) {
        // RenderContext usa el nuevo constructor (Graphics2D, virtualW, virtualH)
        RenderContext ctx = new RenderContext(g, width, height);

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
     * Centra la cámara en un objeto y lo configura como target de seguimiento.
     *
     * FIX REFACTOR DISPLAY: los parámetros son virtualWidth/virtualHeight
     * (constantes de DisplaySettings), NO las dimensiones reales del monitor.
     * Esto garantiza que todos los jugadores ven el mismo área del mundo
     * independientemente de su resolución real.
     *
     * @param obj          objeto a seguir (generalmente el player)
     * @param virtualWidth  DisplaySettings.virtualWidth
     * @param virtualHeight DisplaySettings.virtualHeight
     */
    public void centerCameraOn(GameObjects obj, int virtualWidth, int virtualHeight) {
        this.cameraTarget = obj;
        this.lastVirtualW  = virtualWidth;
        this.lastVirtualH  = virtualHeight;

        var pos = obj.getTransform().getPosition();
        camera.centerOn(
            pos.getX(), pos.getY(),
            virtualWidth, virtualHeight,
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
