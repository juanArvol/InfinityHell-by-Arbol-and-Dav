package Game.World.Generator.Layer;

import Game.World.Chunk.Chunk;
import Game.World.Core.World;
import java.util.Random;

/**
 * Contrato de una capa de generación del mundo.
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: generate(World world, Random random)
 *   Las layers recibían un World cuyas dimensiones eran las del chunk.
 *   Todos los objetos se generaban con posiciones locales [0, width) × [0, height).
 *   Eso significaba que todos los objetos del chunk (1,0) empezaban en x=0
 *   aunque deberían estar en x=1280.
 *
 * AHORA: generate(Chunk chunk, Random random)
 *   Las layers reciben un Chunk que conoce su origen global (originX, originY).
 *   Todos los objetos deben generarse en coordenadas globales:
 *
 *     int globalX = chunk.getOriginX() + localX;
 *     int globalY = chunk.getOriginY() + localY;
 *
 *   El chunk (0,0) tiene originX=0, originY=0 → resultado idéntico al anterior.
 *   El chunk (1,0) tiene originX=1280 → los objetos quedan en [1280, 2560).
 *
 * ── RETROCOMPATIBILIDAD DURANTE LA TRANSICIÓN ─────────────────────────────
 * La firma vieja generate(World, Random) se mantiene como @Deprecated default
 * que lanza UnsupportedOperationException. Esto permite que el compilador
 * detecte cualquier implementación no migrada como error en runtime, sin
 * romper la compilación.
 *
 * Una vez que WorldGenerator.generate() ya no llame a la firma vieja (Etapa 2),
 * la firma legacy puede eliminarse en la Etapa 9.
 */
public interface WorldLayer {

    /**
     * Genera el contenido de esta capa dentro del chunk dado.
     *
     * Todas las posiciones de los objetos añadidos al chunk deben estar en
     * COORDENADAS GLOBALES de mundo:
     *
     *   int globalX = chunk.getOriginX() + localX;
     *   int globalY = chunk.getOriginY() + localY;
     *
     * Para el chunk (0,0), originX=0 y originY=0, por lo que el resultado
     * es numéricamente idéntico al comportamiento anterior.
     *
     * @param chunk  el chunk que se está generando; contiene su origen global
     * @param random generador pseudoaleatorio con seed derivada del chunk
     */
    void generate(Chunk chunk, Random random);

    /**
     * Firma legacy — solo para compatibilidad de compilación temporal.
     *
     * @deprecated Usar {@link #generate(Chunk, Random)}. Esta sobrecarga
     *             será eliminada en la Etapa 9 de la migración.
     */
    @Deprecated(forRemoval = true)
    default void generate(World world, Random random) {
        throw new UnsupportedOperationException(
            "WorldLayer.generate(World, Random) está deprecado. " +
            "Implementar generate(Chunk, Random) en: " + getClass().getName()
        );
    }
}
