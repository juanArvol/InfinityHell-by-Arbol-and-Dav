package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.GameObjects;
import Game.Render.Camera;
import Game.Render.DebugRenderable;
import Game.Render.RenderContext;
import Game.Settings.GameSettings;

import java.util.List;

/**
 * Renderiza los hitboxes y helpers de debug.
 * Solo activo cuando GameSettings.isDebugEnabled() == true.
 */
public class DebugRenderSystem {

    public void render(List<GameObjects> objects, RenderContext ctx, Camera camera) {
        if (!GameSettings.getInstance().isDebugEnabled()) return;

        for (GameObjects obj : objects) {
            for (Component c : obj.getComponents()) {
                if (c instanceof DebugRenderable d) {
                    d.debugRender(ctx, camera);
                }
            }
        }
    }
}
