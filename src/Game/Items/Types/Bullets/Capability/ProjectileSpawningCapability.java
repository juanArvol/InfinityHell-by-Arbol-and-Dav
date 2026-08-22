package Game.Items.Types.Bullets.Capability;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.ProjectileBlueprint;

/**
 * Capacidad de spawning de proyectiles secundarios.
 * 
 * ── QUÉ REPRESENTA ────────────────────────────────────────────────────────
 * 
 * Esta capacidad permite que un proyectil genere otros proyectiles durante
 * su ciclo de vida (típicamente en onExpire o onCollision).
 * 
 * Casos de uso:
 *   - Explosiones que generan shrapnel
 *   - Proyectiles que se dividen al impactar
 *   - Patrones de balas que generan balas secundarias
 * 
 * ── SEPARACIÓN DE RESPONSABILIDADES ───────────────────────────────────────
 * 
 * BulletBehavior:
 *   - Decide CUÁNDO spawnear (onExpire, onCollision, onUpdate)
 *   - Decide QUÉ spawnear (blueprint, dirección, cantidad)
 *   - NO conoce WorldManager ni ProjectilePool
 * 
 * ProjectileSpawningCapability:
 *   - Proporciona el mecanismo de spawn
 *   - Respaldado por WorldManager + ProjectilePool
 *   - Abstrae los detalles de infraestructura
 * 
 * ── EJEMPLO DE USO ────────────────────────────────────────────────────────
 * 
 * En un BulletBehavior:
 * 
 *   @Override
 *   public void onExpire(Bullet bullet, ProjectileContext ctx) {
 *       ProjectileSpawningCapability spawning = 
 *           ctx.getCapability(ProjectileSpawningCapability.class);
 *       
 *       if (spawning != null) {
 *           for (int i = 0; i < 8; i++) {
 *               Vector2D dir = directionAt(i * 45);
 *               spawning.spawnProjectile(fragmentBlueprint, position, dir);
 *           }
 *       }
 *   }
 */
public interface ProjectileSpawningCapability {
    
    /**
     * Spawnea un proyectil secundario en el mundo.
     * 
     * El proyectil spawneado:
     *   - Pasa por el mismo lifecycle que cualquier proyectil normal
     *   - Puede reutilizar instancias del pool si el blueprint lo permite
     *   - Recibe su propio ProjectileContext resuelto según sus requirements
     *   - Se añade automáticamente al mundo
     * 
     * @param blueprint definición del proyectil a spawnear
     * @param position  posición de spawn en el mundo
     * @param direction dirección normalizada de vuelo
     */
    void spawnProjectile(ProjectileBlueprint blueprint,
                         Vector2D position,
                         Vector2D direction);
    
    /**
     * Spawnea un proyectil apuntando hacia un objetivo.
     * Método de conveniencia que calcula la dirección automáticamente.
     * 
     * @param blueprint definición del proyectil
     * @param origin    posición de spawn
     * @param target    posición objetivo (puede ser null para usar dirección default)
     */
    default void spawnProjectileToward(ProjectileBlueprint blueprint,
                                       Vector2D origin,
                                       Vector2D target) {
        Vector2D direction;
        if (target == null) {
            direction = new Vector2D(1, 0); // Default: derecha
        } else {
            double dx = target.getX() - origin.getX();
            double dy = target.getY() - origin.getY();
            double len = Math.hypot(dx, dy);
            direction = (len > 1e-6)
                    ? new Vector2D(dx / len, dy / len)
                    : new Vector2D(1, 0);
        }
        spawnProjectile(blueprint, origin, direction);
    }
}
