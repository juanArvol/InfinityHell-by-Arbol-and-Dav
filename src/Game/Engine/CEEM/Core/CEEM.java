package Game.Engine.CEEM.Core;

import Game.Engine.CEEM.Composition.ModuleRelation;
import Game.Engine.CEEM.Composition.RelationEvaluation;
import Game.Engine.CEEM.Composition.RelationRegistry;
import Game.Engine.CEEM.Identity.StressSourceID;
import Game.Engine.CEEM.Optimization.OptimizationPolicy;
import Game.Engine.CEEM.Stability.StableStressLevel;
import Game.Engine.CEEM.Stability.StressHistory;
import Game.Engine.CEEM.Stress.StressContext;
import Game.Engine.CEEM.Stress.StressContributor;
import Game.Engine.CEEM.Stress.StressLevel;
import Game.Engine.CEEM.Stress.StressReport;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contextual Engine Stress & Optimization Management.
 * 
 * CEEM coordinates stress evaluation and optimization response across engine modules.
 * 
 * CORE RESPONSIBILITIES:
 * 1. Register modules as stress contributors
 * 2. Evaluate stress state by asking each contributor
 * 3. Coordinate optimization policies based on stress with temporal stability
 * 4. Manage inter-module relationships through composition
 * 5. Provide stress information for analysis
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * ARCHITECTURAL PRINCIPLES
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * PRINCIPLE 1: CEEM contains NO module-specific logic.
 * ────────────────────────────────────────────────────
 * - CEEM never checks projectile count, render calls, enemy count, etc.
 * - All such knowledge lives in the module itself.
 * - Module metrics belong to the module's domain.
 * 
 * PRINCIPLE 2: Identity is typed, never string-based.
 * ──────────────────────────────────────────────────
 * - No "projectiles" or "rendering" string comparisons.
 * - StressSourceID provides compile-time safe identity.
 * - String name() exists only for diagnostics/logging.
 * 
 * PRINCIPLE 3: Modules own their optimization strategies.
 * ──────────────────────────────────────────────────────
 * - CEEM coordinates WHEN to optimize.
 * - OptimizationPolicy defines WHAT to optimize.
 * - Optimization logic stays in the module's domain.
 * 
 * PRINCIPLE 4: Dynamic registration.
 * ─────────────────────────────────
 * - Modules register when they exist.
 * - Absence of a module means it doesn't participate yet.
 * - No null checks for "missing" modules.
 * - Adding a module = register contributor + register policy (optional).
 * 
 * PRINCIPLE 5: Composition over centralization.
 * ────────────────────────────────────────────
 * - Module relationships are expressed through ModuleRelation abstractions.
 * - Relations are registered independently, not hardcoded in CEEM.
 * - Adding a relation doesn't modify CEEM core or existing relations.
 * - Relations only evaluate when both modules are active.
 * 
 * PRINCIPLE 6: Temporal stability prevents oscillation.
 * ────────────────────────────────────────────────────
 * - StressHistory tracks temporal trends per module.
 * - Optimization decisions use stable levels, not raw measurements.
 * - Hysteresis prevents rapid activation/deactivation cycles.
 * - MODERATE band acts as neutral zone between states.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * ARCHITECTURAL MODEL
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * CURRENT STATE:
 * 
 * <pre>
 *                         ┌─────────────────────┐
 *                         │        CEEM         │
 *                         │   (Coordination)    │
 *                         └──────────┬──────────┘
 *                                    │
 *                 ┌──────────────────┼──────────────────┐
 *                 │                  │                  │
 *                 ▼                  ▼                  ▼
 *          ┌──────────┐       ┌──────────┐      ┌──────────┐
 *          │Contributor│       │ History  │      │Relations │
 *          │ Registry │       │Stability │      │ Registry │
 *          └─────┬────┘       └────┬─────┘      └────┬─────┘
 *                │                 │                   │
 *                ▼                 ▼                   ▼
 *           Projectiles      Temporal Filter    (future relations)
 * </pre>
 * 
 * FUTURE EVOLUTION:
 * 
 * <pre>
 *                         ┌─────────────────────┐
 *                         │        CEEM         │
 *                         │   (Coordination)    │
 *                         └──────────┬──────────┘
 *                                    │
 *                 ┌──────────────────┼──────────────────┐
 *                 │                  │                  │
 *                 ▼                  ▼                  ▼
 *          ┌──────────┐       ┌──────────┐      ┌──────────┐
 *          │Contributors      │ History  │      │Relations │
 *          │                  │          │      │          │
 *          ├──────────┤       └────┬─────┘      ├──────────┤
 *          │Projectiles│            │            │P ↔ Render│
 *          │ Render   │            ▼            │P ↔ Physics
 *          │ Physics  │       Stable Levels     │R ↔ Particles
 *          │ Enemies  │                         │E ↔ AI    │
 *          │Particles │                         └──────────┘
 *          └──────────┘
 * </pre>
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * DEPENDENCY DIRECTION
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * ALLOWED:
 * Module → StressContributor → CEEM
 * Module → OptimizationPolicy → CEEM
 * Module A + Module B → ModuleRelation → CEEM
 * 
 * FORBIDDEN:
 * CEEM → Module internals
 * CEEM → ProjectileManager
 * CEEM → RenderPipeline
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * LIFECYCLE & USAGE
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * INITIALIZATION:
 * <pre>
 * CEEM ceem = new CEEM();
 * 
 * // Module registers itself
 * ceem.registerContributor(projectileContributor);
 * ceem.registerPolicy(ProjectilesSourceID.PROJECTILES, projectilePolicy);
 * 
 * // Future: relations can be registered
 * // ceem.registerRelation(projectileRenderRelation);
 * </pre>
 * 
 * FRAME LOOP:
 * <pre>
 * ceem.updateTiming(deltaTime);
 * 
 * // Option 1: Manual control
 * StressEvaluation eval = ceem.evaluate();
 * // Analyze eval, make decisions
 * 
 * // Option 2: Automatic optimization with stability
 * StressEvaluation eval = ceem.evaluateAndOptimize();
 * // Policies are applied/restored based on stable stress levels
 * </pre>
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * EXTENSIBILITY MODEL
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * Adding a new module to CEEM:
 * 
 * 1. Define typed identity:
 *    <pre>
 *    public enum RenderingSourceID implements StressSourceID {
 *        RENDERING;
 *        public String name() { return "rendering"; }
 *    }
 *    </pre>
 * 
 * 2. Implement StressContributor:
 *    <pre>
 *    class RenderStressContributor implements StressContributor {
 *        public StressSourceID source() { return RenderingSourceID.RENDERING; }
 *        public StressReport evaluate(StressContext ctx) { ... }
 *    }
 *    </pre>
 * 
 * 3. Implement OptimizationPolicy (optional):
 *    <pre>
 *    class RenderOptimizationPolicy implements OptimizationPolicy { ... }
 *    </pre>
 * 
 * 4. Register with CEEM:
 *    <pre>
 *    ceem.registerContributor(renderContributor);
 *    ceem.registerPolicy(RenderingSourceID.RENDERING, renderPolicy);
 *    </pre>
 * 
 * 5. (Future) Define relations:
 *    <pre>
 *    class ProjectileRenderRelation implements ModuleRelation {
 *        public StressSourceID primarySource() { return ProjectilesSourceID.PROJECTILES; }
 *        public StressSourceID secondarySource() { return RenderingSourceID.RENDERING; }
 *        public RelationEvaluation evaluate(RelationContext ctx) { ... }
 *    }
 *    ceem.registerRelation(new ProjectileRenderRelation());
 *    </pre>
 * 
 * CRITICAL: No modifications to CEEM.java are required.
 * 
 * ═══════════════════════════════════════════════════════════════════════════════
 * THREAD SAFETY
 * ═══════════════════════════════════════════════════════════════════════════════
 * 
 * This implementation uses concurrent collections to allow registration
 * from different initialization contexts. Evaluation is single-threaded.
 */
public final class CEEM { //Calculator of Especific Estres for Module
    
    private final Map<StressSourceID, StressContributor> contributors;
    private final Map<StressSourceID, OptimizationPolicy> policies;
    private final RelationRegistry relationRegistry;
    private final StressHistory stressHistory;
    
    private long frameCounter;
    private double lastDeltaTime;
    
    /**
     * Creates a new CEEM instance.
     */
    public CEEM() {
        this.contributors = new ConcurrentHashMap<>();
        this.policies = new ConcurrentHashMap<>();
        this.relationRegistry = new RelationRegistry();
        this.stressHistory = new StressHistory();
        this.frameCounter = 0;
        this.lastDeltaTime = 0.0;
    }
    
    /**
     * Creates a new CEEM instance with custom history configuration.
     * 
     * @param historyConfig stress history configuration
     */
    public CEEM(StressHistory.Config historyConfig) {
        this.contributors = new ConcurrentHashMap<>();
        this.policies = new ConcurrentHashMap<>();
        this.relationRegistry = new RelationRegistry();
        this.stressHistory = new StressHistory(historyConfig);
        this.frameCounter = 0;
        this.lastDeltaTime = 0.0;
    }
    
    /**
     * Registers a module as a stress contributor.
     * 
     * Once registered, the contributor will be evaluated during
     * each stress evaluation cycle.
     * 
     * IDEMPOTENCY:
     * Registering the same source multiple times replaces the previous contributor.
     * 
     * @param contributor the module's stress contributor
     * @throws IllegalArgumentException if contributor is null
     */
    public void registerContributor(StressContributor contributor) {
        if (contributor == null) {
            throw new IllegalArgumentException("Contributor cannot be null");
        }
        
        StressSourceID source = contributor.source();
        if (source == null) {
            throw new IllegalArgumentException("Contributor source cannot be null");
        }
        
        contributors.put(source, contributor);
    }
    
    /**
     * Registers an optimization policy for a stress source.
     * 
     * The policy will be activated when the corresponding source
     * reports stress exceeding configured thresholds.
     * 
     * NOTE: A contributor can be registered without a policy.
     * This allows monitoring stress without automatic optimization.
     * 
     * @param source the stress source this policy applies to
     * @param policy the optimization policy
     * @throws IllegalArgumentException if source or policy is null
     */
    public void registerPolicy(StressSourceID source, OptimizationPolicy policy) {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (policy == null) {
            throw new IllegalArgumentException("Policy cannot be null");
        }
        
        policies.put(source, policy);
    }
    
    /**
     * Unregisters a stress contributor.
     * 
     * The module will no longer be evaluated for stress.
     * This is useful for modules that are dynamically enabled/disabled.
     * 
     * @param source the source to unregister
     */
    public void unregisterContributor(StressSourceID source) {
        if (source != null) {
            contributors.remove(source);
        }
    }
    
    /**
     * Unregisters an optimization policy.
     * 
     * @param source the source whose policy should be removed
     */
    public void unregisterPolicy(StressSourceID source) {
        if (source != null) {
            policies.remove(source);
        }
    }
    
    /**
     * Updates frame timing information.
     * 
     * This should be called each frame before evaluate() to ensure
     * contributors receive current timing context.
     * 
     * @param deltaTime time elapsed since last frame in seconds
     */
    public void updateTiming(double deltaTime) {
        this.lastDeltaTime = deltaTime;
        this.frameCounter++;
    }
    
    /**
     * Evaluates stress across all registered contributors.
     * 
     * This method:
     * 1. Creates a StressContext with current frame information
     * 2. Asks each contributor to evaluate its stress
     * 3. Collects all StressReports
     * 4. Records reports in stress history for temporal stability
     * 5. Updates relation registry with active modules and reports
     * 6. Returns a StressEvaluation snapshot
     * 
     * PERFORMANCE:
     * Evaluation is currently synchronous and sequential.
     * Future optimization could parallelize contributor evaluation.
     * 
     * ERROR HANDLING:
     * If a contributor throws during evaluation, it is caught and logged,
     * but other contributors continue to be evaluated.
     * 
     * @return complete stress evaluation
     */
    public StressEvaluation evaluate() {
        StressContext context = new CEEMContext(lastDeltaTime, frameCounter);
        
        List<StressReport> reports = new ArrayList<>(contributors.size());
        
        for (StressContributor contributor : contributors.values()) {
            try {
                StressReport report = contributor.evaluate(context);
                if (report != null) {
                    reports.add(report);
                    // Record in history for temporal stability
                    stressHistory.record(report);
                }
            } catch (Exception e) {
                // Log but don't fail entire evaluation
                System.err.println("Error evaluating contributor " + 
                    contributor.source().name() + ": " + e.getMessage());
            }
        }
        
        // Update relation registry with current state
        Set<StressSourceID> activeSources = new HashSet<>();
        for (StressReport report : reports) {
            activeSources.add(report.source());
        }
        relationRegistry.updateActiveModules(activeSources);
        relationRegistry.updateStressReports(reports);
        
        return new StressEvaluation(reports, frameCounter);
    }
    
    /**
     * Evaluates stress and applies optimization policies with temporal stability.
     * 
     * This method combines evaluation with intelligent policy application that
     * avoids rapid oscillation through:
     * 
     * 1. HYSTERESIS: Different thresholds for activation vs deactivation
     *    - Activation: HIGH or above (stable)
     *    - Deactivation: NOMINAL or below (stable)
     *    - MODERATE is a "neutral zone" - no state changes
     * 
     * 2. TEMPORAL STABILITY: Uses stable levels from history
     *    - Policies only activate when stress has persisted
     *    - Transient spikes don't trigger optimization
     *    - Prevents frame-to-frame toggling
     * 
     * OPTIMIZATION LIFECYCLE:
     * 
     * <pre>
     * NOMINAL → MODERATE → HIGH (stable) → [OPTIMIZE]
     *                              ↓
     *                          pressure persists
     *                              ↓
     *                         policy active
     *                              ↓
     *                       MODERATE → NOMINAL (stable) → [RESTORE]
     * </pre>
     * 
     * The MODERATE band acts as a buffer zone where policies remain
     * in their current state without changes.
     * 
     * @return the stress evaluation
     */
    public StressEvaluation evaluateAndOptimize() {
        StressEvaluation evaluation = evaluate();
        
        for (StressReport report : evaluation.reports()) {
            OptimizationPolicy policy = policies.get(report.source());
            
            if (policy != null) {
                // Get stable stress information from history
                StableStressLevel stable = stressHistory.getStableInfo(report.source());
                
                if (stable != null && stable.isStable()) {
                    // Use stable level for optimization decisions
                    StressLevel level = stable.stableLevel();
                    
                    // HYSTERESIS LOGIC:
                    // Apply optimization when stable stress is HIGH or above
                    // Restore when stable stress is NOMINAL
                    // MODERATE is neutral - maintain current state
                    
                    switch (level) {
                        case HIGH, CRITICAL, EMERGENCY -> {
                            // Activate optimization if not already active
                            if (!policy.isActive()) {
                                policy.apply(report);
                            }
                        }
                        case NOMINAL -> {
                            // Restore to normal operation if currently optimized
                            if (policy.isActive()) {
                                policy.restore();
                            }
                        }
                        case MODERATE -> {
                        }
                    }
                    // Neutral zone - maintain current policy state
                    // This prevents oscillation between HIGH and MODERATE
                                    } else {
                    // No stable history yet, use raw level with conservative approach
                    // Only activate on CRITICAL or above to avoid premature optimization
                    switch (report.level()) {
                        case CRITICAL, EMERGENCY -> {
                            if (!policy.isActive()) {
                                policy.apply(report);
                            }
                        }
                        case NOMINAL -> {
                            if (policy.isActive()) {
                                policy.restore();
                            }
                        }
                            
                        default -> {
                        }
                    }
                    // For HIGH and MODERATE without history, wait for stability
                                    }
            }
        }
        
        return evaluation;
    }
    
    /**
     * Returns the number of registered contributors.
     * 
     * @return contributor count
     */
    public int contributorCount() {
        return contributors.size();
    }
    
    /**
     * Returns the current frame number.
     * 
     * @return frame count
     */
    public long frameNumber() {
        return frameCounter;
    }
    
    /**
     * Checks if a specific source is registered.
     * 
     * @param source the source to check
     * @return true if registered
     */
    public boolean isRegistered(StressSourceID source) {
        return contributors.containsKey(source);
    }
    
    /**
     * Registers a module relationship.
     * 
     * Relations express stress-relevant interactions between modules.
     * They are evaluated when both participating modules are active.
     * 
     * ARCHITECTURAL PRINCIPLE:
     * Relations are additive. Adding a new relation does not require
     * modifying CEEM core or existing relations.
     * 
     * @param relation the module relationship to register
     * @throws IllegalArgumentException if relation is null
     */
    public void registerRelation(ModuleRelation relation) {
        relationRegistry.register(relation);
    }
    
    /**
     * Unregisters a module relationship.
     * 
     * @param primarySource the primary source of the relation
     * @param secondarySource the secondary source of the relation
     */
    public void unregisterRelation(StressSourceID primarySource, StressSourceID secondarySource) {
        relationRegistry.unregister(primarySource, secondarySource);
    }
    
    /**
     * Returns all registered relations.
     * 
     * @return unmodifiable collection of relations
     */
    public Collection<ModuleRelation> getRelations() {
        return relationRegistry.getRelations();
    }
    
    /**
     * Returns relations involving a specific module.
     * 
     * @param source the module to query
     * @return relations where source participates
     */
    public Collection<ModuleRelation> getRelationsFor(StressSourceID source) {
        return relationRegistry.getRelationsFor(source);
    }
    
    /**
     * Evaluates all applicable module relations.
     * 
     * Relations are evaluated after stress contributors have been evaluated
     * and provide additional context about module interactions.
     * 
     * This method is typically called after evaluate() to obtain
     * relational stress information.
     * 
     * @return collection of relation evaluations
     */
    public Collection<RelationEvaluation> evaluateRelations() {
        return relationRegistry.evaluateRelations(frameCounter, lastDeltaTime);
    }
    
    /**
     * Returns stability information for a specific stress source.
     * 
     * This provides access to temporal filtering and persistence data
     * that informs optimization decisions.
     * 
     * @param source the module to query
     * @return stable stress level information, or null if no history
     */
    public StableStressLevel getStabilityInfo(StressSourceID source) {
        return stressHistory.getStableInfo(source);
    }
    
    /**
     * Returns the smoothed stress magnitude for a source.
     * 
     * This is a temporally filtered value useful for gradual UI feedback
     * or progressive optimization strategies.
     * 
     * @param source the module to query
     * @return smoothed magnitude (0.0 if no history)
     */
    public double getSmoothedMagnitude(StressSourceID source) {
        return stressHistory.getSmoothedMagnitude(source);
    }
    
    /**
     * Clears stress history for a specific source.
     * 
     * Useful when a module is reset or reinitialized and historical
     * data is no longer relevant.
     * 
     * @param source the module to clear
     */
    public void clearHistory(StressSourceID source) {
        stressHistory.clear(source);
    }
}
