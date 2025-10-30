package Game;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import math.Vector2D;

public abstract class MovingObjects extends GameObjects {
    protected Vector2D velocity;

    public MovingObjects(Vector2D position, BufferedImage texture) {
        super(position, texture);
        this.velocity = new Vector2D();
    }

    public void updatePosition() {
        position = position.add(velocity);
    }

    public Rectangle getBounds() {
        return createBounds(0, 0, texture.getWidth(), texture.getHeight());
    }
    public Vector2D getCenter() {
    double centerX = position.getX() + texture.getWidth() / 2.0;
    double centerY = position.getY() + texture.getHeight() / 2.0;
    return new Vector2D(centerX, centerY);
}

    public abstract void onCollision(GameObjects other);
}
