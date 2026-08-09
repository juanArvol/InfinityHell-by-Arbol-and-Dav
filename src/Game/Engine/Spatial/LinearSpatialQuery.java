package Game.Engine.Spatial;

import Game.Engine.GameObjects;
import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Implementación de SpatialQuery basada en scan lineal O(n).
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *
 * Itera sobre todas las entidades activas y retorna las que están dentro
 * del radio dado. Idéntico al comportamiento de
 * WorldProjectileContext.findEntitiesInRadius() antes de la refactorización.
 *
 * ── POR QUÉ O(n) ES CORRECTO AHORA ────────────────────────────────────────
 *
 * La búsqueda O(n) es suficiente para:
 *   - Efectos de área de proyectiles (explosiones, fragmentación).
 *   - AI de enemigos buscando al jugador en zona.
 *   - Trampas que detectan entidades cercanas.
 *
 * En un mundo donde n < 500 entidades dinámicas, el scan lineal es más
 * rápido que mantener una estructura espacial actualizada cada frame.
 *
 * ── PREPARACIÓN PARA OPTIMIZACIÓN ────────────────────────────────────────
 *
 * Cuando n crezca lo suficiente para que O(n) sea un problema medible,
 * se puede reemplazar LinearSpatialQuery por QuadTreeSpatialQuery o
 * SpatialHashQuery sin modificar ninguna entidad consumidora — solo
 * cambia la implementación inyectada en el contexto.
 *
 * ── FUENTE DE DATOS ───────────────────────────────────────────────────────
 *
 * Recibe la fuente de entidades como Supplier<List<GameObjects>> para no
 * acoplar la búsqueda espacial a DynamicEntityRegistry directamente.
 * Cualquier fuente de objetos (lista de chunk, registry global, etc.)
 * puede alimentar esta implementación.
 */
public final class LinearSpatialQuery implements SpatialQuery {

    /**
     * Proveedor de la lista de entidades activas.
     * Se consulta en cada llamada a findInRadius para obtener el estado actual.
     */
    private final Supplier<List<? extends GameObjects>> entitySource;

    /**
     * Construye una LinearSpatialQuery respaldada por la fuente dada.
     *
     * @param entitySource proveedor de la lista de entidades activas (no null)
     */
    public LinearSpatialQuery(Supplier<List<? extends GameObjects>> entitySource) {
        if (entitySource == null)
            throw new IllegalArgumentException("entitySource no puede ser null");
        this.entitySource = entitySource;
    }

    // ── SpatialQuery ──────────────────────────────────────────────────────

    /**
     * Scan lineal sobre todas las entidades activas.
     * Retorna las que están dentro del radio dado desde el centro.
     *
     * Distancia = distancia euclidiana entre centros (posiciones de transform).
     * Complejidad O(n) donde n = número de entidades activas.
     */
    @Override
    public List<? extends GameObjects> findInRadius(Vector2D center, double radius) {
        double radiusSq = radius * radius;
        List<GameObjects> result = new ArrayList<>();

        for (GameObjects obj : entitySource.get()) {
            Vector2D pos = obj.getTransform().getPosition();
            double dx = pos.getX() - center.getX();
            double dy = pos.getY() - center.getY();
            if ((dx * dx + dy * dy) <= radiusSq) {
                result.add(obj);
            }
        }
        return result;
    }

    /**
     * Scan lineal filtrado por tipo.
     * Retorna solo las instancias del tipo T dentro del radio.
     *
     * @param type clase del tipo que nos interesa
     */
    @Override
    public <T extends GameObjects> List<T> findInRadius(
            Vector2D center, double radius, Class<T> type) {

        double radiusSq = radius * radius;
        List<T> result = new ArrayList<>();

        for (GameObjects obj : entitySource.get()) {
            if (!type.isInstance(obj)) continue;
            Vector2D pos = obj.getTransform().getPosition();
            double dx = pos.getX() - center.getX();
            double dy = pos.getY() - center.getY();
            if ((dx * dx + dy * dy) <= radiusSq) {
                result.add(type.cast(obj));
            }
        }
        return result;
    }
}
