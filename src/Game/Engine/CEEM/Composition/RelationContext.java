package Game.Engine.CEEM.Composition;

import Game.Engine.CEEM.Identity.StressSourceID;
import Game.Engine.CEEM.Stress.StressReport;

/**
 * Contextual information provided to relations during evaluation.
 * 
 * RelationContext gives relations access to the information they need
 * to evaluate module interactions without requiring direct module coupling.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * Relations receive context from CEEM, not direct module references.
 * This maintains clean dependency boundaries.
 * 
 * DESIGN NOTE:
 * This interface is intentionally minimal at introduction.
 * It can be extended as relational evaluation needs evolve.
 */
public interface RelationContext {
    
    /**
     * Returns the current frame number.
     * 
     * Useful for time-series analysis and temporal correlation.
     * 
     * @return current frame count
     */
    long frameNumber();
    
    /**
     * Returns the most recent stress report for a given source.
     * 
     * This allows relations to examine the stress state of their
     * participating modules without requiring direct module access.
     * 
     * USAGE PATTERN:
     * A ProjectileRenderRelation might check:
     * - projectile stress magnitude
     * - render stress level
     * And correlate them to determine if the relationship is significant.
     * 
     * @param source the module to query
     * @return the most recent stress report, or null if not available
     */
    StressReport getStressReport(StressSourceID source);
    
    /**
     * Returns the frame's delta time in seconds.
     * 
     * @return delta time since last frame
     */
    double deltaTime();
}
