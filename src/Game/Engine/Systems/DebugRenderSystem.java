package Game.Engine.Systems;

import Game.Engine.Component;
import Game.Engine.GameObjects;
import Game.UI.POV.Camera;
import Game.UI.POV.DebugRenderable;
import Game.UI.POV.RenderContext;
import Main.Debug.DebugGameSettings;

import java.util.List;

/**
 * Renderiza los hitboxes y helpers de debug.
 *
 * REFACTORIZACIÓN: GameSettings inyectado por constructor.
 *
 *   ANTES: `GameSettings.getInstance().isDebugEnabled()`
 *   → dependencia estática invisible; imposible testear sin el singleton
 *
 *   AHORA: recibe GameSettings en el constructor
 *   → dependencia explícita; testeable; no usa singletons
 *
 *   Justificación DIP: DebugRenderSystem depende de la abstracción
 *   (el objeto GameSettings) no de cómo se obtiene esa instancia.
 *   El wiring (quién crea GameSettings y cómo) es responsabilidad
 *   de la capa de composición (WorldRenderer, GameState).
 *
 * Compatibilidad: se mantiene constructor con GameSettings como argumento.
 * Ningún otro cambio de comportamiento.
 */
public class DebugRenderSystem {

    private final DebugGameSettings settings;

    public DebugRenderSystem(DebugGameSettings settings) {
        this.settings = settings;
    }

    public void render(List<GameObjects> objects, RenderContext ctx, Camera camera) {
        if (!settings.isDebugEnabled()) return;

        for (GameObjects obj : objects) {
            for (Component c : obj.getComponents()) {
                if (c instanceof DebugRenderable d) {
                    d.debugRender(ctx);
                }
            }
        }
    }
}
