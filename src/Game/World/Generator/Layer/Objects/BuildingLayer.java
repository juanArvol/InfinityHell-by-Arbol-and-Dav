package Game.World.Generator.Layer.Objects;

import Game.World.Core.World;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Graficos.Obstacles.ObstaclesAssets;

import java.util.Random;

/**
 * Capa de edificios — genera estructuras rectangulares (muros) en el mundo.
 *
 * Cada "edificio" es un conjunto de 2-4 obstáculos que forman las paredes.
 * El interior queda vacío para que el jugador pueda entrar.
 *
 * Extensible: cuando exista un tipo Building concreto con puerta y loot
 * interior, se puede subclasificar o añadir esa lógica aquí.
 *
 * Uso:
 *   .addLayer(new BuildingLayer(2, 4))  // 2 a 4 edificios por chunk
 */
public class BuildingLayer implements WorldLayer {

    private final int minBuildings;
    private final int maxBuildings;
    private final int buildingW;
    private final int buildingH;
    private final int wallThickness;

    public BuildingLayer() {
        this(2, 4, 160, 120, 20);
    }

    public BuildingLayer(int minBuildings, int maxBuildings) {
        this(minBuildings, maxBuildings, 160, 120, 20);
    }

    public BuildingLayer(int minBuildings, int maxBuildings,
                         int buildingW, int buildingH, int wallThickness) {
        this.minBuildings  = minBuildings;
        this.maxBuildings  = maxBuildings;
        this.buildingW     = buildingW;
        this.buildingH     = buildingH;
        this.wallThickness = wallThickness;
    }

    @Override
    public void generate(World world, Random random) {
        int range = maxBuildings - minBuildings;
        int count = (range > 0) ? minBuildings + random.nextInt(range) : minBuildings;

        int marginX = buildingW + 40;
        int marginY = buildingH + 40;

        int maxX = world.getWidth()  - marginX;
        int maxY = world.getHeight() - marginY;
        if (maxX <= 0 || maxY <= 0) return;

        for (int i = 0; i < count; i++) {
            int originX = 40 + random.nextInt(maxX);
            int originY = 40 + random.nextInt(maxY);
            placeBuilding(world, originX, originY);
        }
    }

    private void placeBuilding(World world, int ox, int oy) {
        var spr = ObstaclesAssets.mondongo.getSprite(); // Reemplazar con WallAsset cuando exista

        // Pared superior
        world.add(WorldObjectFactory.obstacle(ox, oy, buildingW, wallThickness, spr));
        // Pared inferior
        world.add(WorldObjectFactory.obstacle(ox, oy + buildingH - wallThickness, buildingW, wallThickness, spr));
        // Pared izquierda (sin esquinas para no duplicar)
        world.add(WorldObjectFactory.obstacle(ox, oy + wallThickness, wallThickness, buildingH - 2 * wallThickness, spr));
        // Pared derecha
        world.add(WorldObjectFactory.obstacle(ox + buildingW - wallThickness, oy + wallThickness, wallThickness, buildingH - 2 * wallThickness, spr));
        // La "puerta" queda abierta — el centro de la pared inferior no se coloca
    }
}
