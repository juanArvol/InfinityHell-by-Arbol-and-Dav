package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Render.Camera;
import Game.Render.RenderContext;
import Graficos.Renderable;

import java.awt.image.BufferedImage;

/**
 * Dibuja un sprite en la posición del objeto.
 *
 * ── Qué se simplificó ────────────────────────────────────────────────────
 * Se eliminó SizeSyncMode y la sincronización bidireccional con HitBoxComponent.
 * Esa lógica causaba que cambiar el sprite cambiara el collider, y viceversa,
 * creando dependencias invisibles difíciles de rastrear.
 *
 * Ahora SpriteRenderer solo dibuja. El tamaño del collider se define
 * explícitamente en el constructor del objeto (Player, Enemy, BlockWorld...).
 *
 * Para animar: llamar setSprite() con el frame actual desde el componente
 * de animación (PlayerRenderer, EnemyRenderer, etc.).
 */
public class SpriteRenderer extends Component implements Renderable {

    private BufferedImage sprite;
    private int renderWidth;
    private int renderHeight;
    private int offsetX = 0;
    private int offsetY = 0;

    public SpriteRenderer(BufferedImage sprite) {
        this.sprite = sprite;
        if (sprite != null) {
            renderWidth  = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }
    }

    @Override
    public void render(RenderContext ctx, Camera camera) {
        if (sprite == null) return;

        var pos = gameObject.getTransform().getPosition();
        int x = (int)(pos.getX() - camera.getX()) + offsetX;
        int y = (int)(pos.getY() - camera.getY()) + offsetY;

        ctx.drawImage(sprite, x, y, renderWidth, renderHeight);
    }

    /** Cambia el sprite (para animaciones). El tamaño se actualiza automáticamente. */
    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
        if (sprite != null) {
            renderWidth  = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }
    }

    /** Fuerza un tamaño de render distinto al del sprite (escalado). */
    public void setRenderSize(int w, int h) {
        this.renderWidth  = w;
        this.renderHeight = h;
    }

    /** Offset visual respecto a la posición del objeto (sin afectar el collider). */
    public void setOffset(int ox, int oy) {
        this.offsetX = ox;
        this.offsetY = oy;
    }

    public BufferedImage getSprite() { return sprite; }
    public int getRenderWidth()      { return renderWidth; }
    public int getRenderHeight()     { return renderHeight; }
}
