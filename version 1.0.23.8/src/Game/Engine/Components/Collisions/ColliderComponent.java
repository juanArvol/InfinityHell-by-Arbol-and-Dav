package Game.Engine.Components.Collisions;

import Game.Engine.Component;
import Game.Engine.Components.Visuals.HitBoxComponent;
import Game.Engine.Filter.Masks.CollisionProfileMask;

import java.awt.Rectangle;

public class ColliderComponent extends Component {

    public enum ColliderType {
        SOLID,
        TRIGGER
    }

    private int layer = 1;
    private int mask  = 1;

    private ColliderType type;

    private int width;
    private int height;
    private int offsetX;
    private int offsetY;

    @Override
    public void start() {
        syncWithHitBox();
    }

    @Override
    public void update() {
        syncWithHitBox();
    }

    private void syncWithHitBox() {

        HitBoxComponent hitbox =
                gameObject.getComponent(HitBoxComponent.class);

        if (hitbox == null) return;

        Rectangle bounds = hitbox.getBounds();

        width  = bounds.width;
        height = bounds.height;

        offsetX = bounds.x -
                (int)gameObject.getTransform().getPosition().getX();

        offsetY = bounds.y -
                (int)gameObject.getTransform().getPosition().getY();
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

    public void applyProfile(CollisionProfileMask profile) {
        this.layer = profile.getLayer();
        this.mask  = profile.getMask();
    }

    public ColliderType getType() {
        return type;
    }

    public void setType(ColliderType type) {
        this.type = type;
    }

    public int getLayer() { return layer; }
    public int getMask() { return mask; }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public void setLayer(int layer) { this.layer = layer; }
    public void setMask(int mask) { this.mask = mask; }
    
}