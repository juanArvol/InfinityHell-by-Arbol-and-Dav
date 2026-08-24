package Game.World.WorldObjects;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import Game.Items.Savement.ItemStack;
import java.awt.image.BufferedImage;

/**
 * Fábrica de objetos del mundo.
 *
 * ── HRFC — World Objects extensibles ─────────────────────────────────────
 *
 * CAMBIOS:
 *   Los métodos block() y obstacle() ahora retornan WorldObject en lugar de
 *   sus subclases concretas. Los tipos de retorno legacy (BlockWorld, Obstacle)
 *   se mantienen como métodos deprecated para compatibilidad de compilación.
 *
 *   Los métodos block() y obstacle() ya producen WorldObject con SurfaceComponent
 *   en lugar de clases que implementan SurfaceMaterial directamente.
 *   El comportamiento en CollisionsSystem FASE 0 es idéntico: la fricción
 *   se lee desde SurfaceComponent, no desde el tipo de la clase.
 *
 * NUEVOS MÉTODOS:
 *   worldObject()  → WorldObject configurable, firma canónica.
 *   dynamicObject() → WorldObject con CollisionProfile.WORLD_DYNAMIC.
 *
 * MIGRACIÓN:
 *
 *   // Antes — terreno normal
 *   WorldObjectFactory.groundBlock(w, y, h, tex)
 *   → retorna BlockWorld
 *
 *   // Ahora — idéntico en comportamiento, tipo más general
 *   WorldObjectFactory.groundBlock(w, y, h, tex)
 *   → retorna WorldObject (con SurfaceComponent.DEFAULT)
 *
 *   // Caja empujable destructible — nuevo
 *   WorldObjectFactory.dynamicObject(pos, tex, 40, 40)
 *       .withHealth(50)
 *       .withPhysics(new Physics2DComponent(cratePhysics))
 *       .withPushable(0.8)
 *       .withSurface(SurfaceMaterial.DEFAULT);
 */
public final class WorldObjectFactory {

    private WorldObjectFactory() {}

    // ── WorldObject — API canónica nueva ─────────────────────────────────

    /**
     * Crea un WorldObject estático (CollisionProfile.WORLD).
     * Punto de entrada canónico para terreno y obstáculos estáticos.
     *
     * @param position posición en el mundo
     * @param texture  sprite del objeto. Null = solo colisión.
     * @param width    ancho en píxeles
     * @param height   alto en píxeles
     * @return WorldObject listo para configurar via métodos fluent
     */
    public static WorldObject worldObject(Vector2D position,
                                          BufferedImage texture,
                                          int width,
                                          int height) {
        return new WorldObject(position, texture, width, height, CollisionProfile.WORLD);
    }

    /**
     * Crea un WorldObject estático con perfil y material de superficie explícitos.
     *
     * @param position posición en el mundo
     * @param texture  sprite del objeto
     * @param width    ancho en píxeles
     * @param height   alto en píxeles
     * @param profile  perfil de colisión
     * @param material propiedades de superficie. Null = sin SurfaceComponent.
     * @return WorldObject configurado con SurfaceComponent si material != null
     */
    public static WorldObject worldObject(Vector2D position,
                                          BufferedImage texture,
                                          int width,
                                          int height,
                                          CollisionProfile profile,
                                          SurfaceMaterial material) {
        WorldObject obj = new WorldObject(position, texture, width, height, profile);
        if (material != null) obj.withSurface(material);
        return obj;
    }

    /**
     * Crea un WorldObject dinámico (CollisionProfile.WORLD_DYNAMIC).
     * Punto de entrada para objetos del mundo que pueden moverse o ser empujados.
     *
     * Configuración habitual de una caja empujable:
     * <pre>
     *   WorldObjectFactory.dynamicObject(pos, tex, 40, 40)
     *       .withHealth(50)
     *       .withPhysics(new Physics2DComponent(cratePhysics))
     *       .withPushable(0.8)
     *       .withSurface(SurfaceMaterial.DEFAULT);
     * </pre>
     *
     * @param position posición en el mundo
     * @param texture  sprite del objeto
     * @param width    ancho en píxeles
     * @param height   alto en píxeles
     * @return WorldObject con perfil WORLD_DYNAMIC listo para configurar
     */
    public static WorldObject dynamicObject(Vector2D position,
                                             BufferedImage texture,
                                             int width,
                                             int height) {
        return new WorldObject(position, texture, width, height, CollisionProfile.WORLD_DYNAMIC);
    }

    // ── Bloques de terreno — API existente, retorna WorldObject ──────────

    /**
     * Crea un bloque de terreno estático.
     * Retorna WorldObject con SurfaceComponent.DEFAULT.
     *
     * @param position posición del borde superior-izquierdo
     * @param texture  textura del bloque
     * @param width    ancho
     * @param height   alto
     * @return WorldObject con material de superficie por defecto
     */
    public static WorldObject block(Vector2D position,
                                    BufferedImage texture,
                                    int width,
                                    int height) {
        return new WorldObject(position, texture, width, height, CollisionProfile.WORLD)
                .withDefaultSurface();
    }

    /**
     * Crea un bloque de terreno con material de superficie configurable.
     *
     * @param position posición del borde superior-izquierdo
     * @param texture  textura del bloque
     * @param width    ancho
     * @param height   alto
     * @param material propiedades de superficie (fricción, drag, etc.)
     * @return WorldObject con SurfaceComponent del material dado
     */
    public static WorldObject block(Vector2D position,
                                    BufferedImage texture,
                                    int width,
                                    int height,
                                    SurfaceMaterial material) {
        return new WorldObject(position, texture, width, height, CollisionProfile.WORLD)
                .withSurface(material);
    }

    // ── Obstáculos — API existente, retorna WorldObject ──────────────────

    /**
     * Crea un obstáculo estático con coordenadas enteras.
     *
     * @param x       posición X
     * @param y       posición Y
     * @param width   ancho
     * @param height  alto
     * @param texture textura del obstáculo
     * @return WorldObject con material de superficie por defecto
     */
    public static WorldObject obstacle(int x, int y,
                                       int width, int height,
                                       BufferedImage texture) {
        return new WorldObject(new Vector2D(x, y), texture, width, height, CollisionProfile.WORLD)
                .withDefaultSurface();
    }

    /**
     * Crea un obstáculo estático con material configurable.
     *
     * @param x       posición X
     * @param y       posición Y
     * @param width   ancho
     * @param height  alto
     * @param texture textura del obstáculo
     * @param material propiedades de superficie
     * @return WorldObject con SurfaceComponent del material dado
     */
    public static WorldObject obstacle(int x, int y,
                                       int width, int height,
                                       BufferedImage texture,
                                       SurfaceMaterial material) {
        return new WorldObject(new Vector2D(x, y), texture, width, height, CollisionProfile.WORLD)
                .withSurface(material);
    }

    // ── Bloques de suelo — API existente ─────────────────────────────────

    /**
     * Bloque de suelo que ocupa todo el ancho del sector.
     *
     * El origen X es el borde izquierdo del sector (x=0 en coords locales).
     * Cada sector tiene su propio espacio de coordenadas local [0, worldWidth].
     *
     * @param worldWidth  ancho del sector (el bloque lo cubre completamente)
     * @param groundY     posición Y del borde superior del suelo
     * @param groundHeight alto del bloque de suelo
     * @param texture     textura a aplicar
     * @return WorldObject con material de superficie por defecto
     */
    public static WorldObject groundBlock(int worldWidth,
                                           int groundY,
                                           int groundHeight,
                                           BufferedImage texture) {
        return block(new Vector2D(0, groundY), texture, worldWidth, groundHeight);
    }

    /**
     * Bloque de suelo con posición X configurable.
     *
     * @param originX     posición X del borde izquierdo del bloque
     * @param blockWidth  ancho del bloque
     * @param groundY     posición Y del borde superior del suelo
     * @param groundHeight alto del bloque
     * @param texture     textura a aplicar
     * @return WorldObject con material de superficie por defecto
     */
    public static WorldObject groundBlock(int originX,
                                           int blockWidth,
                                           int groundY,
                                           int groundHeight,
                                           BufferedImage texture) {
        return block(new Vector2D(originX, groundY), texture, blockWidth, groundHeight);
    }

    /**
     * Bloque de suelo con material de superficie configurable.
     *
     * @param worldWidth  ancho del sector
     * @param groundY     posición Y del borde superior
     * @param groundHeight alto del bloque
     * @param texture     textura a aplicar
     * @param material    propiedades de superficie (hielo, barro, etc.)
     * @return WorldObject con SurfaceComponent del material dado
     */
    public static WorldObject groundBlock(int worldWidth,
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
        BufferedImage sprite = (icon != null) ? icon : stack.getDefinition().getIcon();
        return new WorldItem(position, stack, sprite);
    }

    public static WorldItem worldItem(Vector2D position, ItemStack stack) {
        return worldItem(position, stack, null);
    }
}
