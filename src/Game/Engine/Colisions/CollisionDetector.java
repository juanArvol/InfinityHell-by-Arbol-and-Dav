package Game.Engine.Colisions;

import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameObjects;
import java.awt.Rectangle;
import java.util.*;

/**
 * Detecta pares de objetos que se solapan usando AABB + spatial hash.
 *
 * ── HRFC — Collision System ────────────────────────────────────────────────
 *
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR:
 *
 *   ANTES:
 *     - Clase de métodos estáticos — no había instancia
 *     - Creaba HashMap, IdentityHashMap y HashSet NUEVOS en cada llamada
 *     - O(n) allocations por frame en un Bullet Hell → presión de GC
 *     - CollisionResult sin normal (0, 0) siempre
 *
 *   AHORA:
 *     - Clase instanciable — CollisionsSystem mantiene una instancia
 *     - HashMap, IdentityHashMap y HashSet se reutilizan entre frames (clear())
 *     - CollisionResult incluye la normal calculada desde la geometría relativa
 *     - Guard de Destroyable: pares donde un objeto está muerto se omiten
 *     - Los pares ya despachados en FASE 1 (sólidos) se excluyen via skipSet
 *
 * ── Responsabilidad única ─────────────────────────────────────────────────
 * Solo DETECTA pares residuales (FASE 2). No mueve objetos, no llama eventos.
 * Los pares de TRIGGERS ya se procesaron en FASE 1B de CollisionsSystem.
 * Esta fase detecta overlaps estáticos entre pares que no participaron en FASE 1.
 *
 * ── Spatial hash ─────────────────────────────────────────────────────────
 * Divide el mundo en celdas de CELL_SIZE px. Cada objeto se registra en
 * las celdas que toca. Solo se comparan objetos en la misma celda → O(n)
 * en lugar de O(n²) para mundos con objetos distribuidos.
 *
 * ── Cálculo de normal en FASE 2 ──────────────────────────────────────────
 * Para pares detectados por overlap AABB (no swept), la normal se calcula
 * geométricamente desde el centro relativo:
 *
 *   1. Centro A y centro B
 *   2. Vector diferencia: dx = cx(A) - cx(B), dy = cy(A) - cy(B)
 *   3. Penetración en cada eje
 *   4. El eje de menor penetración determina la normal
 *
 * Esta normal es aproximada (no es tan precisa como la de SweptAABB) pero
 * correcta para overlaps residuales estáticos.
 *
 * GARANTÍA: computeOverlapNormal() nunca retorna (0,0) cuando hay overlap
 * real — si la penetración es cero en un eje (borde exacto por truncado int),
 * usa el vector centro-a-centro como fallback geométrico. Esto garantiza que
 * los behaviors como BulletJump siempre tengan una normal válida en FASE 2
 * y no necesiten recurrir al fallback heurístico de velocidad.
 */
public final class CollisionDetector {

    private static final int CELL_SIZE = 128;

    // ── Colecciones reutilizables — eliminan allocations por frame ────────
    //
    // En lugar de crear nuevas colecciones en cada llamada a detect(),
    // se reutilizan las mismas instancias limpiándolas con clear().
    // Esto reduce sustancialmente la presión sobre el GC en un Bullet Hell
    // donde detect() se llama 30–60 veces por segundo con cientos de objetos.

    private final HashMap<Long, List<GameObjects>>  grid     = new HashMap<>(256);
    private final IdentityHashMap<GameObjects, Integer> indexMap = new IdentityHashMap<>(256);
    private final HashSet<Long>                     checked  = new HashSet<>(512);

    // ── skipSet: pares ya despachados en FASE 1 ───────────────────────────
    //
    // CollisionsSystem registra aquí los pares procesados en FASE 1 (SweptAABB
    // para sólidos) y FASE 1B (SweptAABB para triggers). detect() los omite
    // para evitar dispatch duplicado en el mismo frame.
    // Se limpia al inicio de cada llamada a detect().
    private final HashSet<Long> skipSet = new HashSet<>(64);

    // ── API pública ────────────────────────────────────────────────────────

    /**
     * Registra un par de objetos como ya despachado en FASE 1 o FASE 1B.
     * CollisionsSystem llama este método después de cada dispatch de swept,
     * antes de llamar a detect(). detect() omitirá este par.
     *
     * @param a primer objeto del par
     * @param b segundo objeto del par
     */
    public void markDispatched(GameObjects a, GameObjects b) {
        // Usar identidad de objetos para la clave — mismo contrato que indexMap
        int idxA = System.identityHashCode(a);
        int idxB = System.identityHashCode(b);
        long pairKey = idxA < idxB
                ? ((long) idxA << 32) | (idxB & 0xFFFFFFFFL)
                : ((long) idxB << 32) | (idxA & 0xFFFFFFFFL);
        skipSet.add(pairKey);
    }

    /**
     * Detecta todos los pares de objetos que se solapan este frame.
     * Excluye los pares ya procesados en FASE 1 (registrados via markDispatched).
     * Excluye objetos marcados como Destroyable.isPendingDestruction().
     *
     * @param objects lista de todos los objetos activos del frame
     * @return lista de pares en colisión (sin duplicados, sin pares ya despachados)
     */
    public List<CollisionResult> detect(List<GameObjects> objects) {

        // Limpiar estructuras del frame anterior
        grid.clear();
        indexMap.clear();
        checked.clear();
        // skipSet se limpia al final — el caller lo popula antes de llamar detect()

        List<CollisionResult> results = new ArrayList<>();

        // Construir indexMap y grid en un solo pass
        for (int i = 0; i < objects.size(); i++) {
            GameObjects obj = objects.get(i);

            // Guard: objetos destruidos no participan
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null) continue;

            indexMap.put(obj, i);

            Rectangle b = col.getBounds();
            int x0 = Math.floorDiv(b.x, CELL_SIZE);
            int y0 = Math.floorDiv(b.y, CELL_SIZE);
            int x1 = Math.floorDiv(b.x + b.width,  CELL_SIZE);
            int y1 = Math.floorDiv(b.y + b.height, CELL_SIZE);

            for (int cx = x0; cx <= x1; cx++) {
                for (int cy = y0; cy <= y1; cy++) {
                    long cellKey = ((long) cx << 32) | (cy & 0xFFFFFFFFL);
                    grid.computeIfAbsent(cellKey, k -> new ArrayList<>(4)).add(obj);
                }
            }
        }

        // Comparar pares dentro de cada celda
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

                    // Guard: si alguno está muerto, omitir
                    if (b instanceof Destroyable d && d.isPendingDestruction()) continue;

                    // Evitar duplicados entre celdas (mismo par en dos celdas)
                    int idxA = indexMap.getOrDefault(a, -1);
                    int idxB = indexMap.getOrDefault(b, -1);
                    if (idxA < 0 || idxB < 0) continue;

                    long pairKey = idxA < idxB
                            ? ((long) idxA << 32) | idxB
                            : ((long) idxB << 32) | idxA;
                    if (!checked.add(pairKey)) continue;

                    // Verificar que no fue despachado ya en FASE 1/1B
                    int idHashA = System.identityHashCode(a);
                    int idHashB = System.identityHashCode(b);
                    long skipKey = idHashA < idHashB
                            ? ((long) idHashA << 32) | (idHashB & 0xFFFFFFFFL)
                            : ((long) idHashB << 32) | (idHashA & 0xFFFFFFFFL);
                    if (skipSet.contains(skipKey)) continue;

                    // Comprobación AABB
                    Rectangle ra = colA.getBounds();
                    Rectangle rb = colB.getBounds();
                    if (!ra.intersects(rb)) continue;

                    boolean isTrigger = colA.isTrigger() || colB.isTrigger();

                    // Calcular normal aproximada desde geometría relativa
                    int[] normal = computeOverlapNormal(ra, rb);

                    results.add(new CollisionResult(a, b, isTrigger, normal[0], normal[1]));
                }
            }
        }

        // Limpiar skipSet para el siguiente frame
        skipSet.clear();

        return results;
    }

    /**
     * Limpia el estado del detector. Llamar al cambiar de mundo/escena.
     */
    public void clear() {
        grid.clear();
        indexMap.clear();
        checked.clear();
        skipSet.clear();
    }

    // ── Normal desde overlap ──────────────────────────────────────────────

    /**
     * Calcula la normal aproximada de un overlap AABB estático.
     *
     * La normal se calcula por el eje de menor penetración:
     *   - Si la penetración en X es menor que en Y → cara lateral → normalX != 0
     *   - Si la penetración en Y es menor que en X → cara vertical → normalY != 0
     *
     * La dirección se determina por qué centro está a la izquierda/arriba del otro.
     *
     * ── Garantía de normal no-nula ────────────────────────────────────────
     * Si las penetraciones calculadas son ambas cero o negativas (puede ocurrir
     * por el truncado (int) en ColliderComponent.getBounds() cuando los objetos
     * se tocan exactamente en un borde), se usa el vector centro-a-centro como
     * fallback geométrico. Esto garantiza que un overlap detectado por AABB
     * produzca siempre una normal válida — nunca (0,0) — para que los behaviors
     * como BulletJump no caigan al fallback heurístico por velocidad.
     *
     * @param ra bounds del objeto A
     * @param rb bounds del objeto B
     * @return int[2] con {normalX, normalY} desde la perspectiva de A hacia B
     */
    private static int[] computeOverlapNormal(Rectangle ra, Rectangle rb) {
        // Centros
        double cxA = ra.x + ra.width  * 0.5;
        double cyA = ra.y + ra.height * 0.5;
        double cxB = rb.x + rb.width  * 0.5;
        double cyB = rb.y + rb.height * 0.5;

        double dx = cxA - cxB;
        double dy = cyA - cyB;

        // Penetraciones en cada eje
        double penX = (ra.width  + rb.width)  * 0.5 - Math.abs(dx);
        double penY = (ra.height + rb.height) * 0.5 - Math.abs(dy);

        if (penX <= 0 || penY <= 0) {
            // Overlap mínimo en un eje — los objetos se tocan exactamente en un borde.
            // Esto ocurre por el truncado (int) en getBounds() cuando la posición
            // double está en el límite exacto. Usar el vector centro-a-centro para
            // producir una normal geométricamente coherente en lugar de (0,0).
            // Un (0,0) forzaría a BulletJump al fallback heurístico de velocidad,
            // que es menos preciso que esta información espacial.
            if (dx == 0 && dy == 0) {
                // Centros exactamente superpuestos — caso degenerado extremo.
                // Usar eje X como fallback arbitrario pero documentado.
                return new int[]{1, 0};
            }
            // Normal apunta desde B hacia A en el eje de mayor separación
            return Math.abs(dx) >= Math.abs(dy)
                    ? new int[]{ dx > 0 ? 1 : -1, 0 }
                    : new int[]{ 0, dy > 0 ? 1 : -1 };
        }

        // El eje de menor penetración determina la normal
        if (penX < penY) {
            // Cara lateral impactada
            return new int[]{ dx > 0 ? 1 : -1, 0 };
        } else {
            // Cara vertical impactada
            return new int[]{ 0, dy > 0 ? 1 : -1 };
        }
    }
}
