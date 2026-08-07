package Game.World.Chunk;

import Game.World.Core.WorldCoordinator;

/**
 * Utilidades de conversión entre coordenadas globales de mundo y
 * coordenadas de grilla de chunks.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Centraliza toda la aritmética de la grilla de chunks. Ningún otro sistema
 * debe duplicar estas fórmulas. Clase utilitaria no instanciable.
 *
 * ── CONVENCIÓN DE COORDENADAS ─────────────────────────────────────────────
 * Coordenada global (gx, gy): posición en píxeles del mundo continuo.
 * Coordenada de chunk (cx, cy): índice entero en la grilla de chunks.
 *
 *   Chunk(0,0) cubre X ∈ [0,      chunkWidth)   Y ∈ [0,       chunkHeight)
 *   Chunk(1,0) cubre X ∈ [cW,     2*cW)         Y ∈ [0,       cH)
 *   Chunk(-1,0) cubre X ∈ [-cW,   0)            Y ∈ [0,       cH)
 *
 * Usa Math.floorDiv para manejar correctamente coordenadas negativas:
 *   floorDiv(-1, 1280) = -1  (correcto)
 *   (-1 / 1280) = 0          (incorrecto — truncación entera de Java)
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Obtener el chunk en el que está un objeto:
 *   WorldCoordinator chunk = GlobalChunkResolver.toChunk(player.x, player.y, cW, cH);
 *
 *   // Obtener el origen global de un chunk:
 *   int originX = GlobalChunkResolver.originX(coord, chunkWidth);
 *   int originY = GlobalChunkResolver.originY(coord, chunkHeight);
 *
 *   // Convertir posición local dentro del chunk a global:
 *   double gx = GlobalChunkResolver.toGlobalX(coord, localX, chunkWidth);
 */
public final class GlobalChunkResolver {

    private GlobalChunkResolver() {}

    // ── Global → Chunk ────────────────────────────────────────────────────

    /**
     * Índice X (columna) del chunk que contiene la coordenada global dada.
     *
     * @param globalX   posición X en coordenadas de mundo
     * @param chunkWidth ancho de cada chunk en píxeles
     * @return índice de columna del chunk (puede ser negativo)
     */
    public static int chunkX(double globalX, int chunkWidth) {
        return Math.floorDiv((int) globalX, chunkWidth);
    }

    /**
     * Índice Y (fila) del chunk que contiene la coordenada global dada.
     *
     * @param globalY    posición Y en coordenadas de mundo
     * @param chunkHeight alto de cada chunk en píxeles
     * @return índice de fila del chunk (puede ser negativo)
     */
    public static int chunkY(double globalY, int chunkHeight) {
        return Math.floorDiv((int) globalY, chunkHeight);
    }

    /**
     * WorldCoordinator del chunk que contiene la posición global (gx, gy).
     *
     * @param globalX     posición X en coordenadas de mundo
     * @param globalY     posición Y en coordenadas de mundo
     * @param chunkWidth  ancho de cada chunk en píxeles
     * @param chunkHeight alto de cada chunk en píxeles
     * @return el WorldCoordinator del chunk contenedor
     */
    public static WorldCoordinator toChunk(double globalX, double globalY,
                                            int chunkWidth, int chunkHeight) {
        return new WorldCoordinator(
            chunkX(globalX, chunkWidth),
            chunkY(globalY, chunkHeight)
        );
    }

    // ── Chunk → Global ────────────────────────────────────────────────────

    /**
     * Coordenada X global del borde izquierdo de un chunk.
     *
     * @param coord      coordenada del chunk
     * @param chunkWidth ancho de cada chunk en píxeles
     * @return posición X global del origen del chunk
     */
    public static int originX(WorldCoordinator coord, int chunkWidth) {
        return coord.x() * chunkWidth;
    }

    /**
     * Coordenada Y global del borde superior de un chunk.
     *
     * @param coord       coordenada del chunk
     * @param chunkHeight alto de cada chunk en píxeles
     * @return posición Y global del origen del chunk
     */
    public static int originY(WorldCoordinator coord, int chunkHeight) {
        return coord.y() * chunkHeight;
    }

    // ── Local → Global ────────────────────────────────────────────────────

    /**
     * Convierte una posición X local (relativa al origen del chunk) a global.
     *
     * @param coord      coordenada del chunk
     * @param localX     posición X relativa al origen del chunk
     * @param chunkWidth ancho de cada chunk en píxeles
     * @return posición X en coordenadas globales de mundo
     */
    public static double toGlobalX(WorldCoordinator coord, double localX, int chunkWidth) {
        return originX(coord, chunkWidth) + localX;
    }

    /**
     * Convierte una posición Y local (relativa al origen del chunk) a global.
     *
     * @param coord       coordenada del chunk
     * @param localY      posición Y relativa al origen del chunk
     * @param chunkHeight alto de cada chunk en píxeles
     * @return posición Y en coordenadas globales de mundo
     */
    public static double toGlobalY(WorldCoordinator coord, double localY, int chunkHeight) {
        return originY(coord, chunkHeight) + localY;
    }

    // ── Global → Local ────────────────────────────────────────────────────

    /**
     * Convierte una posición X global a local (relativa al origen del chunk).
     *
     * @param globalX    posición X global
     * @param chunkWidth ancho de cada chunk en píxeles
     * @return posición X local dentro de su chunk, en [0, chunkWidth)
     */
    public static double toLocalX(double globalX, int chunkWidth) {
        int origin = chunkX(globalX, chunkWidth) * chunkWidth;
        return globalX - origin;
    }

    /**
     * Convierte una posición Y global a local (relativa al origen del chunk).
     *
     * @param globalY     posición Y global
     * @param chunkHeight alto de cada chunk en píxeles
     * @return posición Y local dentro de su chunk, en [0, chunkHeight)
     */
    public static double toLocalY(double globalY, int chunkHeight) {
        int origin = chunkY(globalY, chunkHeight) * chunkHeight;
        return globalY - origin;
    }

    // ── Chunks en rango ───────────────────────────────────────────────────

    /**
     * Devuelve el índice mínimo de chunk que intersecta con el rango global
     * [globalMin, globalMax) en un eje dado.
     *
     * @param globalMin  inicio del rango en píxeles globales
     * @param chunkSize  tamaño del chunk en ese eje
     * @return índice del primer chunk que toca el rango
     */
    public static int firstChunkIndex(double globalMin, int chunkSize) {
        return Math.floorDiv((int) globalMin, chunkSize);
    }

    /**
     * Devuelve el índice máximo (inclusive) de chunk que intersecta con
     * el rango global [globalMin, globalMax) en un eje dado.
     *
     * @param globalMax  fin del rango en píxeles globales (exclusive)
     * @param chunkSize  tamaño del chunk en ese eje
     * @return índice del último chunk que toca el rango
     */
    public static int lastChunkIndex(double globalMax, int chunkSize) {
        // globalMax - 1 para que un rango que termina exactamente en el borde
        // de un chunk no incluya el chunk siguiente.
        return Math.floorDiv((int) Math.ceil(globalMax) - 1, chunkSize);
    }
}
