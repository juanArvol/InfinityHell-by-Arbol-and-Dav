package Game.Engine.Colisions;

import Game.Engine.GameObjects;

public record CollisionsPair(
        GameObjects a,
        GameObjects b,
        boolean trigger
) {}