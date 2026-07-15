package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;
import java.awt.image.BufferedImage;

/**
 * SpriteRenderer — dibuja un sprite en la posición del objeto.
 *
 * ── HRFC-002: SOPORTE DUAL ────────────────────────────────────────────────
 * Acepta tanto SpriteHandle (nuevo sistema) como BufferedImage (compatibilidad).
 * Durante la migración ambos modos coexisten. El objetivo final es que todo
 * el Gameplay use SpriteHandle y BufferedImage desaparezca de los constructores.
 *
 * Modo SpriteHandle:
 *   El frame actual es resuelto por el handle. AnimationController llama
 *   setCurrentFrame() cada tick para actualizar el frame animado.
 *
 * Modo BufferedImage (legacy):
 *   Comportamiento idéntico al anterior — se mantiene para no romper
 *   BlockWorld, Obstacle y Bullet que todavía pasan BufferedImage.
 *
 * ── SizeSyncMode ─────────────────────────────────────────────────────────
 * Sin cambios. El sync collider↔sprite sigue funcionando igual en start().
 *
 * ── RENDER ────────────────────────────────────────────────────────────────
 * render() resuelve el frame en este orden:
 *   1. currentFrame (seteado por AnimationController) → tiene prioridad
 *   2. legacySprite (BufferedImage pasado al constructor)
 *   3. handle.resolveDefault() (frame por defecto del SpriteHandle)
 *   Si ninguno tiene imagen, no dibuja nada.
 */
public class SpriteRenderer extends Component implements Renderable {

    // ── Estado del frame actual ───────────────────────────────────────────

    /** Frame actual (lo setea AnimationController cada tick). */
    private SpriteFrame currentFrame;

    /**
     * Handle del sprite (modo nuevo).
     * Si está presente, currentFrame se resuelve desde aquí cuando no
     * hay AnimationController activo.
     */
    private SpriteHandle handle;

    /**
     * Imagen legacy (modo compatibilidad).
     * Solo se usa si handle es null. Permite que BlockWorld, Obstacle y
     * Bullet sigan funcionando sin cambios hasta que migren a SpriteHandle.
     */
    private BufferedImage legacySprite;

    // ── Tamaño y offset de render ─────────────────────────────────────────

    private int renderWidth;
    private int renderHeight;
    private int offsetX = 0;
    private int offsetY = 0;

    private final SizeSyncMode syncMode;

    // ── Constructores ────────────────────────────────────────────────────

    /**
     * Constructor con SpriteHandle (modo nuevo — Gameplay desacoplado).
     * El frame por defecto se resuelve desde el handle.
     */
    public SpriteRenderer(SpriteHandle handle) {
        this(handle, SizeSyncMode.NONE);
    }

    /**
     * Constructor con SpriteHandle y modo de sync.
     */
    public SpriteRenderer(SpriteHandle handle, SizeSyncMode syncMode) {
        this.handle   = handle;
        this.syncMode = syncMode;
        // Inicializar tamaño desde el frame por defecto del handle
        if (handle != null && handle.isValid()) {
            SpriteFrame def = handle.resolveDefault();
            renderWidth  = def.getWidth();
            renderHeight = def.getHeight();
        }
    }

    /**
     * Constructor con BufferedImage (modo legacy — compatibilidad).
     * Usado por BlockWorld, Obstacle, Bullet y MovingObjects mientras migran.
     */
    public SpriteRenderer(BufferedImage sprite) {
        this(sprite, SizeSyncMode.NONE);
    }

    /**
     * Constructor con BufferedImage y modo de sync (modo legacy).
     */
    public SpriteRenderer(BufferedImage sprite, SizeSyncMode syncMode) {
        this.legacySprite = sprite;
        this.syncMode     = syncMode;
        if (sprite != null) {
            renderWidth  = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────

    @Override
    public void start() {
        if (syncMode == SizeSyncMode.NONE) return;

        ColliderComponent col = gameObject.getComponent(ColliderComponent.class);
        if (col == null) return;

        switch (syncMode) {

            case COLLIDER_TO_SPRITE -> {
                // El collider copia el tamaño del sprite.
                int w = renderWidth;
                int h = renderHeight;
                if (w > 0 && h > 0) col.setSize(w, h);
            }

            case SPRITE_TO_COLLIDER -> {
                renderWidth  = col.getWidth();
                renderHeight = col.getHeight();
            }

            case SPRITE_TO_COLLIDER_WITH_OFFSET -> {
                renderWidth  = col.getWidth();
                renderHeight = col.getHeight();
                offsetX      = col.getOffsetX();
                offsetY      = col.getOffsetY();
            }
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    /**
     * Dibuja el frame actual en la posición de pantalla del objeto.
     *
     * Resolución del frame (en orden de prioridad):
     *   1. currentFrame (animación activa vía AnimationController)
     *   2. legacySprite (BufferedImage directa — modo compatibilidad)
     *   3. handle.resolveDefault() (frame por defecto del SpriteHandle)
     */
    @Override
    public void render(RenderContext ctx, RenderCamera camera) {
        BufferedImage imageToDraw = resolveImage();
        if (imageToDraw == null) return;

        var pos = gameObject.getTransform().getPosition();
        int x = (int)(pos.getX() - camera.getX()) + offsetX;
        int y = (int)(pos.getY() - camera.getY()) + offsetY;

        ctx.drawImage(imageToDraw, x, y, renderWidth, renderHeight);
    }

    /**
     * Resuelve la imagen a dibujar este frame.
     * Orden: currentFrame → legacySprite → handle default.
     */
    private BufferedImage resolveImage() {
        // 1. Frame animado activo
        if (currentFrame != null && currentFrame.isValid()) {
            return currentFrame.getImage();
        }
        // 2. Imagen legacy directa
        if (legacySprite != null) {
            return legacySprite;
        }
        // 3. Frame por defecto del handle
        if (handle != null && handle.isValid()) {
            SpriteFrame def = handle.resolveDefault();
            return def.isValid() ? def.getImage() : null;
        }
        return null;
    }

    // ── API pública ──────────────────────────────────────────────────────

    /**
     * Actualiza el frame actual (llamado por AnimationController cada tick).
     * Reemplaza tanto el legacySprite como el frame por defecto del handle
     * mientras el AnimationController esté activo.
     */
    public void setCurrentFrame(SpriteFrame frame) {
        this.currentFrame = frame;
        // Actualizar tamaño si estamos en modo NONE y el frame tiene dimensiones
        if (syncMode == SizeSyncMode.NONE && frame != null && frame.isValid()) {
            // Solo actualizar si no hay tamaño forzado (renderWidth/Height en 0)
            if (renderWidth == 0)  renderWidth  = frame.getWidth();
            if (renderHeight == 0) renderHeight = frame.getHeight();
        }
    }

    /**
     * Cambia el sprite con BufferedImage directa (compatibilidad legacy).
     * Úsalo solo desde código que todavía no migró a AnimationController.
     */
    public void setSprite(BufferedImage sprite) {
        this.legacySprite = sprite;
        this.currentFrame = null; // el legacy toma precedencia
        if (syncMode == SizeSyncMode.NONE && sprite != null) {
            renderWidth  = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }
    }

    /** Cambia el handle del sprite. Resetea currentFrame. */
    public void setHandle(SpriteHandle handle) {
        this.handle       = handle;
        this.currentFrame = null;
        if (handle != null && handle.isValid()) {
            SpriteFrame def = handle.resolveDefault();
            if (renderWidth  == 0) renderWidth  = def.getWidth();
            if (renderHeight == 0) renderHeight = def.getHeight();
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

    // ── Getters ──────────────────────────────────────────────────────────

    /**
     * Devuelve la BufferedImage actualmente activa (legacy + compatibilidad).
     * Preferir resolveDefault() del handle cuando sea posible.
     */
    public BufferedImage getSprite() {
        if (currentFrame != null && currentFrame.isValid()) return currentFrame.getImage();
        if (legacySprite != null) return legacySprite;
        if (handle != null && handle.isValid()) return handle.resolveDefault().getImage();
        return null;
    }

    public SpriteHandle    getHandle()       { return handle;       }
    public SpriteFrame     getCurrentFrame() { return currentFrame; }
    public int             getRenderWidth()  { return renderWidth;  }
    public int             getRenderHeight() { return renderHeight; }
    public SizeSyncMode    getSyncMode()     { return syncMode;     }
}
