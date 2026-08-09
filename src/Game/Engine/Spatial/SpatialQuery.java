package Game.Engine.Spatial;

import Game.Engine.GameObjects;
import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.List;

/**
 * Capacidad de consulta espacial del Engine.
 *
 * ── MOTIVACIÓN ────────────────────────────────────────────────────────────
 *
 * La lógica de búsqueda espacial ("qué entidades hay cerca de este punto")
 * estaba acoplada a proyectiles via ProjectileContext.findEntitiesInRadius().
 * Eso forzaba a que cualquier entidad que necesitara búsqueda espacial
 * dependiera de la infraestructura de proyectiles.
 *
 * SpatialQuery generaliza esta capacidad como servicio del Engine:
 *
 *   Bullet         → usa Engine.Spatial para buscar entidades cercanas
 *   (futuro) Enemy → usa Engine.Spatial para detectar al jugador
 *   (futuro) Trap  → usa Engine.Spatial para detectar entidades en zona
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 *
 *   SpatialQuery
 *       ↓
 *   consulta por posición + radio
 *       ↓
 *   GameObjects relevantes dentro del área
 *
 * La entidad que consulta solo declara "quiero saber qué hay en este radio".
 * SpatialQuery decide cómo implementar esa búsqueda internamente.
 *
 * ── PREPARACIÓN PARA ESTRUCTURAS AVANZADAS ────────────────────────────────
 *
 * La implementación actual (LinearSpatialQuery) hace un scan O(n) lineal
 * sobre la lista de entidades, igual que WorldProjectileContext antes.
 * No hay ningún cambio de comportamiento.
 *
 * La arquitectura queda preparada para que en el futuro se cambie la
 * implementación interna (QuadTree, SpatialHash, BVH) sin que ninguna
 * entidad consumidora (Bullet, Enemy, Trap) necesite modificarse.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 *
 * SpatialQuery es una interfaz en lugar de una clase concreta para que
 * el Engine no fuerce una única estrategia de búsqueda. Los distintos
 * contextos (mundo abierto, dungeon, boss arena) pueden tener implementaciones
 * con características distintas.
 */
public interface SpatialQuery {

    /**
     * Retorna todos los GameObjects dentro de un radio dado.
     *
     * La distancia se calcula desde el centro del objeto (posición del transform).
     * El radio es en unidades del mundo (píxeles).
     *
     * La lista retornada es una snapshot — no modificar.
     * Puede estar vacía si no hay objetos en el radio.
     *
     * @param center posición central de la consulta
     * @param radius radio de búsqueda en unidades del mundo
     * @return lista de GameObjects cuyo centro está dentro del radio
     */
    List<? extends GameObjects> findInRadius(Vector2D center, double radius);

    /**
     * Versión con filtro de tipo — retorna solo instancias del tipo dado.
     *
     * Equivalente a findInRadius + filter por instanceof, pero permite
     * implementaciones optimizadas que solo escaneen entidades del tipo solicitado.
     *
     * @param center posición central de la consulta
     * @param radius radio de búsqueda en unidades del mundo
     * @param type   clase del tipo que nos interesa
     * @param <T>    tipo de resultado
     * @return lista de instancias del tipo T dentro del radio
     */
    <T extends GameObjects> List<T> findInRadius(Vector2D center, double radius, Class<T> type);

    // ── Implementación nula ───────────────────────────────────────────────

    /**
     * SpatialQuery vacío — todas las consultas retornan lista vacía.
     *
     * Usar como placeholder cuando no hay mundo activo (tests, bootstrap inicial).
     */
    SpatialQuery NULL = new SpatialQuery() {
        @Override
        public List<? extends GameObjects> findInRadius(Vector2D center, double radius) {
            return List.of();
        }

        @Override
        public <T extends GameObjects> List<T> findInRadius(
                Vector2D center, double radius, Class<T> type) {
            return List.of();
        }
    };
}
