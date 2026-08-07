package Game.World.Generator.Layer.Objects;

import Game.World.Chunk.Chunk;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Sprites.Enviroment.Obstacles.ObstaclesAssets;
import java.util.Random;

/**
 * Capa de vegetación — añade obstáculos decorativos en el mundo.
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: posiciones random.nextInt(world.getWidth()) en coords locales.
 *
 * AHORA: posición global = chunk.getOriginX() + random.nextInt(maxLocalX)
 *
 * VERIFICACIÓN para chunk(0,0): originX=0 → idéntico al anterior.
 */
public class VegetationLayer implements WorldLayer {

    private final int minCount;
    private final int maxCount;
    private final int vegW;
    private final int vegH;

    public VegetationLayer() {
        this(10, 20, 16, 32);
    }

    public VegetationLayer(int minCount, int maxCount, int vegW, int vegH) {
        this.minCount = minCount;
        this.maxCount = maxCount;
        this.vegW     = vegW;
        this.vegH     = vegH;
    }

    @Override
    public void generate(Chunk chunk, Random random) {
        int range = maxCount - minCount;
        int count = (range > 0) ? minCount + random.nextInt(range) : minCount;

        int maxLocalX = chunk.getWidth()  - vegW;
        int maxLocalY = chunk.getHeight() - vegH;
        if (maxLocalX <= 0 || maxLocalY <= 0) return;

        int originX = chunk.getOriginX();
        int originY = chunk.getOriginY();

        for (int i = 0; i < count; i++) {
            int localX = random.nextInt(maxLocalX);
            int localY = random.nextInt(maxLocalY);

            int globalX = originX + localX;
            int globalY = originY + localY;

            chunk.add(WorldObjectFactory.obstacle(
                globalX, globalY,
                vegW, vegH,
                ObstaclesAssets.getMondongoImage()
            ));
        }
    }
}
