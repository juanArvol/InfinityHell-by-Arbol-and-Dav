package Game;

import Game.Bullets.Bullet;
import Game.Colisions.SystemColisions.Collidable;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import math.Vector2D;

public abstract class GameObjects implements Collidable {

    protected BufferedImage texture;
    protected Vector2D position;

    public GameObjects(Vector2D position, BufferedImage texture) {
        this.position = position;
        this.texture = texture;
    }

    // Métodos abstractos base
    public abstract void update();
    public abstract void draw(Graphics g);
    public abstract Rectangle getBounds();

    // --- Métodos comunes ---
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

    // --- Colisiones generales ---
    public void onCollision(GameObjects other) {
        // por defecto no hace nada
    }

    // --- Implementación base del double dispatch ---
        @Override
    public void acceptCollision(Collidable other) {
        switch (other) {
            case Player p -> onCollisionWith(p);
            case EnimyNormal e -> onCollisionWith(e);
            case Bullet b -> onCollisionWith(b);
            case Ambiente a -> onCollisionWith(a);
            default -> onCollisionWith((GameObjects) other);
        }
    }
    // Por defecto, no hacer nada si no se sobreescriben en las subclases
    @Override public void onCollisionWith(GameObjects other) {}
    @Override public void onCollisionWith(Player player) {}
    @Override public void onCollisionWith(EnimyNormal enemy) {}
    @Override public void onCollisionWith(Bullet bullet) {}
    @Override public void onCollisionWith(Ambiente ambiente) {}
}
