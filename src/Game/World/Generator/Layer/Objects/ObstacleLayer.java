package Game.World.Generator.Layer.Objects;

import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Sprites.Obstacles.ObstaclesAssets;
import java.util.Random;

/**
 * Capa de obstáculos — dispersa obstáculos aleatorios en el mundo.
 *
 * MEJORAS vs. versión original:
 *
 * 1. USA WorldObjectFactory: no crea Obstacle con new directamente.
 *
 * 2. CANTIDAD CONFIGURABLE: en lugar de hardcodear nextInt(5),
 *    se expone un rango min/max de obstáculos por chunk.
 *
 * 3. TAMAÑO CONFIGURABLE: ancho y alto de obstáculos como parámetros.
 *    Permite subclases fáciles (ObstacleBigLayer, ObstacleSmallLayer…).
 *
 * 4. MARGEN CONFIGURABLE: el margen del borde del mundo (antes fijo en 40/120)
 *    ahora se deriva del tamaño del obstáculo automáticamente.
 *
 * Retro-compatible: ObstacleLayer() sin args reproduce exactamente el original.
 */
public class ObstacleLayer implements WorldLayer {

    private final int minCount;
    private final int maxCount;
    private final int obstacleW;
    private final int obstacleH;

    /** Constructor por defecto — idéntico al original (0–4 obstáculos, 40x80). */
    public ObstacleLayer() {
        this(0, 5, 40, 80);
    }

    /**
     * Constructor configurable.
     *
     * @param minCount   mínimo de obstáculos por chunk (inclusive)
     * @param maxCount   máximo de obstáculos por chunk (exclusive, igual que Random.nextInt)
     * @param obstacleW  ancho de cada obstáculo en píxeles lógicos
     * @param obstacleH  alto de cada obstáculo en píxeles lógicos
     */
    public ObstacleLayer(int minCount, int maxCount, int obstacleW, int obstacleH) {
        this.minCount  = minCount;
        this.maxCount  = maxCount;
        this.obstacleW = obstacleW;
        this.obstacleH = obstacleH;
    }

    @Override
    public void generate(World world, Random random) {
        int worldWidth  = world.getWidth();
        int worldHeight = world.getHeight();

        // Margen automático basado en el tamaño del obstáculo
        int maxX = worldWidth  - obstacleW;
        int maxY = worldHeight - obstacleH;

        if (maxX <= 0 || maxY <= 0) return;

        int range = maxCount - minCount;
        int count = (range > 0) ? minCount + random.nextInt(range) : minCount;

        for (int i = 0; i < count; i++) {
            int x = random.nextInt(maxX);
            int y = random.nextInt(maxY);

            world.add(WorldObjectFactory.obstacle(
                x, y,
                obstacleW, obstacleH,
                ObstaclesAssets.getMondongoImage()
            ));
        }
    }
}
