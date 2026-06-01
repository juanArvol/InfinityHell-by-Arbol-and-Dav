package Game.World.WorldObjects.Visuals;

import Game.Engine.GameObjects;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

import java.awt.image.BufferedImage;

public class BackGround extends GameObjects {

    public BackGround(Vector2D position,
                    BufferedImage texture,
                    int width,
                    int height) {

        getTransform().setPosition(position);

        if (texture != null) {
            addComponent(new SpriteRenderer(texture));
        }

    }

    @Override
    public void update() {
        super.update();
    }
}