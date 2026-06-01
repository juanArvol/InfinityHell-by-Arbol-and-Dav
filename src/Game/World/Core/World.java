package Game.World.Core;

import Game.Engine.GameObjects;
import Game.UI.POV.Camera;
import Game.World.WorldObjects.WorldObjectsContainer;

/**
 * Mundo del juego — estado puro, sin responsabilidades de render.
 *
 * REFACTORIZACIÓN:
 *   World ya NO conoce RenderSystem, DepthSortedRenderSystem, DebugRenderSystem,
 *   RenderContext ni Graphics2D. Esas responsabilidades pertenecen a WorldRenderer.
 *
 *   Justificación técnica:
 *   - World es el modelo del dominio: entidades, posiciones, lógica, física.
 *   - El cómo se dibuja es una preocupación de la capa de presentación.
 *   - Separar ambas hace World testeable sin contexto gráfico.
 *   - Un servidor headless puede usar World sin dependencias AWT.
 *
 *   Eliminado: draw(Graphics2D), USE_DEPTH_SORT flag, instancias de RenderSystem.
 *   Añadido: getCamera() ya existía; getObjectsContainer() ya existía.
 *   Compatibilidad: WorldManager.draw() delega ahora a WorldRenderer.
 */
public class World {

    private int width;
    private int height;
    private final WorldCoordinator coordinate;

    private final WorldObjectsContainer objects = new WorldObjectsContainer();
    private final Camera camera = new Camera();

    // Seguimiento de cámara
    private GameObjects cameraTarget;
    private int lastVirtualW;
    private int lastVirtualH;

    public World(int width, int height, WorldCoordinator coordinate) {
        this.width = width;
        this.height = height;
        this.coordinate = coordinate;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        objects.update();

        if (cameraTarget != null) {
            centerCameraOn(cameraTarget, lastVirtualW, lastVirtualH);
        }
    }

    // ── Gestión de objetos ────────────────────────────────────────────────────

    public void add(GameObjects obj) {
        objects.add(obj);
    }

    public void remove(GameObjects obj) {
        objects.remove(obj);
    }

    public WorldObjectsContainer getObjectsContainer() {
        return objects;
    }

    // ── Cámara ────────────────────────────────────────────────────────────────

    /**
     * Centra la cámara en un objeto y lo configura como target de seguimiento.
     *
     * @param obj           objeto a seguir (generalmente el player)
     * @param virtualWidth  DisplaySettings.virtualWidth
     * @param virtualHeight DisplaySettings.virtualHeight
     */
    public void centerCameraOn(GameObjects obj, int virtualWidth, int virtualHeight) {
        this.cameraTarget = obj;
        this.lastVirtualW = virtualWidth;
        this.lastVirtualH = virtualHeight;

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

    // ── Dimensiones ───────────────────────────────────────────────────────────

    public void resize(int newWidth, int newHeight) {
        this.width = newWidth;
        this.height = newHeight;
    }

    public int getWidth()               { return width;      }
    public int getHeight()              { return height;     }
    public WorldCoordinator getCoordinate() { return coordinate; }
}
