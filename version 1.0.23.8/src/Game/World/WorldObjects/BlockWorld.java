package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Components.Visuals.SizeSyncMode;
import Game.Engine.Colisions.CollisionVisitor;
import GameMath.Vector2D;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class BlockWorld extends GameObjects {

    private HitBoxComponent hitBox;
    private SpriteRenderer renderer;
    private ColliderComponent collider;

    public BlockWorld(Vector2D position,
                      BufferedImage texture,
                      int width,
                      int height) {

        getTransform().setPosition(position);

        hitBox = new HitBoxComponent(width, height, 0, 0);
        hitBox.setVisible(true);
        hitBox.setDebugColor(Color.RED);
        addComponent(hitBox);

        if (texture != null) {
            renderer = new SpriteRenderer(texture);
            renderer.setSyncMode(SizeSyncMode.BIDIRECTIONAL);
            addComponent(renderer);
        }
        addComponent(collider);
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