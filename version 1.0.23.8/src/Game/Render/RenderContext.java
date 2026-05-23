package Game.Render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class RenderContext {

    private final Graphics2D g;

    public RenderContext(Graphics g) {
        this.g = (Graphics2D) g;
    }

    public void drawImage(BufferedImage img, int x, int y, int w, int h) {
        g.drawImage(img, x, y, w, h, null);
    }

    public void drawHitbox(Rectangle r, Color color) {
        g.setColor(color);
        g.drawRect(r.x, r.y, r.width, r.height);
    }

    public void setColor(Color c) {
        g.setColor(c);
    }
    public void fillRect(int x, int y, int w, int h) {
        g.fillRect(x, y, w, h);
    }
}
