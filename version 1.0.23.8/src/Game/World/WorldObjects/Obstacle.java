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

public class Obstacle extends GameObjects implements SurfaceMaterial {

    private final SurfaceMaterial material;

    public Obstacle(int x, int y, int width, int height, BufferedImage texture) {
        this(x, y, width, height, texture, SurfaceMaterial.DEFAULT);
    }

    public Obstacle(int x, int y, int width, int height,
                    BufferedImage texture, SurfaceMaterial material) {

        this.material = material;
        getTransform().setPosition(new Vector2D(x, y));

        ColliderComponent collider = new ColliderComponent(width, height, CollisionProfile.WORLD);
        addComponent(collider);

        addComponent(new HitBoxComponent(Color.RED));

        if (texture != null) {
            addComponent(new SpriteRenderer(texture, SizeSyncMode.COLLIDER_TO_SPRITE));
        }
    }

    @Override public double getFriction() { return material.getFriction(); }
    @Override public double getDrag()     { return material.getDrag(); }

    @Override
    public void update() { super.update(); }
}
