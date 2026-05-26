package Game.Engine.Colisions;

import Game.Engine.GameObjects;

/**
 * Par de objetos que colisionaron en este frame.
 *
 * Reemplaza a CollisionsPair. Igual de simple pero con nombre más claro
 * y un campo extra (normalX/normalY) para saber la dirección del impacto.
 */
public final class CollisionResult {

    public final GameObjects a;
    public final GameObjects b;
    public final boolean trigger;

    /** Normal del impacto en X (-1, 0, +1). Viene del SweptAABB. */
    public final int normalX;
    /** Normal del impacto en Y (-1, 0, +1). Viene del SweptAABB. */
    public final int normalY;

    public CollisionResult(GameObjects a, GameObjects b, boolean trigger) {
        this(a, b, trigger, 0, 0);
    }

    public CollisionResult(GameObjects a, GameObjects b, boolean trigger, int normalX, int normalY) {
        this.a       = a;
        this.b       = b;
        this.trigger = trigger;
        this.normalX = normalX;
        this.normalY = normalY;
    }
}
