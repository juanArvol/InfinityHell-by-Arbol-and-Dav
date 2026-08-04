package Game.World.Generator.Layer.Objects;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.Visuals.BackGround;
import java.util.Random;

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