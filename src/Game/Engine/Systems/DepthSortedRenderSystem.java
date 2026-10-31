package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.Entity.Components.Visuals.SpriteRendererComponent;
import Game.Engine.Entity.Components.Visuals.SpriteSkeletonComponent;
import Game.Engine.GameMath.Logic3D.Transform3D;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import Game.Engine.RenderEngine.Culling.RenderBounds;
import Game.Engine.RenderEngine.Optimized.BulletBatchRenderer;
import Game.Items.Types.Bullets.Definition.Bullet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sistema de render con ordenamiento por profundidad (Painter's Algorithm).
 *
 * ── HRFC: Integración con RenderBounds ───────────────────────────────────
 * setRenderBounds() reemplaza setVirtualSize() como configuración del
 * área de render. El virtual size se propaga a los componentes para culling.
 * setVirtualSize() se mantiene para retrocompatibilidad.
 *
 * ── PAINTER'S ALGORITHM 2.5D ──────────────────────────────────────────────
 * 1. Ordena objetos por depthValue = Y + Z*0.5.
 * 2. Objetos con mayor valor se dibujan después → aparecen encima.
 * 3. Si no hay Transform3D, usa solo Y (retro-compatible con 2D puro).
 *
 * ── STATELESS / REENTRANTE ────────────────────────────────────────────────
 * Buffer de ordenación local por frame — seguro para renders secundarios.
 */
public class DepthSortedRenderSystem {

    private int virtualWidth  = 1280;
    private int virtualHeight = 720;

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

    // ── HRFC: RenderBounds opcional ───────────────────────────────────────

    /**
     * Configura el área de render con RenderBounds completo.
     */
    public void setRenderBounds(RenderBounds bounds, int virtualWidth, int virtualHeight) {
        this.virtualWidth  = virtualWidth;
        this.virtualHeight = virtualHeight;
        int renderW = (int) Math.ceil(bounds.getWidth());
        int renderH = (int) Math.ceil(bounds.getHeight());
        this.virtualWidth  = Math.max(virtualWidth,  renderW);
        this.virtualHeight = Math.max(virtualHeight, renderH);
    }

    /** Retrocompatibilidad. */
    public void setVirtualSize(int vw, int vh) {
        this.virtualWidth  = vw;
        this.virtualHeight = vh;
    }

    public void render(List<GameObjects> objects,
                       RenderContext ctx,
                       RenderCamera camera) {

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
            
            // Sort non-bullets by depth
            nonBullets.sort(Comparator.comparingDouble(this::getDepthValue));
            
            // Render non-bullets using standard pipeline
            for (GameObjects obj : nonBullets) {
                for (Component c : obj.getComponents()) {
                    if (c instanceof SpriteRendererComponent sr) {
                        sr.setVirtualSize(virtualWidth, virtualHeight);
                    } else if (c instanceof SpriteSkeletonComponent sc) {
                        sc.setVirtualSize(virtualWidth, virtualHeight);
                    }
                    if (c instanceof Renderable renderable) {
                        renderable.render(ctx, camera);
                    }
                }
            }
            
            // Render bullets using optimized renderer (bullets don't need depth sort)
            if (!bullets.isEmpty()) {
                bulletRenderer.renderBullets(
                    bullets,
                    camera.getGameCamera(),
                    ctx.getGraphics2D(),
                    virtualWidth,
                    virtualHeight
                );
            }
        } else {
            // Standard pipeline (original behavior)
            List<GameObjects> sortBuffer = new ArrayList<>(objects);
            sortBuffer.sort(Comparator.comparingDouble(this::getDepthValue));

            for (GameObjects obj : sortBuffer) {
                for (Component c : obj.getComponents()) {
                    if (c instanceof SpriteRendererComponent sr) {
                        sr.setVirtualSize(virtualWidth, virtualHeight);
                    } else if (c instanceof SpriteSkeletonComponent sc) {
                        sc.setVirtualSize(virtualWidth, virtualHeight);
                    }
                    if (c instanceof Renderable renderable) {
                        renderable.render(ctx, camera);
                    }
                }
            }
        }
    }

    private double getDepthValue(GameObjects obj) {
        if (obj.getTransform() instanceof Transform3D t3d) {
            return t3d.getDepthSortValue();
        }
        return obj.getTransform().getY();
    }
}
