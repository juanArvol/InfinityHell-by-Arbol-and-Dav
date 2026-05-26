package Game.World.Generator;

import Game.World.Core.World;
import Game.World.Core.WorldCoordinator;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.Generator.Layer.Objects.BackGroundLayer;
import Game.World.Generator.Layer.Objects.ObstacleLayer;
import Game.World.Generator.Layer.Objects.TerrainLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generador de mundos basado en capas (WorldLayer).
 *
 * MEJORAS vs. versión original:
 *
 * 1. DESACOPLADO: el generador no conoce las capas concretas en el constructor.
 *    Se construye con un WorldGeneratorConfig que define qué capas usar.
 *    Esto permite crear mundos con distintas combinaciones (ej. solo urbano, solo
 *    bosque, mezcla) sin tocar esta clase.
 *
 * 2. CONFIGURABLE: WorldGeneratorConfig agrupa todas las decisiones de diseño
 *    del nivel (qué layers, en qué orden, con qué parámetros).
 *
 * 3. SEED EXPLÍCITA: generate() acepta una seed opcional. Si no se pasa,
 *    deriva la seed del WorldCoordinator (comportamiento original).
 *    Esto facilita testing de mundos específicos y reproducibilidad.
 *
 * 4. FACTORY METHODS: WorldGeneratorConfig.defaults() devuelve la configuración
 *    original. WorldGeneratorConfig.empty() sirve para tests.
 *
 * Uso mínimo (igual que antes):
 *   WorldGenerator gen = new WorldGenerator();
 *   World world = gen.generate(width, height, coord);
 *
 * Uso extendido (nueva API):
 *   WorldGeneratorConfig cfg = WorldGeneratorConfig.defaults()
 *       .addLayer(new MyCustomLayer());
 *   WorldGenerator gen = new WorldGenerator(cfg);
 *   World world = gen.generate(width, height, coord);
 */
public class WorldGenerator {

    private final WorldGeneratorConfig config;

    /** Constructor original — usa la configuración por defecto (retro-compatible). */
    public WorldGenerator() {
        this(WorldGeneratorConfig.defaults());
    }

    /** Constructor extendido — recibe una configuración personalizada. */
    public WorldGenerator(WorldGeneratorConfig config) {
        this.config = config;
    }

    /**
     * Genera un mundo con las capas registradas, usando la seed derivada del coord.
     * Retro-compatible con el código existente.
     */
    public World generate(int width, int height, WorldCoordinator coord) {
        long seed = coord.hashCode();
        return generate(width, height, coord, seed);
    }

    /**
     * Genera un mundo con seed explícita.
     * Útil para testing o recrear mundos concretos.
     *
     * @param seed seed determinista para la generación aleatoria
     */
    public World generate(int width, int height, WorldCoordinator coord, long seed) {
        World world = new World(width, height, coord);
        Random random = new Random(seed);

        for (WorldLayer layer : config.getLayers()) {
            layer.generate(world, random);
        }

        return world;
    }

    // ── Configuración ─────────────────────────────────────────────────────

    /**
     * Configuración inmutable de capas para el generador.
     *
     * Se construye mediante el patrón builder fluido:
     *   WorldGeneratorConfig cfg = WorldGeneratorConfig.empty()
     *       .addLayer(new BackGroundLayer())
     *       .addLayer(new TerrainLayer())
     *       .addLayer(new MyBuildingLayer());
     */
    public static final class WorldGeneratorConfig {

        private final List<WorldLayer> layers;

        private WorldGeneratorConfig(List<WorldLayer> layers) {
            this.layers = Collections.unmodifiableList(new ArrayList<>(layers));
        }

        /** Configuración vacía — sin capas. Punto de partida para builders. */
        public static WorldGeneratorConfig empty() {
            return new WorldGeneratorConfig(new ArrayList<>());
        }

        /**
         * Configuración por defecto — reproduce exactamente el comportamiento
         * del WorldGenerator original: fondo → terreno → obstáculos.
         */
        public static WorldGeneratorConfig defaults() {
            return empty()
                .addLayer(new BackGroundLayer())
                .addLayer(new TerrainLayer())
                .addLayer(new ObstacleLayer());
        }

        /** Devuelve una nueva configuración con la capa añadida al final. */
        public WorldGeneratorConfig addLayer(WorldLayer layer) {
            List<WorldLayer> next = new ArrayList<>(layers);
            next.add(layer);
            return new WorldGeneratorConfig(next);
        }

        /** Devuelve una nueva configuración con la capa insertada en la posición dada. */
        public WorldGeneratorConfig insertLayer(int index, WorldLayer layer) {
            List<WorldLayer> next = new ArrayList<>(layers);
            next.add(index, layer);
            return new WorldGeneratorConfig(next);
        }

        public List<WorldLayer> getLayers() {
            return layers;
        }
    }
}
