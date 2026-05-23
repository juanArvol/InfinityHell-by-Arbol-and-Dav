package Game.World.Generator.Layer.Objects;

import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.Obstacle;
import Graficos.Obstacles.ObstaclesAssets;

import java.util.Random;

public class ObstacleLayer implements WorldLayer {

    @Override
    public void generate(World world, Random random) {

        int worldWidth = world.getWidth();
        int worldHeight = world.getHeight();

        if (worldWidth < 40 || worldHeight < 120) return;

        int obstacleCount = random.nextInt(5);

        int maxX = worldWidth - 40;
        int maxY = worldHeight - 120;

        for (int i = 0; i < obstacleCount; i++) {

            int x = random.nextInt(maxX);
            int y = random.nextInt(maxY);

            world.add(
                new Obstacle(
                    x,
                    y,
                    40,
                    80,
                    ObstaclesAssets.mondongo.getSprite()
                )
            );
        }
    }
}