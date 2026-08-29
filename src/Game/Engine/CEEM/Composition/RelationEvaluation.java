package Game.Engine.CEEM.Composition;

import Game.Engine.CEEM.Identity.StressSourceID;

/**
 * Immutable result of evaluating a module relationship.
 * 
 * RelationEvaluation describes how two modules are currently interacting
 * from a stress perspective.
 * 
 * This is analogous to StressReport, but for relationships rather than
 * individual modules.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * Relations produce evaluations that inform CEEM's coordination logic
 * without embedding relationship-specific knowledge in CEEM itself.
 * 
 * PURPOSE:
 * RelationEvaluations are CONTEXT for coordination decisions, not direct
 * inputs to automatic optimization. They answer questions like:
 * 
 * - "Are projectiles and rendering competing for resources?"
 * - "Does optimizing module A help or hurt module B?"
 * - "Which modules should be optimized together vs separately?"
 * 
 * The automatic optimization cycle (evaluateAndOptimize) operates solely
 * on individual StressReports to maintain simplicity. RelationEvaluations
 * are available for external coordination logic when needed.
 * 
 * FUTURE EVOLUTION:
 * This class may be extended to support:
 * - Weighted influence factors
 * - Directional stress propagation
 * - Recommended optimization priorities
 * - Multi-module correlation data
 * - Integration into automatic coordination strategies
 */
public final class RelationEvaluation {
    
    private final StressSourceID primarySource;
    private final StressSourceID secondarySource;
    private final double influence;
    private final String diagnostic;
    
    /**
     * Constructs a relation evaluation.
     * 
     * @param primarySource the primary module in the relationship
     * @param secondarySource the secondary module in the relationship
     * @param influence normalized influence strength (0.0 = none, 1.0 = strong)
     * @param diagnostic human-readable description of the relationship state
     */
    public RelationEvaluation(
            StressSourceID primarySource,
            StressSourceID secondarySource,
            double influence,
            String diagnostic) {
        
        if (primarySource == null) {
            throw new IllegalArgumentException("Primary source cannot be null");
        }
        if (secondarySource == null) {
            throw new IllegalArgumentException("Secondary source cannot be null");
        }
        if (influence < 0.0) {
            throw new IllegalArgumentException("Influence cannot be negative");
        }
        
        this.primarySource = primarySource;
        this.secondarySource = secondarySource;
        this.influence = influence;
        this.diagnostic = diagnostic != null ? diagnostic : "";
    }
    
    /**
     * Returns the primary source in this relationship.
     * 
     * @return primary module identity
     */
    public StressSourceID primarySource() {
        return primarySource;
    }
    
    /**
     * Returns the secondary source in this relationship.
     * 
     * @return secondary module identity
     */
    public StressSourceID secondarySource() {
        return secondarySource;
    }
    
    /**
     * Returns the normalized influence strength of this relationship.
     * 
     * Influence interpretation:
     * - 0.0: no current influence
     * - 0.0 to 0.5: minor influence
     * - 0.5 to 1.0: significant influence
     * - 1.0+: critical influence
     * 
     * The exact mapping is relation-specific.
     * 
     * @return influence magnitude
     */
    public double influence() {
        return influence;
    }
    
    /**
     * Returns diagnostic information about the relationship.
     * 
     * Example: "1500 projectiles increasing render load by 30%"
     * 
     * @return diagnostic string (never null, may be empty)
     */
    public String diagnostic() {
        return diagnostic;
    }
    
    @Override
    public String toString() {
        return String.format(
            "RelationEvaluation[%s ↔ %s, influence=%.2f, %s]",
            primarySource.name(),
            secondarySource.name(),
            influence,
            diagnostic
        );
    }
}
