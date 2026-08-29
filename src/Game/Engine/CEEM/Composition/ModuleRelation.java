package Game.Engine.CEEM.Composition;

import Game.Engine.CEEM.Identity.StressSourceID;

/**
 * Represents a stress-relevant relationship between two engine modules.
 * 
 * ModuleRelation is the foundational abstraction for CEEM's compositional model.
 * It allows modules to express dependencies, correlations, and interactions
 * without requiring direct coupling between their implementations.
 * 
 * ARCHITECTURAL PRINCIPLE: COMPOSITION OVER CENTRALIZATION
 * 
 * Instead of making CEEM aware of specific module relationships like:
 * - "Projectiles affects Rendering"
 * - "Physics affects Particles"
 * - "Enemies affects AI"
 * 
 * We model relationships as independent, registerable components.
 * 
 * CONCEPTUAL MODEL:
 * 
 * <pre>
 *     Module A
 *        │
 *        │ provides context
 *        ▼
 *   ModuleRelation ◄──── evaluates relationship
 *        ▲
 *        │ consumes context
 *        │
 *     Module B
 * </pre>
 * 
 * The relation lives in the composition space between modules,
 * not within either module's domain.
 * 
 * EXAMPLE USE CASES:
 * 
 * 1. Projectile-Render Relation:
 *    "High projectile count increases render pressure"
 *    - primarySource: ProjectilesSourceID
 *    - secondarySource: RenderingSourceID
 *    - evaluation: checks if projectile count affects render load
 * 
 * 2. Enemy-Physics Relation:
 *    "Many active enemies increase physics simulation cost"
 *    - primarySource: EnemiesSourceID
 *    - secondarySource: PhysicsSourceID
 *    - evaluation: correlates enemy behavior with physics workload
 * 
 * 3. Particle-Render Relation:
 *    "Particle effects contribute to render complexity"
 *    - primarySource: ParticlesSourceID
 *    - secondarySource: RenderingSourceID
 *    - evaluation: measures particle rendering overhead
 * 
 * LIFECYCLE:
 * 
 * 1. Relation implementation is created (e.g., ProjectileRenderRelation)
 * 2. Relation is registered with CEEM
 * 3. CEEM evaluates relation when both modules are present
 * 4. Relation provides additional stress context for optimization decisions
 * 
 * DEPENDENCY INVERSION:
 * 
 * Relations depend on StressSourceID abstractions, not concrete modules.
 * This maintains the principle that CEEM never depends on module internals.
 * 
 * OPTIONAL PARTICIPATION:
 * 
 * A relation only exists if:
 * - Both modules exist
 * - Both modules participate in CEEM
 * - The relation implementation is registered
 * 
 * There is NO concept of "null relation" or "missing relation".
 * Absence is represented by simply not registering the relation.
 * 
 * FUTURE EXTENSIBILITY:
 * 
 * Relations can be:
 * - Directional: A affects B (but not vice versa)
 * - Bidirectional: A and B mutually affect each other
 * - Multi-way: A, B, and C interact (via multiple binary relations)
 * - Weighted: relation strength varies dynamically
 * - Conditional: relation only matters in certain states
 * 
 * The base abstraction supports all of these through different implementations.
 */
public interface ModuleRelation {
    
    /**
     * Returns the primary module in this relationship.
     * 
     * The "primary" designation is semantic and implementation-defined.
     * Common interpretations:
     * - The source of stress propagation
     * - The module being analyzed
     * - The dominant contributor
     * 
     * @return the primary module's stress source identity
     */
    StressSourceID primarySource();
    
    /**
     * Returns the secondary module in this relationship.
     * 
     * The "secondary" designation is semantic and implementation-defined.
     * Common interpretations:
     * - The affected module
     * - The receiving module
     * - The dependent module
     * 
     * @return the secondary module's stress source identity
     */
    StressSourceID secondarySource();
    
    /**
     * Evaluates the current state of this relationship.
     * 
     * This method is called by CEEM when performing relational stress analysis.
     * The implementation should examine the relationship between the modules
     * and return a RelationEvaluation describing the current interaction.
     * 
     * CONTEXT ACQUISITION:
     * The relation implementation is responsible for obtaining necessary
     * context from the modules. This might involve:
     * - Accessing module-specific metrics
     * - Querying shared state
     * - Examining recent stress reports through the provided context
     * 
     * PERFORMANCE CONSIDERATION:
     * Like StressContributor.evaluate(), this should be lightweight.
     * Relations are evaluated each frame when both modules are active.
     * 
     * INACTIVE RELATIONSHIPS:
     * If the relationship is temporarily inactive or cannot be evaluated
     * (e.g., insufficient context, one module in dormant state), return
     * a RelationEvaluation with influence = 0.0 rather than null.
     * 
     * This preserves the principle: "registered relation always returns evaluation".
     * Absence of influence is not absence of relation.
     * 
     * ARCHITECTURAL PRINCIPLE:
     * A registered relation always produces an evaluation.
     * If temporarily inactive: influence = 0.0
     * If permanently irrelevant: should not be registered
     * 
     * @param context the evaluation context provided by CEEM
     * @return evaluation result describing current relationship state (never null)
     */
    RelationEvaluation evaluate(RelationContext context);
    
    /**
     * Returns a human-readable description of this relationship.
     * 
     * Used for diagnostics, logging, and debugging.
     * 
     * Example: "Projectile count affects render workload"
     * 
     * @return relationship description
     */
    String description();
}
