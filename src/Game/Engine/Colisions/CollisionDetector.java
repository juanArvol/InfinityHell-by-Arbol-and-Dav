package Game.Engine.Colisions;

import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameObjects;
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
 *     - Usa RectangleH (double) en lugar de Rectangle (int) para precisión exacta
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
 * real — si la penetración es cero en un eje (borde exacto), usa el vector
 * centro-a-centro como fallback geométrico. Esto garantiza que los behaviors
 * como BulletJump siempre tengan una normal válida en FASE 2 y no necesiten
 * recurrir al fallback heurístico de velocidad.
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

            RectangleH b = col.getBounds();
            
            // HRFC: Usar getCellIndices() de RectangleH para evitar cálculo manual
            int[] cellIndices = b.getCellIndices(CELL_SIZE);
            int x0 = cellIndices[0];
            int y0 = cellIndices[1];
            int x1 = cellIndices[2];
            int y1 = cellIndices[3];

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

                    // Comprobación AABB con RectangleH (precisión double)
                    RectangleH ra = colA.getBounds();
                    RectangleH rb = colB.getBounds();
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
     * cuando los objetos se tocan exactamente en un borde), se usa el vector
     * centro-a-centro como fallback geométrico. Esto garantiza que un overlap
     * detectado por AABB produzca siempre una normal válida — nunca (0,0) —
     * para que los behaviors como BulletJump no caigan al fallback heurístico
     * por velocidad.
     *
     * ── HRFC — Uso de RectangleH API ──────────────────────────────────────
     * Actualizado para usar penetrationX() y penetrationY() en lugar de
     * cálculo manual. Esto elimina duplicación de lógica y usa la API probada.
     *
     * @param ra bounds del objeto A
     * @param rb bounds del objeto B
     * @return int[2] con {normalX, normalY} desde la perspectiva de A hacia B
     */
    private static int[] computeOverlapNormal(RectangleH ra, RectangleH rb) {
        // Centros (con precisión double completa de RectangleH)
        double cxA = ra.getCenterX();
        double cyA = ra.getCenterY();
        double cxB = rb.getCenterX();
        double cyB = rb.getCenterY();

        double dx = cxA - cxB;
        double dy = cyA - cyB;

        // HRFC: Usar API de RectangleH en lugar de cálculo manual
        double penX = ra.penetrationX(rb);
        double penY = ra.penetrationY(rb);

        if (penX <= 0 || penY <= 0) {
            // Overlap mínimo en un eje — los objetos se tocan exactamente en un borde.
            // Usar el vector centro-a-centro para producir una normal geométricamente
            // coherente en lugar de (0,0).
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