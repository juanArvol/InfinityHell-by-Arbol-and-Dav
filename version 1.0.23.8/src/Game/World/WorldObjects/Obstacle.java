package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Filter.CollisionProfile;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Obstacle extends GameObjects {

    public Obstacle(
            int x,
            int y,
            int width,
            int height,
            BufferedImage texture
    ) {

        getTransform().setPosition(new Vector2D(x, y));

        // ================= COLLIDER =================

        ColliderComponent collider =
                new ColliderComponent(
                        width,
                        height,
                        CollisionProfile.WORLD
                );

        addComponent(collider);

        // ================= DEBUG =================

        HitBoxComponent hitBox =
                new HitBoxComponent(Color.RED);

        addComponent(hitBox);

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