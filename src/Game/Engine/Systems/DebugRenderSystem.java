package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.GameObjects;
import Game.Engine.RenderEngine.Context.RenderCamera;
import Game.Engine.RenderEngine.Context.RenderContext;
import Game.Engine.RenderEngine.Contracts.DebugRenderable;
import java.util.List;

/**
 * Renderiza los hitboxes y helpers de debug.
 *
 * Recibe DebugSettings por constructor (DIP: depende de la abstracción,
 * no de la implementación concreta Main.Debug.DebugGameSettings).
 * Esto elimina la única dependencia que quedaba del Engine hacia Main.*.
 *
 * El wiring (pasar la instancia concreta) es responsabilidad de la capa
 * de composición (SceneRenderer). DebugGameSettings implementa DebugSettings.
 */
public class DebugRenderSystem {

    private final DebugSettings settings;

    public DebugRenderSystem(DebugSettings settings) {
        this.settings = settings;
    }

    public void render(List<GameObjects> objects, RenderContext ctx, RenderCamera camera) {
        if (!settings.isDebugEnabled()) return;

        for (GameObjects obj : objects) {
            for (Component c : obj.getComponents()) {
                if (c instanceof DebugRenderable d) {
                    d.debugRender(ctx, camera);
                }
            }
        }
    }
}
