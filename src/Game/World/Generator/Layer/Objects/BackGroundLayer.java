package Game.World.Generator.Layer.Objects;

import java.util.Random;

import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.Visuals.BackGround;
import GameMath.Vector2D;

public class BackGroundLayer implements WorldLayer {

    @Override
    public void generate(World world, Random random) {
        int width = world.getWidth();
        int height = world.getHeight();

            // Fondo base
        world.add(
            new BackGround(
                new Vector2D(0, 0),
                null,
                width,
                height
            )
        );
    }
}