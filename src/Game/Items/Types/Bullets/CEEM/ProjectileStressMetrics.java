package Game.Items.Types.Bullets.CEEM;

/**
 * Interface for projectile stress metrics.
 * 
 * This abstraction decouples the stress contributor from the actual
 * projectile system implementation, allowing the contributor to remain
 * stable while the projectile internals evolve.
 * 
 * IMPLEMENTATION:
 * The actual Projectiles module should implement this interface,
 * providing real-time metrics from its internal state.
 * 
 * DESIGN PRINCIPLE:
 * Metrics are pull-based: CEEM asks, module answers.
 * No push notifications or callbacks.
 */
public interface ProjectileStressMetrics {
    
    /**
     * Returns the current count of active projectiles.
     * 
     * "Active" means participating in simulation and updates.
     * This excludes pooled/inactive projectiles.
     * 
     * @return active projectile count
     */
    int activeProjectileCount();
    
    /**
     * Returns the number of collision checks performed last frame.
     * 
     * This is a proxy for collision detection workload.
     * 
     * @return collision check count
     */
    int collisionCheckCount();
    
    /**
     * Returns the count of projectiles currently visible in viewport.
     * 
     * Visible projectiles have higher rendering and update costs.
     * 
     * @return visible projectile count
     */
    int visibleProjectileCount();
}
