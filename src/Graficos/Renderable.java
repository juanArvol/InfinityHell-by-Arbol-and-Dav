package Graficos;

import Game.UI.POV.Camera;
import Game.UI.POV.RenderContext;

public interface Renderable {
    void render(RenderContext ctx, Camera camera);
}
