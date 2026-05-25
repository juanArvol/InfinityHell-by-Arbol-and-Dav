package Game.Player;

import Game.Engine.Component;
import Game.Engine.Components.PhysicsComponent;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Graficos.Player.PlayerAssets;

import java.awt.image.BufferedImage;

public class PlayerRenderer extends Component {

    private PlayerState state;

    private int frame;
    private int animTick;

    private SpriteRenderer baseRenderer;

    public PlayerRenderer(PlayerState state) {
        this.state = state;
    }

    @Override
    public void start() {
        // Obtener el SpriteRenderer base agregado por MovingObjects
        baseRenderer = gameObject.getComponent(SpriteRenderer.class);
    }

    @Override
    public void update() {

        if (baseRenderer == null) return;

        double velocityX =
                gameObject
                        .getComponent(PhysicsComponent.class)
                        .getPhysics()
                        .getVelocity()
                        .getX();

        boolean isMoving = Math.abs(velocityX) > 0.5;

        BufferedImage spriteToUse;

        if (isMoving) {

            animTick++;
            if (animTick >= 10) {
                frame++;
                animTick = 0;
            }

            BufferedImage[] frames = state.isDer()
                    ? PlayerAssets.walkDere.getFrames()
                    : PlayerAssets.walkHiz.getFrames();

            frame %= frames.length;
            spriteToUse = frames[frame];

        } else {

            frame = 0;
            spriteToUse = PlayerAssets.idle.getSprite();
        }

        baseRenderer.setSprite(spriteToUse);
    }
}