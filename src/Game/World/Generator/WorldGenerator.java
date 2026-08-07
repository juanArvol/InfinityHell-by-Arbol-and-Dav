package Game.World.Generator;

import Game.World.Chunk.Chunk;
import Game.World.Core.World;
import Game.World.Core.WorldCoordinator;
import Game.World.Generator.Layer.WorldLayer;
import java.util.Random;

/**
 * Generador de mundos basado en capas (WorldLayer).
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: generate() creaba un World y pasaba ese World a cada WorldLayer.
 *   Las layers generaban objetos con posiciones locales [0, width) × [0, height).
 *
 * AHORA: generate() crea un Chunk con su origen global y lo pasa a cada layer.
 *   Las layers generan objetos con posiciones globales:
 *     globalX = chunk.getOriginX() + localX
 *     globalY = chunk.getOriginY() + localY
 *
 *   Para el chunk (0,0): originX=0, originY=0 → numéricamente idéntico al anterior.
 *   Para el chunk (1,0): originX=chunkWidth → objetos en [chunkWidth, 2*chunkWidth).
 *
 * ── RETROCOMPATIBILIDAD ───────────────────────────────────────────────────
 * Los métodos generate(int, int, WorldCoordinator) y su variante con seed
 * se mantienen con la misma firma, pero ahora retornan Chunk en lugar de World.
 *
 * Durante la transición, WorldCache todavía trabaja con World. WorldManager
 * seguirá llamando a generate() y envolviendo el Chunk en un World hasta
 * la Etapa 3, donde World se convierte en el agregador global.
 *
 * ── DETERMINISMO ──────────────────────────────────────────────────────────
 * La seed se sigue derivando de coord.hashCode() para garantizar que el
 * mismo chunk siempre produce el mismo contenido.
 */
public class WorldGenerator {

    private final WorldGeneratorConfig config;

    /** Constructor original — usa la configuración por defecto (retrocompatible). */
    public WorldGenerator() {
        this(WorldGeneratorConfig.defaults());
    }

    /** Constructor extendido — recibe una configuración personalizada. */
    public WorldGenerator(WorldGeneratorConfig config) {
        this.config = config;
    }

    // ── API principal: genera un Chunk ────────────────────────────────────

    /**
     * Genera un Chunk con todas las capas registradas.
     * La seed se deriva de coord.hashCode() para determinismo.
     *
     * Los objetos generados tienen posiciones en COORDENADAS GLOBALES.
     * El chunk (0,0) produce exactamente el mismo resultado visual que antes.
     *
     * @param width  ancho del chunk en píxeles
     * @param height alto del chunk en píxeles
     * @param coord  coordenada del chunk en la grilla
     * @return Chunk completamente generado y marcado como loaded
     */
    public Chunk generateChunk(int width, int height, WorldCoordinator coord) {
        long seed = coord.hashCode();
        return generateChunk(width, height, coord, seed);
    }

    /**
     * Genera un Chunk con seed explícita.
     * Útil para testing reproducible o recrear chunks concretos.
     *
     * @param width  ancho del chunk en píxeles
     * @param height alto del chunk en píxeles
     * @param coord  coordenada del chunk en la grilla
     * @param seed   seed determinista para la generación
     * @return Chunk completamente generado y marcado como loaded
     */
    public Chunk generateChunk(int width, int height, WorldCoordinator coord, long seed) {
        Chunk chunk = new Chunk(coord, width, height);
        Random random = new Random(seed);

        for (WorldLayer layer : config.getLayers()) {
            layer.generate(chunk, random);
        }

        chunk.markLoaded();
        return chunk;
    }

    // ── API de compatibilidad: genera un World (DEPRECATED — Etapa 3) ─────

    /**
     * Genera un World envolviendo un Chunk generado.
     *
     * @deprecated Este método existe exclusivamente para compatibilidad con
     *             WorldCache y WorldManager durante la transición.
     *             Será eliminado en la Etapa 3 cuando World se convierta en
     *             el agregador global y WorldCache pase a ChunkStorage.
     *             Usar {@link #generateChunk(int, int, WorldCoordinator)}.
     *
     * @param width  ancho del sector en píxeles
     * @param height alto del sector en píxeles
     * @param coord  coordenada del sector
     * @return World con los objetos del chunk generado
     */
    @Deprecated(forRemoval = true)
    public World generate(int width, int height, WorldCoordinator coord) {
        long seed = coord.hashCode();
        return generate(width, height, coord, seed);
    }

    /**
     * Genera un World con seed explícita.
     *
     * @deprecated Ver {@link #generate(int, int, WorldCoordinator)}.
     */
    @Deprecated(forRemoval = true)
    public World generate(int width, int height, WorldCoordinator coord, long seed) {
        // Generar el Chunk con posiciones globales
        Chunk chunk = generateChunk(width, height, coord, seed);

        // Envolver en un World para compatibilidad con el sistema actual.
        // CORRECCIÓN CRÍTICA: usar world.addChunk(chunk) para que:
        //   1. Los objetos estáticos vayan al ChunkStorage del World (correcto).
        //   2. SimulationRegion.rebuildFromStorage() los encuentre en el storage.
        //   3. El terreno NO termine en el globalDynamicRegistry como entidad dinámica.
        //   4. El terreno NO reciba update() de física cada frame.
        World world = new World(width, height, coord);
        world.addChunk(chunk);
        return world;
    }
}
