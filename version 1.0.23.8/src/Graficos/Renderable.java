package Graficos;

import Game.Render.Camera;
import Game.Render.RenderContext;

public interface Renderable {
    void render(RenderContext ctx, Camera camera);
}
