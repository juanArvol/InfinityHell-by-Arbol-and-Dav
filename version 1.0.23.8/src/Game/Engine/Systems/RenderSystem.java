package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.GameObjects;
import Game.Render.Camera;
import Game.Render.RenderContext;
import Graficos.Renderable;

import java.util.List;

/**
 * Renderiza todos los objetos en orden de lista (sin depth sort).
 * Para depth sort usar DepthSortedRenderSystem.
 */
public class RenderSystem {

    public void render(List<GameObjects> objects, RenderContext ctx, Camera camera) {
        for (GameObjects obj : objects) {
            for (Component c : obj.getComponents()) {
                if (c instanceof Renderable r) {
                    r.render(ctx, camera);
                }
            }
        }
    }
}
