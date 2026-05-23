package Game.Engine.Components.Visuals;

import Game.Engine.Component;
import Game.Render.Camera;
import Game.Render.RenderContext;
import Graficos.Renderable;

import java.awt.image.BufferedImage;

public class SpriteRenderer extends Component implements Renderable {

    protected BufferedImage sprite;

    private int renderWidth;
    private int renderHeight;

    private int offsetX = 0;
    private int offsetY = 0;

    private SizeSyncMode syncMode = SizeSyncMode.NONE;

    public SpriteRenderer(BufferedImage sprite) {
        this.sprite = sprite;

        if (sprite != null) {
            renderWidth = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }
    }

    @Override
    public void start() {
        applyInitialSync();
    }

    @Override
    public void update() {
        if (syncMode == SizeSyncMode.BIDIRECTIONAL) {
            syncWithHitbox();
        }
    }

    private void applyInitialSync() {

        HitBoxComponent hitbox = gameObject.getComponent(HitBoxComponent.class);
        if (hitbox == null || sprite == null) return;

        switch (syncMode) {
            case SPRITE_TO_HITBOX:
            case BIDIRECTIONAL:
                renderWidth = hitbox.getWidth();
                renderHeight = hitbox.getHeight();
                break;

            case HITBOX_TO_SPRITE:
                hitbox.setSize(sprite.getWidth(), sprite.getHeight());
                break;

            default:
                break;
        }
    }

    public void syncWithHitbox() {

        HitBoxComponent hitbox = gameObject.getComponent(HitBoxComponent.class);
        if (hitbox == null) return;

        if (syncMode == SizeSyncMode.SPRITE_TO_HITBOX ||
            syncMode == SizeSyncMode.BIDIRECTIONAL) {

            renderWidth = hitbox.getWidth();
            renderHeight = hitbox.getHeight();
        }
    }

    public void setSyncMode(SizeSyncMode mode) {
        this.syncMode = mode;
        if (gameObject != null) {
            applyInitialSync();
        }
    }

    public SizeSyncMode getSyncMode() {
        return syncMode;
    }

    public void setSize(int width, int height) {
        renderWidth = width;
        renderHeight = height;
        syncMode = SizeSyncMode.NONE;
    }

    @Override
    public void render(RenderContext ctx, Camera camera) {

        if (sprite == null) return;

        var pos = gameObject.getTransform().getPosition();

        int x = (int)(pos.getX() - camera.getX() + offsetX);
        int y = (int)(pos.getY() - camera.getY() + offsetY);

        ctx.drawImage(sprite, x, y, renderWidth, renderHeight);
    }

    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;

        if (syncMode == SizeSyncMode.NONE && sprite != null) {
            renderWidth = sprite.getWidth();
            renderHeight = sprite.getHeight();
        }

        applyInitialSync();
    }

    public BufferedImage getSprite() {
        return sprite;
    }
}