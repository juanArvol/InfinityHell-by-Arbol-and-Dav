package Game.Engine.Filter.Masks;

import Game.Engine.Filter.Layer;

public class CollisionProfiles {

    // PLAYER
    public static final CollisionProfileMask PLAYER =
            new CollisionProfileMask(
                    Layer.PLAYER,
                    Layer.WORLD | Layer.ENEMY
            );

    // ENEMY (sin friendly fire)
    public static final CollisionProfileMask ENEMY =
            new CollisionProfileMask(
                    Layer.ENEMY,
                    Layer.WORLD | Layer.PLAYER
            );

    // ENEMY (friendly fire)
    public static final CollisionProfileMask ENEMY_FRIENDLY =
            new CollisionProfileMask(
                    Layer.ENEMY,
                    Layer.WORLD | Layer.PLAYER | Layer.ENEMY
            );

    // BULLET NORMAL
    public static final CollisionProfileMask BULLET =
            new CollisionProfileMask(
                    Layer.DEFAULT,
                    Layer.WORLD | Layer.ENEMY
            );

    // WORLD
    public static final CollisionProfileMask WORLD =
            new CollisionProfileMask(
                    Layer.WORLD,
                    Layer.PLAYER | Layer.ENEMY | Layer.DEFAULT
            );
}