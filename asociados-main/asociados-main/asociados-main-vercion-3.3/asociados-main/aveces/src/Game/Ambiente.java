package Game;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import math.Vector2D;

public class Ambiente extends GameObjects {

    private int ancho; 
    private int alto;

    public Ambiente(Vector2D position, BufferedImage texture, int ancho, int alto){
        super(position, texture);
        this.ancho = ancho;
        this.alto = alto;
    }

    @Override
    public void update() { }

    @Override
    public Rectangle getBounds() {
        return new Rectangle((int) position.getX(), (int) position.getY(), ancho, alto);
    }

    @Override
    public void draw(Graphics g) {
        
        g.drawImage(texture, (int) position.getX(), (int) position.getY(), ancho, 100, null);
        drawHitbox(g);
    }
}