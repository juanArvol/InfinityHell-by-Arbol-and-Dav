package Game.Items.Types.Bullets.CEEM;

/**
 * Control interface for projectile optimization actions.
 * 
 * This abstraction decouples the optimization policy from the actual
 * projectile system implementation.
 * 
 * IMPLEMENTATION:
 * The actual Projectiles module should implement this interface,
 * exposing controls that the policy can activate.
 * 
 * DESIGN PRINCIPLE:
 * Controls are command-style: policy requests actions,
 * module implementation executes them.
 * 
 * ARCHITECTURAL NOTE:
 * This is where policy meets implementation. The policy knows
 * WHAT to optimize and WHEN, but the controls define HOW.
 */
public interface ProjectileOptimizationControls {
    
    /**
     * Enables aggressive offscreen culling.
     * 
     * Projectiles outside the visible region are culled more quickly.
     */
    void enableOffscreenCulling();
    
    /**
     * Disables offscreen culling, returning to normal retention behavior.
     */
    void disableOffscreenCulling();
    
    /**
     * Sets the retention time multiplier for offscreen projectiles.
     * 
     * This contextually adjusts how long projectiles persist when offscreen.
     * Values less than 1.0 reduce retention, greater than 1.0 increase it.
     * 
     * DESIGN NOTE:
     * This implements the HRFC principle that retention should be contextual,
     * not based on magic numbers.
     * 
     * @param multiplier retention factor (0.0 to 1.0+)
     */
    void setRetentionMultiplier(double multiplier);
    
    /**
     * Simplifies collision detection for reduced CPU cost.
     * 
     * Examples: broader phase only, reduced precision, spatial culling
     */
    void simplifyCollision();
    
    /**
     * Restores normal collision detection precision.
     */
    void restoreNormalCollision();
    
    /**
     * Enables trail/particle effects on projectiles.
     */
    void enableTrailEffects();
    
    /**
     * Reduces trail effect quality (fewer particles, simpler trails).
     */
    void reduceTrailQuality();
    
    /**
     * Completely disables trail effects.
     */
    void disableTrailEffects();
    
    /**
     * Enables cosmetic effects (sparks, glows, etc.).
     */
    void enableCosmeticEffects();
    
    /**
     * Reduces cosmetic effect quality.
     */
    void reduceCosmeticEffects();
    
    /**
     * Disables all cosmetic effects.
     */
    void disableCosmeticEffects();
    
    /**
     * Sets the simulation rate multiplier.
     * 
     * Values less than 1.0 reduce simulation frequency.
     * For example, 0.5 means simulate every other frame.
     * 
     * @param rate simulation rate factor (0.0 to 1.0)
     */
    void setSimulationRate(double rate);
}
