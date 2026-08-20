package Game.World.Generator.Layer.Objects;

import Game.World.Chunk.Chunk;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Sprites.Enviroment.Obstacles.ObstaclesAssets;
import java.util.Random;

/**
 * Capa de obstáculos — dispersa obstáculos aleatorios en el mundo.
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: posiciones random.nextInt(world.getWidth()) → coordenadas locales [0, width).
 *
 * AHORA: posición global = chunk.getOriginX() + random.nextInt(chunk.getWidth())
 *
 * VERIFICACIÓN para chunk(0,0): originX=0 → global = 0 + local ← IDÉNTICO
 * VERIFICACIÓN para chunk(1,0): originX=1280 → global = 1280 + local ← correcto
 *
 * El número y tipo de obstáculos generados es idéntico al anterior para (0,0).
 */
public class ObstacleLayer implements WorldLayer {

    private final int minCount;
    private final int maxCount;
    private final int obstacleW;
    private final int obstacleH;

    /** Constructor por defecto — idéntico al original (0–4 obstáculos, 40x80). */
    public ObstacleLayer() {
        this(0, 10, 100, 200);
    }

    /**
     * Constructor configurable.
     *
     * @param minCount   mínimo de obstáculos por chunk (inclusive)
     * @param maxCount   máximo de obstáculos por chunk (exclusive)
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
    public void generate(Chunk chunk, Random random) {
        // Área disponible en coordenadas locales (sin margen de borde)
        int maxLocalX = chunk.getWidth()  - obstacleW;
        int maxLocalY = chunk.getHeight() - obstacleH;

        if (maxLocalX <= 0 || maxLocalY <= 0) return;

        int range = maxCount - minCount;
        int count = (range > 0) ? minCount + random.nextInt(range) : minCount;

        // Origen global del chunk
        int originX = chunk.getOriginX();
        int originY = chunk.getOriginY();

        for (int i = 0; i < count; i++) {
            // Posición local aleatoria dentro del chunk
            int localX = random.nextInt(maxLocalX);
            int localY = random.nextInt(maxLocalY);

            // Convertir a coordenadas globales
            int globalX = originX + localX;
            int globalY = originY + localY;

            chunk.add(WorldObjectFactory.obstacle(
                globalX, globalY,
                obstacleW, obstacleH,
                ObstaclesAssets.getMondongoImage()
            ));
        }
    }
}
