package Game.World.Generator;

import Game.World.Generator.Layer.WorldLayer;
import Game.World.Generator.Layer.Objects.BackGroundLayer;
import Game.World.Generator.Layer.Objects.ObstacleLayer;
import Game.World.Generator.Layer.Objects.TerrainLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuración de capas para WorldGenerator.
 *
 * Se construye mediante el patrón builder fluido:
 *
 *   WorldGeneratorConfig cfg = WorldGeneratorConfig.empty()
 *       .addLayer(new BackGroundLayer())
 *       .addLayer(new TerrainLayer())
 *       .addLayer(new MyBuildingLayer());
 *
 *   WorldGenerator gen = new WorldGenerator(cfg);
 *
 * Uso rápido con la configuración por defecto (fondo → terreno → obstáculos):
 *
 *   WorldGenerator gen = new WorldGenerator(); // usa defaults() internamente
 */
public final class WorldGeneratorConfig {

    private final List<WorldLayer> layers;

    private WorldGeneratorConfig(List<WorldLayer> layers) {
        this.layers = Collections.unmodifiableList(new ArrayList<>(layers));
    }

    /** Configuración vacía — sin capas. Punto de partida para builders. */
    public static WorldGeneratorConfig empty() {
        return new WorldGeneratorConfig(new ArrayList<>());
    }

    /**
     * Configuración por defecto — reproduce el comportamiento original del
     * WorldGenerator: fondo → terreno → obstáculos.
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
