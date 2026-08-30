package Game.Items.Types.Bullets.CEEM;

import Game.Engine.CEEM.Identity.StressSourceID;
import Game.Engine.CEEM.Stress.*;

/**
 * CEEM stress contributor for the Projectiles module.
 * 
 * This contributor evaluates computational stress based on projectile-specific
 * metrics such as:
 * - Active projectile count
 * - Collision detection cost
 * - Simulation complexity
 * - Rendering cost
 * - Spatial density
 * 
 * IMPLEMENTATION NOTE:
 * This is a reference implementation demonstrating CEEM integration.
 * The actual stress calculation logic should be adapted to match
 * the real Projectiles system implementation.
 * 
 * RESPONSIBILITY:
 * Answers: "How stressed is the Projectiles module right now?"
 */
public final class ProjectileStressContributor implements StressContributor {
    
    // These will be injected with actual projectile system components
    private final ProjectileStressMetrics metrics;
    
    /**
     * Creates a projectile stress contributor.
     * 
     * @param metrics the metrics provider for stress calculation
     */
    public ProjectileStressContributor(ProjectileStressMetrics metrics) {
        if (metrics == null) {
            throw new IllegalArgumentException("Metrics cannot be null");
        }
        this.metrics = metrics;
    }
    
    @Override
    public StressSourceID source() {
        return ProjectilesSourceID.PROJECTILES;
    }
    
    @Override
    public StressReport evaluate(StressContext context) {
        // Gather current metrics
        int activeCount = metrics.activeProjectileCount();
        int collisionChecks = metrics.collisionCheckCount();
        int visibleCount = metrics.visibleProjectileCount();
        
        // Calculate stress magnitude based on multiple factors
        double countStress = calculateCountStress(activeCount);
        double collisionStress = calculateCollisionStress(collisionChecks);
        double densityStress = calculateDensityStress(activeCount, visibleCount);
        
        // Weighted combination
        double magnitude = (countStress * 0.4) + 
                          (collisionStress * 0.4) + 
                          (densityStress * 0.2);
        
        // Determine stress level
        StressLevel level = determineLevel(magnitude);
        
        // Build diagnostic information
        String diagnostic = buildDiagnostic(activeCount, collisionChecks, visibleCount);
        
        return new StressReport(source(), magnitude, level, diagnostic);
    }
    
    /**
     * Calculates stress contribution from projectile count.
     * 
     * This is contextual: the cost depends on what kind of projectiles exist,
     * not just how many.
     */
    private double calculateCountStress(int count) {
        // Reference thresholds (should be tuned based on actual performance)
        if (count < 100) return 0.0;
        if (count < 500) return 0.3;
        if (count < 1000) return 0.6;
        if (count < 2000) return 0.9;
        return 1.2; // Exceeding nominal capacity
    }
    
    /**
     * Calculates stress from collision detection workload.
     */
    private double calculateCollisionStress(int checks) {
        // Collision checks are often more expensive than raw count
        if (checks < 200) return 0.0;
        if (checks < 1000) return 0.4;
        if (checks < 5000) return 0.8;
        return 1.5;
    }
    
    /**
     * Calculates stress from spatial density.
     * 
     * High density (many projectiles in view) is more expensive
     * than scattered projectiles.
     */
    private double calculateDensityStress(int total, int visible) {
        if (total == 0) return 0.0;
        
        double visibilityRatio = (double) visible / total;
        
        // High density in viewport is expensive for rendering and collision
        if (visibilityRatio > 0.8 && visible > 500) {
            return 0.8;
        } else if (visibilityRatio > 0.5 && visible > 300) {
            return 0.5;
        }
        
        return 0.2;
    }
    
    /**
     * Maps magnitude to discrete stress level.
     */
    private StressLevel determineLevel(double magnitude) {
        if (magnitude < 0.3) return StressLevel.NOMINAL;
        if (magnitude < 0.6) return StressLevel.MODERATE;
        if (magnitude < 1.0) return StressLevel.HIGH;
        if (magnitude < 1.5) return StressLevel.CRITICAL;
        return StressLevel.EMERGENCY;
    }
    
    /**
     * Builds human-readable diagnostic information.
     */
    private String buildDiagnostic(int active, int collisionChecks, int visible) {
        return String.format(
            "Active: %d | Collision checks: %d | Visible: %d",
            active,
            collisionChecks,
            visible
        );
    }
}
