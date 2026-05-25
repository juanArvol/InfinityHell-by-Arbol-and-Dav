package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.CollisionProfile;
import Game.World.Surface.SurfaceMaterial;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Bloque estático del mundo.
 *
 * Implementa SurfaceMaterial para que CollisionsSystem pueda leer
 * la fricción/drag de este bloque cuando un objeto aterriza sobre él.
 *
 * Para crear un bloque de hielo:
 *   new BlockWorld(pos, texture, w, h, SurfaceMaterial.ICE)
 *
 * Para crear un bloque custom:
 *   new BlockWorld(pos, texture, w, h, new SurfaceMaterial() {
 *       public double getFriction() { return 0.3; }
 *       public double getDrag()     { return 0.95; }
 *   });
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
            addComponent(new SpriteRenderer(texture, SizeSyncMode.COLLIDER_TO_SPRITE));
        }
    }

    // ── SurfaceMaterial ───────────────────────────────────────────────────

    @Override
    public double getFriction() { return material.getFriction(); }

    @Override
    public double getDrag()     { return material.getDrag(); }

    @Override
    public void update() {
        super.update();
    }
}
