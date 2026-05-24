package Game.World.WorldObjects;

import Game.Bullets.Bullet;
import Game.Bullets.BulletLife;
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

        // FIX ARCH-09: limpiar balas muertas cada frame para evitar memory leak
        for (GameObjects obj : objects) {
            if (obj instanceof Bullet bullet) {
                if (!bullet.getBulletLife().isAlive()) {
                    pendingRemove.add(obj);
                }
            }
        }
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