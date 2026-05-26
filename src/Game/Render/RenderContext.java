package Game.Render;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Contexto de render — wrapper sobre Graphics2D.
 * Centraliza todas las operaciones de dibujo para que los componentes
 * no dependan directamente de java.awt.Graphics.
 *
 * NUEVO: drawShadowEllipse() para sombras proyectadas en 2.5D.
 */
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

    /**
     * NUEVO para 2.5D: dibuja una elipse de sombra semi-transparente.
     * Usada por ShadowComponent para sombras proyectadas de objetos elevados.
     *
     * @param x     esquina superior izquierda de la elipse
     * @param y     esquina superior izquierda de la elipse
     * @param w     ancho de la elipse
     * @param h     alto de la elipse
     * @param alpha transparencia [0-255], 0=invisible, 255=opaco
     */
    public void drawShadowEllipse(int x, int y, int w, int h, int alpha) {
        Composite original = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            alpha / 255.0f
        ));
        g.setColor(new Color(0, 0, 0));
        g.fillOval(x, y, w, h);
        g.setComposite(original);
    }

    public Graphics2D getGraphics2D() {
        return g;
    }
}
