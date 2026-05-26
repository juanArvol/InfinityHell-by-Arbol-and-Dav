package Game.World.Core;

import Game.World.Generator.WorldGenerator;
import Game.Engine.GameObjects;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona los mundos del juego, su caché y las transiciones entre ellos.
 *
 * FIX BUG-007: en el original, update() comparaba el tamaño del mundo con
 * el tamaño de la pantalla y llamaba world.resize() si diferían, escalando
 * las posiciones de TODOS los objetos cada frame. Esto hacía que el jugador
 * y enemigos se teleportaran continuamente.
 *
 * Solución: el WorldManager ya NO escala posiciones de objetos.
 * El mundo tiene coordenadas lógicas fijas. La cámara y el render
 * se adaptan al tamaño de pantalla, no los objetos.
 *
 * FIX REFACTOR DISPLAY:
 *  - draw() recibe Graphics2D (framebuffer virtual) en lugar de Graphics.
 *    Necesario porque World.draw() ahora requiere Graphics2D para
 *    construir RenderContext correctamente.
 */
public class WorldManager {

    private static WorldManager instance;

    private final WorldCache    cache     = new WorldCache();
    private final WorldGenerator generator = new WorldGenerator();

    private WorldCoordinator currentCoord;

    private int logicalWidth;
    private int logicalHeight;

    private WorldManager(int width, int height) {
        this.logicalWidth  = width;
        this.logicalHeight = height;
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
            World world = generator.generate(logicalWidth, logicalHeight, currentCoord);
            cache.put(world);
        }
        return cache.get(currentCoord);
    }

    /**
     * FIX BUG-007: ya NO escala posiciones de objetos al actualizar.
     * El tamaño de pantalla solo se usa para centrar la cámara.
     */
    public void update(int screenWidth, int screenHeight) {
        World world = getCurrentWorld();
        world.update();
        handleTransfers(world);
    }

    /**
     * FIX REFACTOR DISPLAY: recibe Graphics2D en lugar de Graphics.
     * Delega a World.draw(Graphics2D) que construye RenderContext correctamente.
     *
     * @param g Graphics2D del framebuffer virtual (de DisplayManager.beginFrame())
     */
    public void draw(Graphics2D g) {
        getCurrentWorld().draw(g);
    }

    /**
     * Resize del mundo lógico (solo para regenerar mundos futuros).
     * NO mueve objetos existentes.
     */
    public void resize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) return;
        this.logicalWidth  = newWidth;
        this.logicalHeight = newHeight;
        // No escalar objetos existentes — FIX BUG-007
    }

    private void handleTransfers(World world) {
        List<GameObjects> toTransfer = new ArrayList<>();

        for (var obj : world.getObjectsContainer().getObjects()) {
            var pos = obj.getTransform().getPosition();
            if (pos.getX() < 0 || pos.getX() > logicalWidth ||
                pos.getY() < 0 || pos.getY() > logicalHeight) {
                toTransfer.add(obj);
            }
        }

        for (var obj : toTransfer) {
            var pos = obj.getTransform().getPosition();

            int dx = 0, dy = 0;
            if (pos.getX() < 0)             dx = -1;
            else if (pos.getX() >= logicalWidth)  dx = 1;
            if (pos.getY() < 0)             dy = -1;
            else if (pos.getY() >= logicalHeight) dy = 1;

            WorldCoordinator nextCoord = new WorldCoordinator(
                    currentCoord.x() + dx,
                    currentCoord.y() + dy
            );

            if (!cache.contains(nextCoord)) {
                cache.put(generator.generate(logicalWidth, logicalHeight, nextCoord));
            }

            World nextWorld = cache.get(nextCoord);

            double newX = pos.getX();
            double newY = pos.getY();
            if (dx != 0) newX = (dx > 0) ? newX - logicalWidth  : newX + logicalWidth;
            if (dy != 0) newY = (dy > 0) ? newY - logicalHeight : newY + logicalHeight;

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
        cache.put(generator.generate(logicalWidth, logicalHeight, currentCoord));
    }
}
