package Game.World.WorldObjects;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.SurfaceComponent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import java.awt.image.BufferedImage;

/**
 * Bloque estático del mundo.
 *
 * ── HRFC — World Objects extensibles ─────────────────────────────────────
 *
 * @deprecated Usar {@link WorldObject} directamente.
 *
 * BlockWorld es ahora un alias de conveniencia sobre WorldObject.
 * Se mantiene para no romper las capas de generación ({@link Game.World.Generator.Layer.Objects.TerrainLayer})
 * ni el código existente de {@link WorldObjectFactory} durante la transición.
 *
 * MIGRACIÓN:
 *
 *   // Antes
 *   new BlockWorld(pos, texture, w, h)
 *   new BlockWorld(pos, texture, w, h, SurfaceMaterial.ICE)
 *
 *   // Después
 *   new WorldObject(pos, texture, w, h)
 *       .withDefaultSurface()
 *
 *   new WorldObject(pos, texture, w, h)
 *       .withSurface(SurfaceMaterial.ICE)
 *
 * DIFERENCIA ARQUITECTÓNICA:
 *   BlockWorld implementaba SurfaceMaterial directamente en la clase.
 *   WorldObject delega esa responsabilidad en {@link SurfaceComponent}.
 *   CollisionsSystem FASE 0 ahora resuelve surface via getComponent(SurfaceComponent.class)
 *   con fallback a instanceof SurfaceMaterial para compatibilidad temporal.
 */
@Deprecated(forRemoval = true)
public class BlockWorld extends WorldObject {

    /**
     * Constructor de compatibilidad — suelo normal.
     *
     * @deprecated Usar {@code new WorldObject(position, texture, width, height).withDefaultSurface()}
     */
    @Deprecated(forRemoval = true)
    public BlockWorld(Vector2D position, BufferedImage texture, int width, int height) {
        this(position, texture, width, height, SurfaceMaterial.DEFAULT);
    }

    /**
     * Constructor de compatibilidad — material configurable.
     *
     * @deprecated Usar {@code new WorldObject(position, texture, width, height).withSurface(material)}
     */
    @Deprecated(forRemoval = true)
    public BlockWorld(Vector2D position, BufferedImage texture,
                      int width, int height, SurfaceMaterial material) {
        super(position, texture, width, height, CollisionProfile.WORLD);
        // Capacidad de superficie via SurfaceComponent — no via implementación de clase.
        // CollisionsSystem resuelve surface via getComponent(SurfaceComponent.class);
        // si no existe, hace fallback a instanceof SurfaceMaterial (ruta legacy).
        // Al usar SurfaceComponent aquí, este objeto ya usa la ruta nueva correcta.
        addComponent(new SurfaceComponent(material));
    }
}
