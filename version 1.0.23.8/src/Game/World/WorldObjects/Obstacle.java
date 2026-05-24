package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
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

        addComponent(new HitBoxComponent(Color.RED));

        // ================= RENDER =================

        if (texture != null) {
            // COLLIDER_TO_SPRITE: el sprite se escala al tamaño del obstáculo.
            addComponent(new SpriteRenderer(texture, SizeSyncMode.COLLIDER_TO_SPRITE));
        }
    }

    @Override
    public void update() {
        super.update();
    }
}
