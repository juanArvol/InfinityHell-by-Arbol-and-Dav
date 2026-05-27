package Game.World.Generator;

import Game.World.Core.World;
import Game.World.Core.WorldCoordinator;
import Game.World.Generator.Layer.WorldLayer;

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

}
