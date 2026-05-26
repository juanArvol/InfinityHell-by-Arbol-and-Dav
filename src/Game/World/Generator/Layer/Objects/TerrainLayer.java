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

        int width  = world.getWidth();
        int height = world.getHeight();

        int groundHeight = height / 4;
        int groundY      = height - groundHeight;

        // FIX: el bloque del suelo tiene height=groundHeight (150px), NO groundHeight*4 (600px).
        // El bloque gigante bloqueaba el movimiento horizontal del jugador:
        // su collider de 600px de alto hacía que el SweptAABB eje-X lo detectara
        // como obstáculo lateral aunque el jugador estuviera parado encima.
        BlockWorld ground = new BlockWorld(
                new Vector2D(0, groundY),
                BlocksAssets.suelo.getSprite(),
                width,
                groundHeight   // ← era groundHeight*4, bug del generador
        );
        world.add(ground);
    }
}
