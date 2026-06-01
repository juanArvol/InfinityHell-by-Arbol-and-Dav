package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.UI.POV.Camera;
import Game.UI.POV.RenderContext;
import Graficos.Renderable;

import java.awt.Color;

public class RectRenderer extends Component implements Renderable {

    private int width;
    private int height;
    private Color color;

    public RectRenderer(int width, int height, Color color) {
        this.width = width;
        this.height = height;
        this.color = color;
    }

    @Override
    public void render(RenderContext ctx, Camera camera) {

        var pos = gameObject.getTransform().getPosition();

        int x = (int)(pos.getX() - camera.getX());
        int y = (int)(pos.getY() - camera.getY());

        ctx.setColor(color);
        ctx.fillRect(x, y, width, height);
    }
}