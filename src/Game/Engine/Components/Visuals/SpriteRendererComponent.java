package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Engine.Components.Collisions.ColliderComponent;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import Game.Engine.RenderEngine.Culling.ViewportCuller;
import Game.Engine.RenderEngine.Sprites.Alignment;
import Game.Engine.RenderEngine.Sprites.FillMode;
import Game.Engine.RenderEngine.Sprites.FillModeRenderer;
import Game.Engine.RenderEngine.Sprites.SizeSyncMode;
import Game.Engine.RenderEngine.Strategies.ShadowStrategy;
import Game.Engine.RenderEngine.Strategies.SpriteDrawer;
import Game.Engine.RenderEngine.Transform.TransformData;
import Sprites.Core.SpriteFrame;
import Sprites.Core.SpriteHandle;
import java.awt.image.BufferedImage;

/**
 * SpriteRenderer — dibuja un sprite en la posición del objeto.
 *
 * ── HRFC-004: FillMode y Alignment ───────────────────────────────────────
 * Añadidos FillMode y Alignment para controlar cómo el sprite ocupa el área.
 *
 *   FillMode.STRETCH (default) → comportamiento anterior exacto, sin overhead.
 *   FillMode.FIT/COVER/CENTER  → escala con aspect ratio / centrado.
 *   FillMode.TILE/TILE_X/TILE_Y → repetición del sprite para llenar el área.
 *
 * Alignment (alignH, alignV) controla dónde queda el resto visual cuando
 * el sprite no llena el área exactamente (relevante en TILE, FIT, CENTER).
 *
 * ── HRFC-003: PIPELINE COMPLETO ───────────────────────────────────────────
 * En FillMode.STRETCH, el flujo sigue siendo:
 *   SpriteFrame → SpriteDrawer → TransformData → Graphics2D
 * Un único drawImage() por frame, sin overhead.
 *
 * Para todos los demás modos, FillModeRenderer gestiona la geometría.
 * TransformData (flip, alpha, tint, blend) se aplica al Graphics2D antes
 * de delegar en FillModeRenderer para que las transformaciones afecten
 * a todos los tiles/celdas uniformemente.
 *
 * ── HRFC-002: SOPORTE DUAL ────────────────────────────────────────────────
 * Acepta tanto SpriteHandle (nuevo sistema) como BufferedImage (compatibilidad).
 * El path legacy (BufferedImage directa) sigue disponible durante la migración.
 *
 * ── Culling ───────────────────────────────────────────────────────────────
 * Se verifica visibilidad antes de cualquier draw. Los sprites completamente
 * fuera del viewport se omiten sin ningún draw.
 */
public class SpriteRendererComponent extends Component implements Renderable {

    // ── Estado del frame actual ───────────────────────────────────────────

    /** Frame actual (lo setea AnimationController cada tick). */
    private SpriteFrame currentFrame;

    /** Handle del sprite (modo nuevo). */
    private SpriteHandle handle;

    /** Imagen legacy (modo compatibilidad). */
    private BufferedImage legacySprite;

    // ── Tamaño y offset de render ─────────────────────────────────────────

    private int renderWidth;
    private int renderHeight;
    private int offsetX = 0;
    private int offsetY = 0;

    private final SizeSyncMode syncMode;

    // ── HRFC-003: TransformData ───────────────────────────────────────────

    /** Transformación visual completa. IDENTITY por defecto (path más rápido). */
    private TransformData transform = TransformData.IDENTITY;

    /** Sombra 2D opcional. */
    private ShadowStrategy shadowStrategy = null;

    // ── HRFC-003: Culling ─────────────────────────────────────────────────

    private int virtualWidth  = 1280;
    private int virtualHeight = 720;

    // ── HRFC-004: FillMode y Alignment ────────────────────────────────────

    /**
     * Modo de relleno del área. STRETCH es el default (comportamiento previo exacto).
     */
    private FillMode  fillMode = FillMode.STRETCH;

    /** Alineación horizontal para FIT, COVER, CENTER y TILE. */
    private Alignment alignH   = Alignment.CENTER;

    /** Alineación vertical para FIT, COVER, CENTER y TILE. */
    private Alignment alignV   = Alignment.CENTER;

    // ── Constructores ─────────────────────────────────────────────────────

    /** Constructor con SpriteHandle (modo nuevo — Gameplay desacoplado). */
    public SpriteRendererComponent(SpriteHandle handle) {
        this(handle, SizeSyncMode.NONE);
    }

    /** Constructor con SpriteHandle y modo de sync. */
    public SpriteRendererComponent(SpriteHandle handle, SizeSyncMode syncMode) {
        this.handle   = handle;
        this.syncMode = syncMode;
        if (handle != null && handle.isValid()) {
            SpriteFrame def = handle.resolveDefault();
            renderWidth  = def.getWidth();
            renderHeight = def.getHeight();
        }
    }

    /** Constructor con BufferedImage (modo legacy — compatibilidad). */
    public SpriteRendererComponent(BufferedImage sprite) {
        this(sprite, SizeSyncMode.NONE);
    }

    /** Constructor con BufferedImage y modo de sync (modo legacy). */
    public SpriteRendererComponent(BufferedImage sprite, SizeSyncMode syncMode) {
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

    @Override
    public void render(RenderContext ctx, RenderCamera camera) {
        BufferedImage imageToDraw = resolveImage();
        if (imageToDraw == null) return;

        var pos = gameObject.getTransform().getPosition();
        int x = (int)(pos.getX() - camera.getX()) + offsetX;
        int y = (int)(pos.getY() - camera.getY()) + offsetY;

        int rw = renderWidth  > 0 ? renderWidth  : imageToDraw.getWidth();
        int rh = renderHeight > 0 ? renderHeight : imageToDraw.getHeight();

        // Culling: omitir sprites completamente fuera del viewport
        if (!ViewportCuller.isVisibleOnScreen(x, y, rw, rh, virtualWidth, virtualHeight)) {
            return;
        }

        // ── FillMode.STRETCH (default) — path original intacto ────────────
        // SpriteDrawer aplica TransformData completo. Un único drawImage().
        if (fillMode == FillMode.STRETCH) {
            SpriteFrame frame = resolveFrame();
            if (frame != null && frame.isValid()) {
                SpriteDrawer.INSTANCE.draw(
                    ctx.getGraphics2D(), frame, x, y, rw, rh, transform, shadowStrategy);
            } else if (imageToDraw != null) {
                // Legacy: BufferedImage directa sin TransformData
                ctx.drawImage(imageToDraw, x, y, rw, rh);
            }
            return;
        }

        // ── FillMode no-STRETCH — FillModeRenderer gestiona la geometría ──
        var g2d = ctx.getGraphics2D();
        if (shadowStrategy != null) {
            SpriteFrame frame = resolveFrame();
            if (frame != null && frame.isValid()) {
                var shadowCtx = new Game.Engine.RenderEngine.Strategies.RenderStrategy.DrawContext(
                    x + transform.offsetX, y + transform.offsetY, rw, rh, frame, transform);
                shadowStrategy.apply(g2d, shadowCtx);
            }
        }

        // Aplicar transformaciones de alpha/blend/geometric al Graphics2D
        // antes de que FillModeRenderer dibuje.
        // Estado guardado en variables LOCALES — seguro ante reentrancia.
        java.awt.Composite       savedComposite = g2d.getComposite();
        java.awt.geom.AffineTransform savedAffine = g2d.getTransform();

        applyTransformToContext(g2d, x, y, rw, rh, savedComposite, savedAffine);

        FillModeRenderer.draw(g2d, imageToDraw, x, y, rw, rh, fillMode, alignH, alignV);

        restoreContext(g2d, savedComposite, savedAffine);
    }

    // ── Aplicación de TransformData para FillMode no-STRETCH ─────────────

    /**
     * Aplica alpha y transformaciones geométricas al Graphics2D antes de
     * delegar en FillModeRenderer.
     *
     * El estado previo se pasa como parámetros (capturado en variables locales
     * del caller) para evitar cualquier estado de instancia mutable que podría
     * corromperse en escenarios de reentrancia o render en múltiples contextos.
     */
    private void applyTransformToContext(java.awt.Graphics2D g,
                                         int x, int y, int rw, int rh,
                                         java.awt.Composite savedComposite,
                                         java.awt.geom.AffineTransform savedAffine) {
        // Alpha
        if (transform.alpha < 1.0f) {
            g.setComposite(java.awt.AlphaComposite.getInstance(
                java.awt.AlphaComposite.SRC_OVER, transform.alpha));
        }
        // Flip horizontal
        if (transform.flipH) {
            g.translate(x + rw, y);
            g.scale(-1, 1);
            g.translate(-x, -y);
        }
        // Flip vertical
        if (transform.flipV) {
            g.translate(x, y + rh);
            g.scale(1, -1);
            g.translate(-x, -y);
        }
    }

    private void restoreContext(java.awt.Graphics2D g,
                                java.awt.Composite savedComposite,
                                java.awt.geom.AffineTransform savedAffine) {
        g.setTransform(savedAffine);
        g.setComposite(savedComposite);
    }

    // ── Resolución de imagen y frame ──────────────────────────────────────

    private BufferedImage resolveImage() {
        if (currentFrame != null && currentFrame.isValid()) return currentFrame.getImage();
        if (legacySprite != null) return legacySprite;
        if (handle != null && handle.isValid()) {
            SpriteFrame def = handle.resolveDefault();
            return def.isValid() ? def.getImage() : null;
        }
        return null;
    }

    private SpriteFrame resolveFrame() {
        if (currentFrame != null && currentFrame.isValid()) return currentFrame;
        if (handle != null && handle.isValid()) {
            SpriteFrame def = handle.resolveDefault();
            if (def.isValid()) return def;
        }
        return null;
    }

    // ── API pública ──────────────────────────────────────────────────────

    /** Actualiza el frame actual (llamado por AnimationController cada tick). */
    public void setCurrentFrame(SpriteFrame frame) {
        this.currentFrame = frame;
        if (syncMode == SizeSyncMode.NONE && frame != null && frame.isValid()) {
            if (renderWidth  == 0) renderWidth  = frame.getWidth();
            if (renderHeight == 0) renderHeight = frame.getHeight();
        }
    }

    /** Cambia el sprite con BufferedImage directa (compatibilidad legacy). */
    public void setSprite(BufferedImage sprite) {
        this.legacySprite = sprite;
        this.currentFrame = null;
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

    /** Offset visual respecto a la posición del objeto. */
    public void setOffset(int ox, int oy) {
        this.offsetX = ox;
        this.offsetY = oy;
    }

    // ── HRFC-004: FillMode API ────────────────────────────────────────────

    /**
     * Establece el modo de relleno del área de render.
     * Default: STRETCH (comportamiento previo, sin overhead).
     */
    public void setFillMode(FillMode mode) {
        this.fillMode = mode != null ? mode : FillMode.STRETCH;
    }

    /** FillMode actual. */
    public FillMode getFillMode() { return fillMode; }

    /**
     * Establece la alineación horizontal.
     * Relevante para FIT, COVER, CENTER, TILE, TILE_X.
     * Default: CENTER.
     */
    public void setAlignH(Alignment align) {
        this.alignH = align != null ? align : Alignment.CENTER;
    }

    /**
     * Establece la alineación vertical.
     * Relevante para FIT, COVER, CENTER, TILE, TILE_Y.
     * Default: CENTER.
     */
    public void setAlignV(Alignment align) {
        this.alignV = align != null ? align : Alignment.CENTER;
    }

    /** Atajo: establece alineación horizontal y vertical simultáneamente. */
    public void setAlignment(Alignment h, Alignment v) {
        setAlignH(h);
        setAlignV(v);
    }

    public Alignment getAlignH() { return alignH; }
    public Alignment getAlignV() { return alignV; }

    // ── HRFC-003: TransformData API ───────────────────────────────────────

    public void setTransform(TransformData transform) {
        this.transform = transform != null ? transform : TransformData.IDENTITY;
    }

    public TransformData getTransform() { return transform; }

    public void setFlipH(boolean flipH) {
        if (transform.flipH == flipH) return;
        transform = TransformData.builder()
            .flipH(flipH)
            .flipV(transform.flipV)
            .scaleX(transform.scaleX).scaleY(transform.scaleY)
            .rotation(transform.rotation)
            .pivot(transform.pivotX, transform.pivotY)
            .offset(transform.offsetX, transform.offsetY)
            .alpha(transform.alpha)
            .tint(transform.tintColor, transform.tintAlpha)
            .blendMode(transform.blendMode)
            .build();
    }

    public void setAlpha(float alpha) {
        if (transform.alpha == alpha) return;
        transform = TransformData.builder()
            .flipH(transform.flipH).flipV(transform.flipV)
            .scaleX(transform.scaleX).scaleY(transform.scaleY)
            .rotation(transform.rotation)
            .pivot(transform.pivotX, transform.pivotY)
            .offset(transform.offsetX, transform.offsetY)
            .alpha(alpha)
            .tint(transform.tintColor, transform.tintAlpha)
            .blendMode(transform.blendMode)
            .build();
    }

    public void setShadowStrategy(ShadowStrategy strategy) {
        this.shadowStrategy = strategy;
    }

    public void setVirtualSize(int vw, int vh) {
        this.virtualWidth  = vw;
        this.virtualHeight = vh;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public BufferedImage getSprite() {
        if (currentFrame != null && currentFrame.isValid()) return currentFrame.getImage();
        if (legacySprite != null) return legacySprite;
        if (handle != null && handle.isValid()) return handle.resolveDefault().getImage();
        return null;
    }

    public SpriteHandle getHandle()       { return handle;       }
    public SpriteFrame  getCurrentFrame() { return currentFrame; }
    public int          getRenderWidth()  { return renderWidth;  }
    public int          getRenderHeight() { return renderHeight; }
    public SizeSyncMode getSyncMode()     { return syncMode;     }
}
