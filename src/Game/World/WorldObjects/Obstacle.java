package Game.World.WorldObjects;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.SurfaceComponent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Physics.KineticPhysics.SurfaceMaterial;
import java.awt.image.BufferedImage;

/**
 * Obstáculo estático del mundo.
 *
 * ── HRFC — World Objects extensibles ─────────────────────────────────────
 *
 * @deprecated Usar {@link WorldObject} directamente.
 *
 * Obstacle es ahora un alias de conveniencia sobre WorldObject.
 * Se mantiene para no romper {@link Game.World.Generator.Layer.Objects.ObstacleLayer}
 * ni {@link WorldObjectFactory} durante la transición.
 *
 * DIFERENCIA CON BlockWorld (pre-HRFC):
 *   Antes: Obstacle recibía coordenadas (int x, int y) en lugar de Vector2D.
 *          Esa diferencia de firma era la única razón por la que existían
 *          dos clases para el mismo concepto.
 *   Ahora: Ambos son WorldObject. Obstacle adapta la firma legacy.
 *
 * MIGRACIÓN:
 *
 *   // Antes
 *   new Obstacle(x, y, w, h, texture)
 *   new Obstacle(x, y, w, h, texture, SurfaceMaterial.ICE)
 *
 *   // Después
 *   new WorldObject(new Vector2D(x, y), texture, w, h)
 *       .withDefaultSurface()
 *
 *   new WorldObject(new Vector2D(x, y), texture, w, h)
 *       .withSurface(SurfaceMaterial.ICE)
 */
@Deprecated(forRemoval = true)
public class Obstacle extends WorldObject {

    /**
     * Constructor de compatibilidad — coordenadas enteras, material por defecto.
     *
     * @deprecated Usar {@code new WorldObject(new Vector2D(x, y), texture, w, h).withDefaultSurface()}
     */
    @Deprecated(forRemoval = true)
    public Obstacle(int x, int y, int width, int height, BufferedImage texture) {
        this(x, y, width, height, texture, SurfaceMaterial.DEFAULT);
    }

    /**
     * Constructor de compatibilidad — coordenadas enteras, material configurable.
     *
     * @deprecated Usar {@code new WorldObject(new Vector2D(x, y), texture, w, h).withSurface(material)}
     */
    @Deprecated(forRemoval = true)
    public Obstacle(int x, int y, int width, int height,
                    BufferedImage texture, SurfaceMaterial material) {
        super(new Vector2D(x, y), texture, width, height, CollisionProfile.WORLD);
        addComponent(new SurfaceComponent(material));
    }
}
