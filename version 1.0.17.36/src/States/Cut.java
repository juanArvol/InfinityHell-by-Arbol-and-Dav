package States;

import Game.GameObjects;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import math.Vector2D;

public class Cut extends GameObjects {

    public Cut(Vector2D position, BufferedImage texture) {
        super(position, texture);
        //TODO Auto-generated constructor stub
    }

    @Override
    public void update() {
    }

    @Override
    public void draw(Graphics g) {

        g.setColor(Color.BLACK);
        //g.drawImage(Assets.mondongo,10,10,null);
        g.dispose();
    }

    @Override
    public Rectangle getBounds() {
        return createBounds(0, 0, 50, 160);
    }
}
