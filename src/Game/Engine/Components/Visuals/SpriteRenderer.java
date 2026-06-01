package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.UI.POV.Camera;
import Game.UI.POV.RenderContext;
import Graficos.Renderable;

import java.awt.image.BufferedImage;

/**
 * Dibuja un sprite en la posición del objeto.
 *
 * ── SizeSyncMode ───────────────────────────────────────────────────────────
 *
 * Permite declarar explícitamente cómo se relacionan el sprite y el collider.
 * La sincronización se aplica UNA SOLA VEZ en start(), cuando el componente
 * ya tiene acceso al gameObject y a su ColliderComponent.
 *
 * No hay dependencias circulares ni efectos secundarios ocultos.
 * El resultado es predecible: lo que configurás en el constructor es lo
 * que obtenés en pantalla.
 *
 * Ejemplos de uso:
 *
 *   // Bloque de mundo: hitbox = tamaño del sprite
 *   new SpriteRenderer(texture, SizeSyncMode.COLLIDER_TO_SPRITE)
 *
 *   // Jugador: sprite escala al tamaño de la hitbox (15x24)
 *   new SpriteRenderer(texture, SizeSyncMode.SPRITE_TO_COLLIDER)
 *
 *   // Jugador con collider offseteado: sprite alineado al collider
 *   new SpriteRenderer(texture, SizeSyncMode.SPRITE_TO_COLLIDER_WITH_OFFSET)
 *
 *   // Control total manual
 *   new SpriteRenderer(texture)  // o SizeSyncMode.NONE
 *   renderer.setRenderSize(32, 48);
 *   renderer.setOffset(-4, 0);
 */
public class SpriteRenderer extends Component implements Renderable {

    private BufferedImage sprite;
    private int renderWidth;
    private int renderHeight;
    private int offsetX = 0;
    private int offsetY = 0;

    private final SizeSyncMode syncMode;

    // ── Constructores ────────────────────────────────────────────────────

    /** Sin sincronización (control manual). */
    public SpriteRenderer(BufferedImage sprite) {
        this(sprite, SizeSyncMode.NONE);
    }

    /** Con modo de sincronización declarativo. */
    public SpriteRenderer(BufferedImage sprite, SizeSyncMode syncMode) {
        this.sprite   = sprite;
        this.syncMode = syncMode;
        if (sprite != null) {
            renderWidth  = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────

    /**
     * Se llama una vez al agregar el componente al objeto.
     * Aplica la sincronización si el modo no es NONE.
     * Requiere que ColliderComponent ya esté en el mismo objeto.
     */
    @Override
    public void start() {
        if (syncMode == SizeSyncMode.NONE) return;

        ColliderComponent col = gameObject.getComponent(ColliderComponent.class);
        if (col == null) return;  // Sin collider, no hay nada que sincronizar.

        switch (syncMode) {

            case COLLIDER_TO_SPRITE -> {
                // El collider copia el tamaño del sprite.
                // Útil para bloques: hitbox exacta al sprite.
                if (sprite != null) {
                    col.setSize(sprite.getWidth(), sprite.getHeight());
                }
            }

            case SPRITE_TO_COLLIDER -> {
                // El sprite se escala al tamaño del collider.
                // Útil cuando la hitbox define el gameplay y el sprite debe seguirla.
                renderWidth  = col.getWidth();
                renderHeight = col.getHeight();
            }

            case SPRITE_TO_COLLIDER_WITH_OFFSET -> {
                // Como SPRITE_TO_COLLIDER, pero además alinea el sprite al offset del collider.
                renderWidth  = col.getWidth();
                renderHeight = col.getHeight();
                offsetX      = col.getOffsetX();
                offsetY      = col.getOffsetY();
            }
        }
    }

    // ── Render ───────────────────────────────────────────────────────────

    @Override
    public void render(RenderContext ctx, Camera camera) {
        if (sprite == null) return;

        var pos = gameObject.getTransform().getPosition();
        int x = (int)(pos.getX() - camera.getX()) + offsetX;
        int y = (int)(pos.getY() - camera.getY()) + offsetY;

        ctx.drawImage(sprite, x, y, renderWidth, renderHeight);
    }

    // ── API pública ──────────────────────────────────────────────────────

    /**
     * Cambia el sprite (para animaciones).
     * El tamaño de render NO cambia automáticamente si hay un syncMode activo,
     * para que las animaciones no rompan el tamaño configurado.
     * Si necesitás que el tamaño cambie con el sprite, llamá setRenderSize() explícitamente.
     */
    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
        // Solo actualizar tamaño si es NONE (sin sync), para no romper lo configurado en start().
        if (syncMode == SizeSyncMode.NONE && sprite != null) {
            renderWidth  = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }
    }

    /** Fuerza un tamaño de render (override de cualquier sync anterior). */
    public void setRenderSize(int w, int h) {
        this.renderWidth  = w;
        this.renderHeight = h;
    }

    /** Offset visual respecto a la posición del objeto (sin afectar el collider). */
    public void setOffset(int ox, int oy) {
        this.offsetX = ox;
        this.offsetY = oy;
    }

    public BufferedImage getSprite()  { return sprite; }
    public int getRenderWidth()       { return renderWidth; }
    public int getRenderHeight()      { return renderHeight; }
    public SizeSyncMode getSyncMode() { return syncMode; }

}
