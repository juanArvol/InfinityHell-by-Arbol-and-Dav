package Game.World.Generator.Layer.Objects;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Chunk.Chunk;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.Visuals.BackGround;
import java.util.Random;

/**
 * Capa de fondo base del mundo.
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: la posición del fondo era (0, 0) — borde superior izquierdo del chunk
 * en coordenadas locales. Correcto para el chunk (0,0), incorrecto para (1,0).
 *
 * AHORA: la posición usa chunk.getOriginX/Y() para obtener el origen global.
 *
 *   Chunk(0,0): originX=0,    originY=0    → BackGround en (0, 0)      ← idéntico
 *   Chunk(1,0): originX=1280, originY=0    → BackGround en (1280, 0)   ← correcto
 *   Chunk(0,1): originX=0,    originY=720  → BackGround en (0, 720)    ← correcto
 *
 * El resultado visual para el chunk (0,0) es idéntico al anterior.
 */
public class BackGroundLayer implements WorldLayer {

    @Override
    public void generate(Chunk chunk, Random random) {
        // La posición del fondo es el origen global del chunk.
        // Para chunk(0,0): (0,0) — idéntico al comportamiento anterior.
        chunk.add(new BackGround(
            new Vector2D(chunk.getOriginX(), chunk.getOriginY()),
            null,
            chunk.getWidth(),
            chunk.getHeight()
        ));
    }
}
