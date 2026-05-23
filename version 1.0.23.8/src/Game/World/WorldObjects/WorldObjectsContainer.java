package Game.World.WorldObjects;

import Game.Engine.GameObjects;
import Game.Engine.Systems.CollisionsSystem;

import java.util.ArrayList;
import java.util.List;

public class WorldObjectsContainer {

    private final List<GameObjects> objects = new ArrayList<>();
    private final List<GameObjects> pendingAdd = new ArrayList<>();
    private final List<GameObjects> pendingRemove = new ArrayList<>();

    private final CollisionsSystem collisionsSystem = new CollisionsSystem();

    public void update() {
        flush();
        for (GameObjects obj : objects) {
            obj.update();
        }
        collisionsSystem.update(objects);

    }

    public void flush() {
        if (!pendingAdd.isEmpty()) {
            objects.addAll(pendingAdd);
            pendingAdd.clear();
        }

        if (!pendingRemove.isEmpty()) {
            objects.removeAll(pendingRemove);
            pendingRemove.clear();
        }
    }

    public void add(GameObjects obj) {
        pendingAdd.add(obj);
    }

    public void remove(GameObjects obj) {
        pendingRemove.add(obj);
    }

    public List<GameObjects> getObjects() {
        return objects;
    }
}