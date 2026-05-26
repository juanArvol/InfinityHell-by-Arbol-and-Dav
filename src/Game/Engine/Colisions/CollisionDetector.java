package Game.Engine.Colisions;

import Game.Engine.GameObjects;
import Game.Engine.Components.Collisions.ColliderComponent;

import java.awt.Rectangle;
import java.util.*;

/**
 * Detecta pares de objetos que se solapan usando AABB + spatial hash.
 *
 * ── Responsabilidad única ────────────────────────────────────────────────
 * Solo DETECTA. No mueve objetos, no llama eventos, no aplica física.
 * Retorna una lista de CollisionResult para que CollisionsSystem los procese.
 *
 * ── Spatial hash ─────────────────────────────────────────────────────────
 * Divide el mundo en celdas de CELL_SIZE px. Cada objeto se registra en
 * las celdas que toca. Solo se comparan objetos en la misma celda → O(n)
 * en lugar de O(n²) para mundos con objetos distribuidos.
 */
public final class CollisionDetector {

    private static final int CELL_SIZE = 128;

    private CollisionDetector() {}

    /**
     * Detecta todos los pares de objetos que se solapan este frame.
     *
     * @param objects lista de todos los objetos del mundo
     * @return lista de pares en colisión (sin duplicados)
     */
    public static List<CollisionResult> detect(List<GameObjects> objects) {

        Map<Long, List<GameObjects>> grid = new HashMap<>();
        List<CollisionResult> results = new ArrayList<>();

        // Registrar cada objeto en sus celdas
        for (GameObjects obj : objects) {
            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null) continue;

            Rectangle b = col.getBounds();
            int x0 = Math.floorDiv(b.x, CELL_SIZE);
            int y0 = Math.floorDiv(b.y, CELL_SIZE);
            int x1 = Math.floorDiv(b.x + b.width,  CELL_SIZE);
            int y1 = Math.floorDiv(b.y + b.height, CELL_SIZE);

            for (int cx = x0; cx <= x1; cx++) {
                for (int cy = y0; cy <= y1; cy++) {
                    long cellKey = ((long) cx << 32) | (cy & 0xFFFFFFFFL);
                    grid.computeIfAbsent(cellKey, k -> new ArrayList<>()).add(obj);
                }
            }
        }

        // Comparar pares dentro de cada celda
        // Usamos IdentityHashMap para IDs únicos garantizados (no hashCode)
        IdentityHashMap<GameObjects, Integer> indexMap = new IdentityHashMap<>(objects.size() * 2);
        for (int i = 0; i < objects.size(); i++) indexMap.put(objects.get(i), i);

        Set<Long> checked = new HashSet<>();

        for (List<GameObjects> cell : grid.values()) {
            int size = cell.size();
            for (int i = 0; i < size; i++) {
                GameObjects a    = cell.get(i);
                ColliderComponent colA = a.getComponent(ColliderComponent.class);
                if (colA == null) continue;

                for (int j = i + 1; j < size; j++) {
                    GameObjects b    = cell.get(j);
                    ColliderComponent colB = b.getComponent(ColliderComponent.class);
                    if (colB == null) continue;

                    // Filtro de capas
                    if (!colA.canCollideWith(colB)) continue;

                    // Evitar duplicados entre celdas
                    int idxA = indexMap.getOrDefault(a, -1);
                    int idxB = indexMap.getOrDefault(b, -1);
                    if (idxA < 0 || idxB < 0) continue;

                    long pairKey = idxA < idxB
                            ? ((long) idxA << 32) | idxB
                            : ((long) idxB << 32) | idxA;
                    if (!checked.add(pairKey)) continue;

                    // Comprobación AABB
                    if (colA.getBounds().intersects(colB.getBounds())) {
                        boolean isTrigger = colA.isTrigger() || colB.isTrigger();
                        results.add(new CollisionResult(a, b, isTrigger));
                    }
                }
            }
        }

        return results;
    }
}
