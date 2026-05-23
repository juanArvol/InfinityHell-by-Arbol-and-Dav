package Game.World.Core;

import java.util.ArrayList;
import java.util.List;

public class WorldCache {

    private final List<World> worlds = new ArrayList<>();

    public void put(World world) {
        if (!contains(world.getCoordinate())) {
            worlds.add(world);
        }
    }

    public World get(WorldCoordinator coord) {
        for (World world : worlds) {
            if (world.getCoordinate().equals(coord)) {
                return world;
            }
        }
        return null; // No existe en cache
    }

    public boolean contains(WorldCoordinator coord) {
        return get(coord) != null;
    }

    public List<World> getAllWorlds() {
        return worlds;
    }

    public void clear() {
        worlds.clear();
    }
}