package Game.Engine.Entity.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import java.awt.Color;

/**
 * Dibuja un rectángulo de color en la posición del objeto.
 *
 * ── CORRECCIÓN: offset configurable ─────────────────────────────────────
 * Problema anterior:
 *   RectRenderer calculaba la posición de pantalla correctamente (con camera
 *   offset) pero no tenía offsetX/offsetY configurables. SpriteRenderer sí
 *   los tiene. La inconsistencia hacía imposible alinear un RectRenderer con
 *   un SpriteRenderer offseteado en el mismo objeto.
 *
 * Solución:
 *   Se añaden offsetX/offsetY con la misma semántica que en SpriteRenderer:
 *   desplazamiento visual respecto a la posición del objeto, sin afectar
 *   al collider ni a la física.
 */
public class RectRendererComponent extends Component implements Renderable {

    private int   width;
    private int   height;
    private Color color;
    private int   offsetX = 0;
    private int   offsetY = 0;

    /** Sin offset (comportamiento original). */
    public RectRendererComponent(int width, int height, Color color) {
        this.width  = width;
        this.height = height;
        this.color  = color;
    }

    /** Con offset visual respecto a la posición del objeto. */
    public RectRendererComponent(int width, int height, Color color, int offsetX, int offsetY) {
        this(width, height, color);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void render(RenderContext ctx, RenderCamera camera) {
        var pos = gameObject.getTransform().getPosition();

        int x = (int)(pos.getX() - camera.getX()) + offsetX;
        int y = (int)(pos.getY() - camera.getY()) + offsetY;

        ctx.setColor(color);
        ctx.fillRect(x, y, width, height);
    }

    /** Offset visual respecto a la posición del objeto (sin afectar el collider). */
    public void setOffset(int ox, int oy) {
        this.offsetX = ox;
        this.offsetY = oy;
    }

    public void  setColor(Color c)  { this.color  = c; }
    public void  setWidth(int w)    { this.width   = w; }
    public void  setHeight(int h)   { this.height  = h; }
    public Color getColor()         { return color;  }
    public int   getWidth()         { return width;  }
    public int   getHeight()        { return height; }
    public int   getOffsetX()       { return offsetX; }
    public int   getOffsetY()       { return offsetY; }
}
