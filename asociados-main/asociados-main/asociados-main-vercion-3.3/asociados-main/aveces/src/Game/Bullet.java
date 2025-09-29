package Game;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import math.Vector2D;

public class Bullet extends GameObjects {

    private double velocidadX; 
    private double velocidadY;
    
    public Bullet(Vector2D position, BufferedImage texture, double velocidadX, double velocidadY) {
        super(position, texture);
        this.velocidadX = velocidadX;
        this.velocidadY = velocidadY;
    }
    
    @Override
    public void update() {
        position.setX(position.getX() + velocidadX);
        position.setY(position.getY() + velocidadY);
        
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(texture, (int) position.getX(), (int) position.getY(), null);
    }

    @Override
    public Rectangle getBounds() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
