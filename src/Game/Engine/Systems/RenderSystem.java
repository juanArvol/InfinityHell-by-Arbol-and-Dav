package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.Components.Visuals.SpriteRenderer;
import Game.Engine.Components.Visuals.SpriteSkeletonComponent;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.Renderable;
import java.util.List;

/**
 * Renderiza todos los objetos en orden de lista (sin depth sort).
 * Para depth sort usar DepthSortedRenderSystem.
 */
public class RenderSystem {

    private int virtualWidth  = 1280;
    private int virtualHeight = 720;

    public void setVirtualSize(int vw, int vh) {
        this.virtualWidth  = vw;
        this.virtualHeight = vh;
    }

    public void render(List<GameObjects> objects, RenderContext ctx, RenderCamera camera) {
        for (GameObjects obj : objects) {
            for (Component c : obj.getComponents()) {
                if (c instanceof SpriteRenderer sr) {
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
