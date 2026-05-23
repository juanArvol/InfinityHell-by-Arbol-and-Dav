package Game.Engine.Colisions;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;

import java.awt.Rectangle;
import java.util.*;

public class CollisionManager {

    private static final int CELL_SIZE = 128;

    private static boolean canCollide(ColliderComponent a, ColliderComponent b) {
        return (a.getMask() & b.getLayer()) != 0 &&
               (b.getMask() & a.getLayer()) != 0;
    }

    public static List<CollisionsPair> detect(List<GameObjects> objects) {

        Map<Cell, List<GameObjects>> grid = new HashMap<>();
        List<CollisionsPair> collisions = new ArrayList<>();

        for (GameObjects obj : objects) {

            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null) continue;

            Rectangle bounds = col.getBounds();

            int startX = bounds.x / CELL_SIZE;
            int startY = bounds.y / CELL_SIZE;
            int endX = (bounds.x + bounds.width) / CELL_SIZE;
            int endY = (bounds.y + bounds.height) / CELL_SIZE;

            for (int x = startX; x <= endX; x++) {
                for (int y = startY; y <= endY; y++) {

                    Cell cell = new Cell(x, y);

                    grid.computeIfAbsent(cell, k -> new ArrayList<>())
                            .add(obj);
                }
            }
        }

        Set<Long> checked = new HashSet<>();

        for (List<GameObjects> cellObjects : grid.values()) {

            int size = cellObjects.size();

            for (int i = 0; i < size; i++) {

                GameObjects a = cellObjects.get(i);
                ColliderComponent colA =
                        a.getComponent(ColliderComponent.class);

                if (colA == null) continue;

                Rectangle boundsA = colA.getBounds();

                for (int j = i + 1; j < size; j++) {

                    GameObjects b = cellObjects.get(j);
                    ColliderComponent colB =
                            b.getComponent(ColliderComponent.class);

                    if (colB == null) continue;
                    if (!canCollide(colA, colB)) continue;

                    long idA = System.identityHashCode(a);
                    long idB = System.identityHashCode(b);

                    long key = (idA < idB)
                            ? (idA << 32) | idB
                            : (idB << 32) | idA;

                    if (!checked.add(key)) continue;

                    Rectangle boundsB = colB.getBounds();

                    if (boundsA.intersects(boundsB)) {

                        boolean trigger =
                                colA.getType() == ColliderComponent.ColliderType.TRIGGER ||
                                colB.getType() == ColliderComponent.ColliderType.TRIGGER;

                        collisions.add(new CollisionsPair(a, b, trigger));
                    }
                }
            }
        }

        return collisions;
    }

    private record Cell(int x, int y) {}
}