package Game.World.WorldObjects;

import Game.Engine.GameMath.Physics.SurfaceMaterial;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Items.Savement.ItemStack;
import java.awt.image.BufferedImage;

/**
 * Fábrica de objetos del mundo — extendida con WorldItem.
 *
 * RETRO-COMPATIBLE: los métodos block(), obstacle(), groundBlock() son idénticos
 * al original del Backup. Solo se añade worldItem() al final.
 *
 * Si el proyecto ya tiene WorldObjectFactory.java, solo pegar el método worldItem()
 * al final de la clase existente.
 */
public final class WorldObjectFactory {

    private WorldObjectFactory() {}

    // ── Bloques de terreno (BlockWorld) — ORIGINALES ──────────────────────

    public static BlockWorld block(Vector2D position,
                                   BufferedImage texture,
                                   int width,
                                   int height) {
        return new BlockWorld(position, texture, width, height);
    }

    public static BlockWorld block(Vector2D position,
                                   BufferedImage texture,
                                   int width,
                                   int height,
                                   SurfaceMaterial material) {
        return new BlockWorld(position, texture, width, height, material);
    }

    // ── Obstáculos (Obstacle) — ORIGINALES ───────────────────────────────

    public static Obstacle obstacle(int x, int y,
                                    int width, int height,
                                    BufferedImage texture) {
        return new Obstacle(x, y, width, height, texture);
    }

    public static Obstacle obstacle(int x, int y,
                                    int width, int height,
                                    BufferedImage texture,
                                    SurfaceMaterial material) {
        return new Obstacle(x, y, width, height, texture, material);
    }

    public static BlockWorld groundBlock(int worldWidth,
                                          int groundY,
                                          int groundHeight,
                                          BufferedImage texture) {
        return block(new Vector2D(0, groundY), texture, worldWidth, groundHeight);
    }

    // ── NUEVO: WorldItem ──────────────────────────────────────────────────

    /**
     * Crea un WorldItem en el mundo (ítem recogible).
     *
     * @param position  posición en el mundo
     * @param stack     ítem que representa
     * @param icon      sprite visible (puede ser null, usa el icon de ItemDefinition)
     */
    public static WorldItem worldItem(Vector2D position, ItemStack stack, BufferedImage icon) {
        BufferedImage sprite = (icon != null) ? icon : stack.getDefinition().icon;
        return new WorldItem(position, stack, sprite);
    }

    public static WorldItem worldItem(Vector2D position, ItemStack stack) {
        return worldItem(position, stack, null);
    }
}
