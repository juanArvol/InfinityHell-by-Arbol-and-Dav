package Game.World.Generator;

import Game.World.Core.World;
import Game.World.Core.WorldCoordinator;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.Generator.Layer.Objects.BackGroundLayer;
import Game.World.Generator.Layer.Objects.ObstacleLayer;
import Game.World.Generator.Layer.Objects.TerrainLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WorldGenerator {

    private final List<WorldLayer> layers = new ArrayList<>();

    public WorldGenerator() {
        // Correct order: background → terrain → obstacles
        layers.add(new BackGroundLayer());
        layers.add(new TerrainLayer());
        layers.add(new ObstacleLayer());
    }

    public World generate(int width,
                          int height,
                          WorldCoordinator coord) {

        World world = new World(width, height, coord);

        Random random = new Random(coord.hashCode());

        for (WorldLayer layer : layers) {
            layer.generate(world, random);
        }

        return world;
    }
}