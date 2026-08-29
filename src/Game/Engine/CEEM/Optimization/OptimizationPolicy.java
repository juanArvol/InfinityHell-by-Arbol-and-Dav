package Game.Engine.CEEM.Optimization;

import Game.Engine.CEEM.Stress.StressReport;

/**
 * Defines module-specific optimization strategies activated by stress.
 * 
 * OptimizationPolicy represents the answer to: "What can you do to reduce stress?"
 * 
 * Each module defines its own optimization strategies. For example:
 * 
 * PROJECTILES might optimize by:
 * - Offscreen culling
 * - Simulation rate reduction
 * - Collision simplification
 * - Effect degradation
 * 
 * RENDERING might optimize by:
 * - Particle reduction
 * - Effect quality reduction
 * - LOD switching
 * - Draw call batching
 * 
 * PHYSICS might optimize by:
 * - Broadphase simplification
 * - Sleeping body expansion
 * - Constraint relaxation
 * 
 * ARCHITECTURAL PRINCIPLE:
 * CEEM coordinates when to apply optimizations, but never defines what they are.
 * The policy belongs to the module, not to CEEM.
 * 
 * LIFECYCLE:
 * 1. CEEM receives StressReport from contributor
 * 2. CEEM determines optimization is needed
 * 3. CEEM invokes policy.apply() with the stress context
 * 4. Policy executes module-specific optimization actions
 * 
 * DESIGN NOTE:
 * The policy receives the StressReport that triggered it, allowing
 * optimization to be proportional to stress magnitude and level.
 */
public interface OptimizationPolicy {
    
    /**
     * Applies optimization actions appropriate for the given stress level.
     * 
     * This method is called by CEEM when stress exceeds acceptable thresholds.
     * The implementation should:
     * 
     * 1. Interpret the stress report
     * 2. Select appropriate optimization strategies
     * 3. Apply them to the module's internal state
     * 4. (Optionally) track what optimizations were activated
     * 
     * IDEMPOTENCY CONSIDERATION:
     * This method may be called multiple consecutive frames if stress persists.
     * Implementations should handle repeated calls gracefully, either by:
     * - Making optimizations idempotent
     * - Tracking already-applied optimizations
     * - Using progressive degradation
     * 
     * PROPORTIONALITY:
     * Optimization intensity should scale with stress magnitude and level.
     * MODERATE stress might trigger light optimizations,
     * while EMERGENCY stress might trigger aggressive measures.
     * 
     * @param report the stress report that triggered optimization
     */
    void apply(StressReport report);
    
    /**
     * Removes previously applied optimizations, restoring nominal operation.
     * 
     * This method is called by CEEM when stress returns to acceptable levels.
     * The implementation should restore the module to its unoptimized state.
     * 
     * DESIGN NOTE:
     * This allows CEEM to implement hysteresis: apply optimizations at one
     * threshold, but only remove them when stress drops significantly below.
     * 
     * IMPLEMENTATION CONSIDERATION:
     * The policy may need to track which optimizations are active to
     * properly restore state.
     */
    void restore();
    
    /**
     * Returns whether optimizations are currently active.
     * 
     * IMPORTANT SEMANTIC BOUNDARY:
     * This method reports OPTIMIZATION state, NOT module state.
     * 
     * It answers: "Are optimization actions currently applied?"
     * It does NOT answer: "Is the module active/enabled/running?"
     * 
     * CEEM uses this to avoid redundant apply()/restore() calls:
     * - Don't call apply() if already optimizing
     * - Don't call restore() if already nominal
     * 
     * ARCHITECTURAL PRINCIPLE:
     * CEEM coordinates optimization timing but never manages module lifecycle.
     * The module itself controls whether it's active, enabled, or running.
     * This method MUST NOT conflate optimization state with module state.
     * 
     * CORRECT IMPLEMENTATION:
     * <pre>
     * private boolean optimizationsApplied = false;
     * 
     * public void apply(StressReport report) {
     *     enableCulling();
     *     reduceQuality();
     *     optimizationsApplied = true;
     * }
     * 
     * public boolean isActive() {
     *     return optimizationsApplied;  // State of optimizations, not module
     * }
     * </pre>
     * 
     * INCORRECT IMPLEMENTATION:
     * <pre>
     * public boolean isActive() {
     *     return module.isEnabled();  // WRONG: This is module state, not optimization state
     * }
     * </pre>
     * 
     * @return true if optimization actions are currently applied, false otherwise
     */
    boolean isActive();
}
