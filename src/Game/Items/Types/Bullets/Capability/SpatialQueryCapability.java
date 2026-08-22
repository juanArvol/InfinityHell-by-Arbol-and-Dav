package Game.Items.Types.Bullets.Capability;

import Game.Engine.AbstractEntity;
import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.List;

/**
 * Capacidad de búsqueda espacial de entidades.
 * 
 * ── QUÉ REPRESENTA ────────────────────────────────────────────────────────
 * 
 * Esta capacidad permite que un proyectil busque entidades cercanas en el
 * mundo, típicamente para: explosiones en área, búsqueda de objetivos,
 * efectos de proximidad, mecánicas de homing.
 * 
 * Casos de uso:
 *   - Explosiones que dañan entidades en radio (MetheorBullet)
 *   - Proyectiles que buscan el objetivo más cercano
 *   - Efectos de área (slow, poison, etc.) aplicados en radio
 * 
 * ── SEPARACIÓN DE RESPONSABILIDADES ───────────────────────────────────────
 * 
 * BulletBehavior:
 *   - Decide QUÉ buscar (centro, radio)
 *   - Decide QUÉ HACER con las entidades encontradas (daño, empuje, etc.)
 *   - NO conoce WorldManager, SpatialQuery, registries
 * 
 * SpatialQueryCapability:
 *   - Proporciona la búsqueda espacial
 *   - Respaldado por SpatialQuery del Engine
 *   - Abstrae la estrategia de búsqueda (linear, quadtree, hash, etc.)
 * 
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 * 
 * En MetheorBullet:
 * 
 *   private void explode(Bullet bullet) {
 *       SpatialQueryCapability spatial = 
 *           bullet.getProjectileContext()
 *                 .getCapability(SpatialQueryCapability.class);
 *       
 *       if (spatial == null) {
 *           throw new IllegalStateException("MetheorBullet requires SpatialQuery");
 *       }
 *       
 *       List<? extends AbstractEntity> entities = 
 *           spatial.findEntitiesInRadius(center, explosionRadius);
 *       
 *       for (AbstractEntity entity : entities) {
 *           entity.gotDamage(calculateDamage(distance));
 *       }
 *   }
 */
public interface SpatialQueryCapability {
    
    /**
     * Busca entidades AbstractEntity vivas dentro de un radio dado.
     * 
     * La búsqueda:
     *   - Usa la infraestructura espacial del Engine (SpatialQuery)
     *   - Filtra automáticamente entidades muertas (isDead() == false)
     *   - Retorna lista inmutable (defensiva)
     * 
     * La estrategia de búsqueda (linear O(n), quadtree O(log n), spatial hash)
     * es transparente para el consumidor — es responsabilidad del provider.
     * 
     * @param center posición central de búsqueda
     * @param radius radio de búsqueda en unidades del mundo
     * @return lista inmutable de entidades encontradas (puede estar vacía, nunca null)
     */
    List<? extends AbstractEntity> findEntitiesInRadius(Vector2D center, double radius);
}
