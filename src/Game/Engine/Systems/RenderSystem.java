package Game.Engine.Systems;

import Game.Engine.Component;
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

    public void render(List<GameObjects> objects, RenderContext ctx, RenderCamera camera) {
        for (GameObjects obj : objects) {
            for (Component c : obj.getComponents()) {
                if (c instanceof Renderable r) {
                    r.render(ctx, camera);
                }
            }
        }
    }
}
