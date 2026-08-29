package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.Entity.Components.Visuals.SpriteSkeletonComponent;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import Game.Engine.RenderEngine.Culling.RenderBounds;
import Game.Engine.RenderEngine.Optimized.BulletBatchRenderer;
import Game.Items.Types.Bullets.Definition.Bullet;
import java.util.ArrayList;
import java.util.List;

/**
 * Renderiza todos los objetos en orden de lista (sin depth sort).
 * Para depth sort usar DepthSortedRenderSystem.
 *
 * ── HRFC: Integración con RenderBounds ───────────────────────────────────
 * setRenderBounds() reemplaza setVirtualSize() como forma de configurar
 * el área de render. setVirtualSize() se mantiene para retrocompatibilidad.
 * Cuando hay RenderBounds activo, se propaga a los componentes de render
 * para culling asimétrico correcto.
 */
public class RenderSystem {

    private int virtualWidth  = 1280;
    private int virtualHeight = 720;

    // ── HRFC: RenderBounds opcional ───────────────────────────────────────

    /**
     * Configura el área de render con RenderBounds completo.
     * El ancho/alto virtuales se derivan del RenderBounds para compatibilidad.
     */
    public void setRenderBounds(RenderBounds bounds, int virtualWidth, int virtualHeight) {
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
        // Los componentes de render usan virtual size para culling.
        // Con RenderBounds asimétrico, propagamos el tamaño de los bounds
        // para que el culling incluya el área extendida.
        int renderW = (int) Math.ceil(bounds.getWidth());
        int renderH = (int) Math.ceil(bounds.getHeight());
        this.virtualWidth  = Math.max(virtualWidth,  renderW);
        this.virtualHeight = Math.max(virtualHeight, renderH);
    }

    /** Retrocompatibilidad: configura virtual size sin RenderBounds. */
    public void setVirtualSize(int vw, int vh) {
        this.virtualWidth  = vw;
        this.virtualHeight = vh;
    }

    // ── HRFC: Optimized Bullet Rendering ──────────────────────────────────
    
    /**
     * Flag para activar/desactivar el optimized bullet renderer.
     * true = usar BulletBatchRenderer (más rápido para muchos bullets)
     * false = usar el pipeline estándar
     */
    private boolean useOptimizedBulletRenderer = true;
    
    /**
     * Renderer optimizado para bullets (usa render data cache).
     */
    private final BulletBatchRenderer bulletRenderer = new BulletBatchRenderer();
    
    /**
     * Listas temporales para separar bullets de otros objetos.
     * Reutilizadas entre frames para evitar allocations.
     */
    private final List<Bullet> bullets = new ArrayList<>(2000);
    private final List<GameObjects> nonBullets = new ArrayList<>(500);

    public void setUseOptimizedBulletRenderer(boolean enabled) {
        this.useOptimizedBulletRenderer = enabled;
    }

    public void render(List<GameObjects> objects, RenderContext ctx, RenderCamera camera) {
        // ── HRFC: Separate bullets from other objects ─────────────────────
        if (useOptimizedBulletRenderer) {
            bullets.clear();
            nonBullets.clear();
            
            for (GameObjects obj : objects) {
                if (obj instanceof Bullet) {
                    bullets.add((Bullet) obj);
                } else {
                    nonBullets.add(obj);
                }
            }
            
            // Render bullets using optimized renderer
            if (!bullets.isEmpty()) {
                bulletRenderer.renderBullets(
                    bullets,
                    camera.getGameCamera(),
                    ctx.getGraphics2D(),
                    virtualWidth,
                    virtualHeight
                );
            }
            
            // Render non-bullets using standard pipeline
            for (GameObjects obj : nonBullets) {
                for (Component c : obj.getComponents()) {
                    if (c instanceof SpriteRendererComponent sr) {
                        sr.setVirtualSize(virtualWidth, virtualHeight);
                    } else if (c instanceof SpriteSkeletonComponent sc) {
                        sc.setVirtualSize(virtualWidth, virtualHeight);
                    }
                    if (c instanceof Renderable r) {
                        r.render(ctx, camera);
                    }
                }
            }
        } else {
            // Standard pipeline (original behavior)
            for (GameObjects obj : objects) {
                for (Component c : obj.getComponents()) {
                    if (c instanceof SpriteRendererComponent sr) {
                        sr.setVirtualSize(virtualWidth, virtualHeight);
                    } else if (c instanceof SpriteSkeletonComponent sc) {
                        sc.setVirtualSize(virtualWidth, virtualHeight);
                    }
                    if (c instanceof Renderable r) {
                        r.render(ctx, camera);
                    }
                }
            }
        }
    }
}
