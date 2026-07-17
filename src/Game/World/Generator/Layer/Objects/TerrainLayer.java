package Game.World.Generator.Layer.Objects;

import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Sprites.Enviroment.Around.Blocks.BlocksAssets;
import java.util.Random;

/**
 * Capa de terreno — genera el suelo base del mundo.
 *
 * MEJORAS vs. versión original:
 *
 * 1. USA WorldObjectFactory: ya no crea BlockWorld con new directamente.
 *    Esto desacopla la generación de la construcción concreta.
 *
 * 2. RELACIÓN ALTURA CONFIGURABLE: el groundRatio (por defecto 0.25 = 25% del mundo)
 *    se puede ajustar al construir la capa.
 *    Ejemplo: new TerrainLayer(0.15) → suelo más delgado.
 *
 * 3. El bug del groundHeight*4 ya estaba corregido en la versión anterior.
 *    Se mantiene el fix: el bloque tiene height=groundHeight, no groundHeight*4.
 */
public class TerrainLayer implements WorldLayer {

    private final double groundRatio;

    /** Constructor por defecto: suelo = 25% de la altura del mundo (igual que antes). */
    public TerrainLayer() {
        this(0.25);
    }

    /**
     * Constructor con ratio configurable.
     *
     * @param groundRatio fracción de la altura del mundo que ocupa el suelo (0.0–1.0)
     */
    public TerrainLayer(double groundRatio) {
        if (groundRatio <= 0 || groundRatio >= 1) {
            throw new IllegalArgumentException("groundRatio debe estar en (0, 1). Recibido: " + groundRatio);
        }
        this.groundRatio = groundRatio;
    }

    @Override
    public void generate(World world, Random random) {
        int width  = world.getWidth();
        int height = world.getHeight();

        int groundHeight = (int)(height * groundRatio);
        int groundY      = height - groundHeight;

        world.add(WorldObjectFactory.groundBlock(
            width,
            groundY,
            groundHeight,
            BlocksAssets.getSueloImage()
        ));
    }
}
