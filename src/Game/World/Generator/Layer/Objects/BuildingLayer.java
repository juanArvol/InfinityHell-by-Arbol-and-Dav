package Game.World.Generator.Layer.Objects;

import Game.World.Chunk.Chunk;
import Game.World.Generator.Layer.WorldLayer;
import Game.World.WorldObjects.WorldObjectFactory;
import Sprites.Enviroment.Obstacles.ObstaclesAssets;
import java.util.Random;

/**
 * Capa de edificios — genera estructuras rectangulares (muros) en el mundo.
 *
 * ── MIGRACIÓN A COORDENADAS GLOBALES (ETAPA 2) ────────────────────────────
 *
 * ANTES: origen del edificio calculado como random.nextInt(maxX) en coords locales.
 *
 * AHORA: origen del edificio = chunk.getOriginX() + random.nextInt(maxLocalX)
 *
 * VERIFICACIÓN para chunk(0,0): originX=0 → resultado idéntico al anterior.
 * VERIFICACIÓN para chunk(1,0): edificios en [1280+40, 1280+maxX] → correcto.
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
    public void generate(Chunk chunk, Random random) {
        int range = maxBuildings - minBuildings;
        int count = (range > 0) ? minBuildings + random.nextInt(range) : minBuildings;

        int marginX = buildingW + 40;
        int marginY = buildingH + 40;

        int maxLocalX = chunk.getWidth()  - marginX;
        int maxLocalY = chunk.getHeight() - marginY;
        if (maxLocalX <= 0 || maxLocalY <= 0) return;

        int originX = chunk.getOriginX();
        int originY = chunk.getOriginY();

        for (int i = 0; i < count; i++) {
            // Posición local dentro del chunk con margen
            int localX = 40 + random.nextInt(maxLocalX);
            int localY = 40 + random.nextInt(maxLocalY);

            // Convertir a coordenadas globales
            int globalX = originX + localX;
            int globalY = originY + localY;

            placeBuilding(chunk, globalX, globalY);
        }
    }

    private void placeBuilding(Chunk chunk, int ox, int oy) {
        var spr = ObstaclesAssets.getMondongoImage();

        // Todas las posiciones son ya globales (ox, oy son globales)
        chunk.add(WorldObjectFactory.obstacle(ox, oy, buildingW, wallThickness, spr));
        chunk.add(WorldObjectFactory.obstacle(ox, oy + buildingH - wallThickness, buildingW, wallThickness, spr));
        chunk.add(WorldObjectFactory.obstacle(ox, oy + wallThickness, wallThickness, buildingH - 2 * wallThickness, spr));
        chunk.add(WorldObjectFactory.obstacle(ox + buildingW - wallThickness, oy + wallThickness, wallThickness, buildingH - 2 * wallThickness, spr));
    }
}
