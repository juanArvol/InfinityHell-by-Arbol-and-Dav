package Game.World.Generator.Layer.Objects;

import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.BlockWorld;
import GameMath.Vector2D;
import Graficos.Around.Blocks.BlocksAssets;

import java.util.Random;

public class TerrainLayer implements WorldLayer {

    @Override
    public void generate(World world, Random random) {

        int width = world.getWidth();
        int height = world.getHeight();

        int groundHeight = height / 4;
        int groundY = height - groundHeight;

        BlockWorld ground = new BlockWorld(
                new Vector2D(0, groundY),
                BlocksAssets.suelo.getSprite(),
                width,
                groundHeight*4
        );
        world.add(ground);
    }
}