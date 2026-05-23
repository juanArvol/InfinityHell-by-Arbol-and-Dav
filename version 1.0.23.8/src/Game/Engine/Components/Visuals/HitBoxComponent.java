package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Render.Camera;
import Game.Render.DebugRenderable;
import Game.Render.RenderContext;

import java.awt.Color;
import java.awt.Rectangle;

public class HitBoxComponent extends Component implements DebugRenderable {

    private int width;
    private int height;
    private int offsetX;
    private int offsetY;

    private boolean visible = true;
    private Color debugColor = Color.RED;

    public HitBoxComponent(int width, int height) {
        this(width, height, 0, 0);
    }

    public HitBoxComponent(int width, int height, int offsetX, int offsetY) {
        this.width = width;
        this.height = height;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    @Override
    public void start() {
        syncWithSprite();
    }

    public void syncWithSprite() {

        SpriteRenderer sprite = gameObject.getComponent(SpriteRenderer.class);
        if (sprite == null || sprite.getSprite() == null) return;

        SizeSyncMode mode = sprite.getSyncMode();

        if (mode == SizeSyncMode.HITBOX_TO_SPRITE) {
            width = sprite.getSprite().getWidth();
            height = sprite.getSprite().getHeight();
        }
    }

    public Rectangle getBounds() {
        var pos = gameObject.getTransform().getPosition();
        return new Rectangle(
                (int) pos.getX() + offsetX,
                (int) pos.getY() + offsetY,
                width,
                height
        );
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
        notifySprite();
    }

    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }

    private void notifySprite() {
        SpriteRenderer sprite = gameObject.getComponent(SpriteRenderer.class);
        if (sprite != null) {
            sprite.syncWithHitbox();
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }

    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isVisible() { return visible; }

    public void setDebugColor(Color color) { this.debugColor = color; }
    public Color getDebugColor() { return debugColor; }

    @Override
    public void debugRender(RenderContext ctx, Camera camera) {
        if (!visible) return;

        Rectangle rect = getBounds();

        int x = (int)(rect.x - camera.getX());
        int y = (int)(rect.y - camera.getY());

        ctx.drawHitbox(new Rectangle(x, y, rect.width, rect.height), debugColor);
    }
}