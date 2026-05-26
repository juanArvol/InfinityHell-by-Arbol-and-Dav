package Game.World.Generator.Layer.Objects;

import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Graficos.Obstacles.ObstaclesAssets;

import java.util.Random;

/**
 * Capa de vegetación — añade obstáculos decorativos (arbustos, árboles)
 * sin bloquear el movimiento del jugador (no-collision, solo visual).
 *
 * En la arquitectura actual Obstacle sí tiene colisión, así que por ahora
 * VegetationLayer usa obstáculos pequeños como proxy visual.
 * Cuando se añada un tipo VegetationObject sin colisión, basta con cambiar
 * WorldObjectFactory.vegetation(...) aquí.
 *
 * Configurable mediante builder (igual que LootSpawnLayer).
 *
 * Uso:
 *   .addLayer(new VegetationLayer(10, 20, 16, 32))
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
    public void generate(World world, Random random) {
        int range = maxCount - minCount;
        int count = (range > 0) ? minCount + random.nextInt(range) : minCount;

        int maxX = world.getWidth()  - vegW;
        int maxY = world.getHeight() - vegH;
        if (maxX <= 0 || maxY <= 0) return;

        for (int i = 0; i < count; i++) {
            int x = random.nextInt(maxX);
            int y = random.nextInt(maxY);

            // Usa ObstaclesAssets por ahora; reemplazar con VegetationAssets cuando existan
            world.add(WorldObjectFactory.obstacle(
                x, y,
                vegW, vegH,
                ObstaclesAssets.mondongo.getSprite()
            ));
        }
    }
}
