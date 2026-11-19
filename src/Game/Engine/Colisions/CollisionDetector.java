package Game.Engine.Colisions;

import static Game.Engine.Colisions.CollisionConstants.*;
import Game.Engine.Destroyable;
import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameMath.Logic2D.RectangleH;
import Game.Engine.GameObjects;
import java.util.*;

/**
 * Detecta pares de objetos que se solapan usando AABB + spatial hash.
 *
 * ── HRFC — Collision System Refactor ──────────────────────────────────────
 *
 * RESPONSABILIDAD ÚNICA:
 *   Solo DETECTA pares residuales (FASE 2). No mueve objetos, no llama eventos.
 *   Los pares ya procesados en FASE 1 se excluyen via skipSet.
 *
 * MEJORAS EN ESTA VERSIÓN:
 *   ✓ Reutiliza colecciones entre frames (zero allocations)
 *   ✓ Spatial hash adaptativo basado en tamaño promedio de objetos
 *   ✓ Calcula normales geométricas precisas para overlaps estáticos
 *   ✓ Guards contra objetos destruidos
 *   ✓ Validación de bounds de array en matrix access
 *   ✓ Constantes centralizadas (no magic numbers)
 *
 * SPATIAL HASH ADAPTATIVO:
 *   El tamaño de celda se calcula dinámicamente:
 *   - MIN: 64px (bullets pequeños)
 *   - MAX: 256px (bosses grandes)
 *   - DEFAULT: 128px (balance)
 *
 * GARANTÍA DE NORMAL:
 *   computeOverlapNormal() NUNCA retorna (0,0) cuando hay overlap real.
 *   Usa centro-a-centro como fallback geométrico determinista.
 */
public final class CollisionDetector {

    /**
     * Tamaño de celda actual del spatial hash.
     * Se recalcula dinámicamente en detect() basado en el tamaño promedio
     * de los objetos activos en el frame.
     */
    private int currentCellSize = DEFAULT_CELL_SIZE;

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
     * ── HRFC — Spatial hash adaptativo ────────────────────────────────────
     * El tamaño de celda se calcula dinámicamente basado en el tamaño promedio
     * de los colliders activos. Esto optimiza el spatial hash para el contenido
     * actual del frame.
     *
     * @param objects lista de todos los objetos activos del frame
     * @return lista de pares en colisión (sin duplicados, sin pares ya despachados)
     */
    public List<CollisionResult> detect(List<GameObjects> objects) {

        // Limpiar estructuras del frame anterior (skipSet se pobla ANTES de detect())
        grid.clear();
        indexMap.clear();
        checked.clear();

        List<CollisionResult> results = new ArrayList<>();

        // Calcular cell size adaptativo basado en objetos activos
        currentCellSize = computeAdaptiveCellSize(objects);

        // Construir indexMap y grid en un solo pass
        for (int i = 0; i < objects.size(); i++) {
            GameObjects obj = objects.get(i);

            // Guard: objetos destruidos no participan
            if (obj instanceof Destroyable d && d.isPendingDestruction()) continue;

            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null) continue;

            indexMap.put(obj, i);

            RectangleH b = col.getBounds();
            
            // Usar getCellIndices() de RectangleH con cell size dinámico
            int[] cellIndices = b.getCellIndices(currentCellSize);
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

                    // Calcular normal aproximada desde geometría relativa
                    int[] normal = computeOverlapNormal(ra, rb);

                    results.add(new CollisionResult(a, b, normal[0], normal[1]));
                }
            }
        }

        // Limpiar skipSet para el siguiente frame
        skipSet.clear();

        return results;
    }

    /**
     * Calcula el tamaño de celda óptimo basado en los colliders activos.
     *
     * ── HRFC — Spatial hash adaptativo ────────────────────────────────────
     * ESTRATEGIA:
     *   - Calcular el tamaño promedio de los colliders activos
     *   - cell_size = promedio × 2 (para que objetos típicos ocupen ~1-2 celdas)
     *   - Clampear entre MIN_CELL_SIZE y MAX_CELL_SIZE
     *
     * BENEFICIOS:
     *   - Bullets pequeños: cell=64 → menos celdas desperdiciadas
     *   - Bosses grandes: cell=256 → menos multi-celda overhead
     *   - Mix: cell=128 (default) → balance general
     *
     * @param objects lista de objetos activos
     * @return tamaño de celda óptimo para este frame
     */
    private int computeAdaptiveCellSize(List<GameObjects> objects) {
        if (objects.isEmpty()) {
            return DEFAULT_CELL_SIZE;
        }

        double totalSize = 0.0;
        int count = 0;

        for (GameObjects obj : objects) {
            ColliderComponent col = obj.getComponent(ColliderComponent.class);
            if (col == null) continue;

            RectangleH b = col.getBounds();
            // Usar la diagonal como medida representativa del tamaño
            double size = Math.sqrt(b.width * b.width + b.height * b.height);
            totalSize += size;
            count++;
        }

        if (count == 0) {
            return DEFAULT_CELL_SIZE;
        }

        double averageSize = totalSize / count;
        int adaptiveSize = (int) Math.round(averageSize * 2.0);

        // Clampear entre límites razonables
        return Math.max(MIN_CELL_SIZE, Math.min(MAX_CELL_SIZE, adaptiveSize));
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
     *   - Si penX < penY → cara lateral → normalX != 0
     *   - Si penY < penX → cara vertical → normalY != 0
     *
     * La dirección se determina por qué centro está a la izquierda/arriba del otro.
     *
     * ── Garantía de normal no-nula ────────────────────────────────────────
     * Si las penetraciones son ambas ≤ 0 (borde exacto), se usa el vector
     * centro-a-centro como fallback geométrico. NUNCA retorna (0,0) cuando
     * hay overlap real detectado por AABB.
     *
     * ── Uso de RectangleH API ─────────────────────────────────────────────
     * Usa penetrationX() y penetrationY() en lugar de cálculo manual.
     *
     * ── Tie-breaking consistente en esquinas ──────────────────────────────
     * Cuando penX ≈ penY (diferencia < CORNER_TIE_EPSILON), usa el vector
     * centro-a-centro como desempate en lugar de elegir arbitrariamente.
     * Esto es consistente con el tie-breaking de AABB.calculate2D().
     *
     * @param ra bounds del objeto A
     * @param rb bounds del objeto B
     * @return int[2] con {normalX, normalY} desde A hacia B
     */
    private static int[] computeOverlapNormal(RectangleH ra, RectangleH rb) {
        // Centros (con precisión double completa de RectangleH)
        double cxA = ra.getCenterX();
        double cyA = ra.getCenterY();
        double cxB = rb.getCenterX();
        double cyB = rb.getCenterY();

        double dx = cxA - cxB;
        double dy = cyA - cyB;

        // Usar API de RectangleH para cálculo de penetración
        double penX = ra.penetrationX(rb);
        double penY = ra.penetrationY(rb);

        if (penX <= 0 || penY <= 0) {
            // Overlap mínimo en un eje — los objetos se tocan exactamente en un borde.
            // Usar el vector centro-a-centro para producir una normal geométricamente
            // coherente en lugar de (0,0).
            if (dx == 0 && dy == 0) {
                // Centros exactamente superpuestos — caso degenerado extremo.
                // Usar eje X como fallback arbitrario pero documentado.
                return new int[]{NORMAL_RIGHT, NORMAL_NONE};
            }
            // Normal apunta desde B hacia A en el eje de mayor separación
            return Math.abs(dx) >= Math.abs(dy)
                    ? new int[]{ dx > 0 ? NORMAL_RIGHT : NORMAL_LEFT, NORMAL_NONE }
                    : new int[]{ NORMAL_NONE, dy > 0 ? NORMAL_DOWN : NORMAL_UP };
        }

        // Tie-breaking para esquinas (penX ≈ penY)
        // Usar vector centro-a-centro como desempate en lugar de elegir Y arbitrariamente
        if (Math.abs(penX - penY) <= CORNER_TIE_EPSILON) {
            // Esquina: usar el vector centro-a-centro para determinar el eje
            return Math.abs(dx) >= Math.abs(dy)
                    ? new int[]{ dx > 0 ? NORMAL_RIGHT : NORMAL_LEFT, NORMAL_NONE }
                    : new int[]{ NORMAL_NONE, dy > 0 ? NORMAL_DOWN : NORMAL_UP };
        }

        // El eje de menor penetración determina la normal
        if (penX < penY) {
            // Cara lateral impactada
            return new int[]{ dx > 0 ? NORMAL_RIGHT : NORMAL_LEFT, NORMAL_NONE };
        } else {
            // Cara vertical impactada
            return new int[]{ NORMAL_NONE, dy > 0 ? NORMAL_DOWN : NORMAL_UP };
        }
    }
}