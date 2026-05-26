package Game.World.Generator.Layer;

import Game.World.Core.World;

import java.util.Random;

public interface WorldLayer {
    void generate(World world, Random random);
}