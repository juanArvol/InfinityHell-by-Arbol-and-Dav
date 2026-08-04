package Game.World.WorldObjects.Visuals;

import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import java.awt.image.BufferedImage;

public class BackGround extends GameObjects {

    public BackGround(Vector2D position,
                    BufferedImage texture,
                    int width,
                    int height) {

        getTransform().setPosition(position);

        if (texture != null) {
            addComponent(new SpriteRendererComponent(texture));
        }

    }

    @Override
    public void update() {
        super.update();
    }
}