package Game;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import math.Vector2D;

public abstract class GameObjects {
    protected BufferedImage texture;
    protected Vector2D position;

    public GameObjects(Object position, BufferedImage texture){
        this.position = (Vector2D) position;
        this.texture = texture;
    }

    public void onCollision(GameObjects other) {
        // por defecto no hace nada
    }

    public abstract void update();
    public abstract void draw(Graphics g);

    public Vector2D getPosition() {
        return position;
    }
    public void setPosition(Vector2D position) {
        this.position = position;
    }


    public Rectangle createBounds(int offsetX, int offsetY, int width, int height) {
        return new Rectangle(
            (int) (position.getX() + offsetX),
            (int) (position.getY() + offsetY),
            width,
            height
        );
    }
        public void drawHitbox(Graphics g) {
        Rectangle r = getBounds();
        g.setColor(java.awt.Color.RED);
        g.drawRect(r.x, r.y, r.width, r.height);
    }
    public abstract Rectangle getBounds();
}
