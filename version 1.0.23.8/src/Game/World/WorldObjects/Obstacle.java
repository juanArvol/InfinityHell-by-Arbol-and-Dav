package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Colisions.CollisionVisitor;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Components.Visuals.SpriteRenderer;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Obstacle extends GameObjects {
    private HitBoxComponent hitBox;
    private SpriteRenderer renderer;

    public Obstacle(int x,
                    int y,
                    int width,
                    int height,
                    BufferedImage texture) {

        getTransform().setPosition(new Vector2D(x, y));

        hitBox = new HitBoxComponent(width, height, 0, 0);
        hitBox.setVisible(true);
        hitBox.setDebugColor(Color.RED);
        addComponent(hitBox);

        if (texture != null) {
            renderer = new SpriteRenderer(texture);
            renderer.setSyncMode(SizeSyncMode.BIDIRECTIONAL);
            addComponent(renderer);
        }
        addComponent(new ColliderComponent());
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void acceptVisitor(CollisionVisitor visitor) {
        visitor.visit(this);
    }
}