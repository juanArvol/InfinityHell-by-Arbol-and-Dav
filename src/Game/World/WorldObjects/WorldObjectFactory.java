package Game.World.WorldObjects;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Items.Savement.ItemStack;
import java.awt.image.BufferedImage;

/**
 * Fábrica de objetos del mundo.
 *
 * ── BUG CORREGIDO: x=0 hardcodeado en groundBlock() ─────────────────────
 *
 * ANTES: groundBlock() siempre pasaba x=0 como posición horizontal del bloque.
 * Esto asumía implícitamente que el suelo empieza en el borde izquierdo del
 * sector, lo cual es correcto por la misma razón que BackGroundLayer: cada
 * sector tiene coordenadas locales [0, width]. Pero el nombre "0" era un
 * literal mágico sin justificación en el código.
 *
 * AHORA: La posición x=0 está documentada explícitamente como el borde
 * izquierdo del sector (coordenadas locales). La firma agrega una variante
 * con originX explícito para mundos que necesiten suelo desplazado.
 *
 * ── NUEVO: groundBlock con originX configurable ───────────────────────────
 * Para mecánicas futuras (plataformas flotantes, sectores con offset,
 * biomas con terreno irregular) se añade la variante con originX.
 */
public final class WorldObjectFactory {

    private WorldObjectFactory() {}

    // ── Bloques de terreno (BlockWorld) ───────────────────────────────────

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

    // ── Obstáculos (Obstacle) ─────────────────────────────────────────────

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

    /**
     * Bloque de suelo que ocupa todo el ancho del sector.
     *
     * El origen X es el borde izquierdo del sector (x=0 en coords locales).
     * Esto es correcto para todos los sectores porque cada sector tiene
     * su propio espacio de coordenadas local [0, worldWidth].
     *
     * @param worldWidth  ancho del sector (el bloque lo cubre completamente)
     * @param groundY     posición Y del borde superior del suelo
     * @param groundHeight alto del bloque de suelo
     * @param texture     textura a aplicar
     */
    public static BlockWorld groundBlock(int worldWidth,
                                          int groundY,
                                          int groundHeight,
                                          BufferedImage texture) {
        // x=0 es explícitamente el borde izquierdo del sector (coordenadas locales).
        return block(new Vector2D(0, groundY), texture, worldWidth, groundHeight);
    }

    /**
     * Bloque de suelo con posición X configurable.
     *
     * Usar cuando el suelo no debe empezar en el borde izquierdo del sector:
     * plataformas flotantes, terrenos con desplazamiento, sectores especiales.
     *
     * @param originX     posición X del borde izquierdo del bloque
     * @param blockWidth  ancho del bloque (puede ser < worldWidth)
     * @param groundY     posición Y del borde superior del suelo
     * @param groundHeight alto del bloque
     * @param texture     textura a aplicar
     */
    public static BlockWorld groundBlock(int originX,
                                          int blockWidth,
                                          int groundY,
                                          int groundHeight,
                                          BufferedImage texture) {
        return block(new Vector2D(originX, groundY), texture, blockWidth, groundHeight);
    }

    /**
     * Bloque de suelo con material configurable.
     */
    public static BlockWorld groundBlock(int worldWidth,
                                          int groundY,
                                          int groundHeight,
                                          BufferedImage texture,
                                          SurfaceMaterial material) {
        return block(new Vector2D(0, groundY), texture, worldWidth, groundHeight, material);
    }

    // ── WorldItem ─────────────────────────────────────────────────────────

    /**
     * Crea un WorldItem en el mundo (ítem recogible).
     *
     * @param position  posición en el mundo
     * @param stack     ítem que representa
     * @param icon      sprite visible (puede ser null — usa el icon de ItemDefinition)
     */
    public static WorldItem worldItem(Vector2D position, ItemStack stack, BufferedImage icon) {
        BufferedImage sprite = (icon != null) ? icon : stack.getDefinition().icon;
        return new WorldItem(position, stack, sprite);
    }

    public static WorldItem worldItem(Vector2D position, ItemStack stack) {
        return worldItem(position, stack, null);
    }
}
