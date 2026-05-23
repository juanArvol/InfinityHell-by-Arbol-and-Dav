package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.CollisionProfile;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class BlockWorld extends GameObjects {

    public BlockWorld(
            Vector2D position,
            BufferedImage texture,
            int width,
            int height
    ) {

        getTransform().setPosition(position);

        // ================= COLLIDER =================

        ColliderComponent collider =
                new ColliderComponent(
                        width,
                        height,
                        CollisionProfile.WORLD
                );

        addComponent(collider);

        // ================= DEBUG =================

        addComponent(new HitBoxComponent(Color.BLUE));

        // ================= RENDER =================

        if (texture != null) {
            addComponent(new SpriteRenderer(texture));
        }
    }

    @Override
    public void update() {
        super.update();
    }
}