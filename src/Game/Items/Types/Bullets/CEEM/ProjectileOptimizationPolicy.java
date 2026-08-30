package Game.Items.Types.Bullets.CEEM;

import Game.Engine.CEEM.Optimization.OptimizationPolicy;
import Game.Engine.CEEM.Stress.StressLevel;
import Game.Engine.CEEM.Stress.StressReport;

/**
 * Optimization policy for the Projectiles module.
 * 
 * This policy defines HOW projectiles should be optimized when stress is detected.
 * CEEM coordinates WHEN to optimize; this policy defines WHAT to optimize.
 * 
 * OPTIMIZATION STRATEGIES:
 * 
 * MODERATE stress:
 * - Enable aggressive offscreen culling
 * - Reduce retention time for out-of-bounds projectiles
 * 
 * HIGH stress:
 * - Reduce collision precision
 * - Simplify trail effects
 * - Increase culling aggressiveness
 * 
 * CRITICAL stress:
 * - Disable cosmetic effects
 * - Reduce simulation rate
 * - Aggressive culling
 * 
 * EMERGENCY stress:
 * - Immediate culling of non-critical projectiles
 * - Minimal simulation
 * - All effects disabled
 * 
 * DESIGN PRINCIPLE:
 * Optimizations are proportional to stress level.
 * They degrade gracefully rather than binary on/off.
 */
public final class ProjectileOptimizationPolicy implements OptimizationPolicy {
    
    private final ProjectileOptimizationControls controls;
    
    private boolean isActive = false;
    private StressLevel currentLevel = StressLevel.NOMINAL;
    
    /**
     * Creates a projectile optimization policy.
     * 
     * @param controls the control interface for applying optimizations
     */
    public ProjectileOptimizationPolicy(ProjectileOptimizationControls controls) {
        if (controls == null) {
            throw new IllegalArgumentException("Controls cannot be null");
        }
        this.controls = controls;
    }
    
    @Override
    public void apply(StressReport report) {
        StressLevel level = report.level();
        
        // Only intensify if stress increased
        if (!isActive || level.ordinal() > currentLevel.ordinal()) {
            applyOptimizationsForLevel(level);
            currentLevel = level;
            isActive = true;
        }
    }
    
    @Override
    public void restore() {
        if (isActive) {
            controls.disableOffscreenCulling();
            controls.restoreNormalCollision();
            controls.enableTrailEffects();
            controls.enableCosmeticEffects();
            controls.setSimulationRate(1.0);
            controls.setRetentionMultiplier(1.0);
            
            isActive = false;
            currentLevel = StressLevel.NOMINAL;
        }
    }
    
    @Override
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Applies optimizations appropriate for the stress level.
     */
    private void applyOptimizationsForLevel(StressLevel level) {
        switch (level) {
            case MODERATE:
                applyModerateOptimizations();
                break;
                
            case HIGH:
                applyHighOptimizations();
                break;
                
            case CRITICAL:
                applyCriticalOptimizations();
                break;
                
            case EMERGENCY:
                applyEmergencyOptimizations();
                break;
                
            default:
                // NOMINAL - should not be called, but safe fallback
                restore();
                break;
        }
    }
    
    /**
     * Moderate stress: light optimizations.
     */
    private void applyModerateOptimizations() {
        controls.enableOffscreenCulling();
        controls.setRetentionMultiplier(0.8); // Slightly reduce retention
    }
    
    /**
     * High stress: significant optimizations.
     */
    private void applyHighOptimizations() {
        controls.enableOffscreenCulling();
        controls.setRetentionMultiplier(0.6);
        controls.simplifyCollision(); // Reduce collision precision
        controls.reduceTrailQuality(); // Simpler trail effects
    }
    
    /**
     * Critical stress: aggressive optimizations.
     */
    private void applyCriticalOptimizations() {
        controls.enableOffscreenCulling();
        controls.setRetentionMultiplier(0.4);
        controls.simplifyCollision();
        controls.disableTrailEffects(); // No trails
        controls.reduceCosmeticEffects(); // Minimal effects
        controls.setSimulationRate(0.8); // Slightly reduce simulation rate
    }
    
    /**
     * Emergency stress: maximum optimizations.
     */
    private void applyEmergencyOptimizations() {
        controls.enableOffscreenCulling();
        controls.setRetentionMultiplier(0.2); // Very aggressive culling
        controls.simplifyCollision();
        controls.disableTrailEffects();
        controls.disableCosmeticEffects(); // All effects off
        controls.setSimulationRate(0.6); // Reduced simulation
    }
}
