package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.GameObjects;
import Game.Render.Camera;
import Game.Render.DebugRenderable;
import Game.Render.RenderContext;
import Game.Settings.GameSettings;

import java.util.List;

public class DebugRenderSystem {

    public void render(List<GameObjects> objects,
                       RenderContext ctx,
                       Camera camera) {

        if (!GameSettings.getInstance().isDebugEnabled()) return;

        for (GameObjects obj : objects) {

            for (Component component : obj.getComponents()) {

                if (component instanceof DebugRenderable debugRenderable) {
                    debugRenderable.debugRender(ctx, camera);
                }
            }
        }
    }
}