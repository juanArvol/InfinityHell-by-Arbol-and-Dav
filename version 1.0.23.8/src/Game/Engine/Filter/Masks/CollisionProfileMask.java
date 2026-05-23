package Game.Engine.Filter.Masks;

public class CollisionProfileMask {

    private final int layer;
    private final int mask;

    public CollisionProfileMask(int layer, int mask) {
        this.layer = layer;
        this.mask = mask;
    }

    public int getLayer() {
        return layer;
    }

    public int getMask() {
        return mask;
    }
}