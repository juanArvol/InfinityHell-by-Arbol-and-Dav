package Game.World.Generator.Layer.Objects;

import Game.World.Chunk.Chunk;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Sprites.Enviroment.Around.Blocks.BlocksAssets;
import java.util.Random;

/**
 * Capa de terreno — genera el suelo base del mundo.
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: el suelo siempre empezaba en x=0 (local al chunk).
 *   groundBlock(worldWidth, groundY, ...) → bloques en (0, groundY)
 *
 * AHORA: el suelo empieza en chunk.getOriginX() (global).
 *   El ancho del bloque sigue siendo chunk.getWidth().
 *   El Y del suelo es chunk.getOriginY() + groundHeight_offset.
 *
 * VERIFICACIÓN para chunk(0,0) con chunkWidth=1280, chunkHeight=720, ratio=0.25:
 *   groundHeight = 720 * 0.25 = 180
 *   groundY (local)  = 720 - 180 = 540    ← antes
 *   groundY (global) = 0 + 720 - 180 = 540  ← ahora para chunk(0,0)  ← IDÉNTICO
 *
 * VERIFICACIÓN para chunk(1,0):
 *   originX = 1280, originY = 0
 *   groundY (global) = 0 + 720 - 180 = 540
 *   Bloque en (1280, 540, ancho=1280, alto=180)  ← correcto (continuo con chunk 0,0)
 */
public class TerrainLayer implements WorldLayer {

    private final double groundRatio;

    /** Constructor por defecto: suelo = 25% de la altura del chunk. */
    public TerrainLayer() {
        this(0.25);
    }

    /**
     * Constructor con ratio configurable.
     *
     * @param groundRatio fracción de la altura del chunk que ocupa el suelo (0.0–1.0)
     */
    public TerrainLayer(double groundRatio) {
        if (groundRatio <= 0 || groundRatio >= 1) {
            throw new IllegalArgumentException(
                "groundRatio debe estar en (0, 1). Recibido: " + groundRatio);
        }
        this.groundRatio = groundRatio;
    }

    @Override
    public void generate(Chunk chunk, Random random) {
        int groundHeight = (int)(chunk.getHeight() * groundRatio);

        // Posición Y global del borde superior del suelo
        // = origen Y del chunk + altura del chunk - altura del suelo
        int groundY = chunk.getOriginY() + chunk.getHeight() - groundHeight;

        // Posición X global = origen X del chunk (borde izquierdo)
        int originX = chunk.getOriginX();

        chunk.add(WorldObjectFactory.groundBlock(
            originX,
            chunk.getWidth(),
            groundY,
            groundHeight,
            BlocksAssets.getSueloImage()
        ));
    }
}
