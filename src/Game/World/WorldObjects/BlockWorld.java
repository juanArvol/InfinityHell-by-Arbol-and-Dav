package Game.World.WorldObjects;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.Entity.Components.Visuals.HitBoxComponent;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameMath.KineticPhysics.SurfaceMaterial;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Bloque estático del mundo.
 *
 * Implementa SurfaceMaterial para que CollisionsSystem pueda leer
 * las propiedades físicas de este bloque cuando un objeto aterriza sobre él.
 *
 * FIX: Se agregaron getAirControl() y getAccelScale() que faltaban tras la
 * refactorización de SurfaceMaterial de clase a interface con 4 métodos.
 * Su ausencia causaba AbstractMethodError/crash al colisionar con el suelo.
 *
 * Para crear un bloque de hielo:
 *   new BlockWorld(pos, texture, w, h, SurfaceMaterial.ICE)
 *
 * Para crear un bloque custom:
 *   new BlockWorld(pos, texture, w, h, SurfaceMaterial.of(0.3, 0.95, 0.8, 1.0));
 */
public class BlockWorld extends GameObjects implements SurfaceMaterial {

    private final SurfaceMaterial material;

    // Constructor con material por defecto (suelo normal)
    public BlockWorld(Vector2D position, BufferedImage texture, int width, int height) {
        this(position, texture, width, height, SurfaceMaterial.DEFAULT);
    }

    // Constructor con material explícito
    public BlockWorld(Vector2D position, BufferedImage texture,
                      int width, int height, SurfaceMaterial material) {

        this.material = material;
        getTransform().setPosition(position);

        // ================= COLLIDER =================

        ColliderComponent collider = new ColliderComponent(width, height, CollisionProfile.WORLD);
        addComponent(collider);

        // ================= DEBUG =================

        addComponent(new HitBoxComponent(Color.BLUE));

        // ================= RENDER =================

        if (texture != null) {
            addComponent(new SpriteRendererComponent(texture, SizeSyncMode.COLLIDER_TO_SPRITE));
        }
    }

    // ── SurfaceMaterial ───────────────────────────────────────────────────

    @Override public double getFriction()   { return material.getFriction();   }
    @Override public double getDrag()       { return material.getDrag();       }
    @Override public double getAirControl() { return material.getAirControl(); }  // FIX: faltaba
    @Override public double getAccelScale() { return material.getAccelScale(); }  // FIX: faltaba

    @Override
    public void update() {
        super.update();
    }
}
