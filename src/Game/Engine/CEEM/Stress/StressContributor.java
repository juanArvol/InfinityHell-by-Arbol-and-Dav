package Game.Engine.CEEM.Stress;

import Game.Engine.CEEM.Identity.StressSourceID;

/**
 * Contract for modules that participate in CEEM stress evaluation.
 * 
 * StressContributor is the primary integration point between CEEM and
 * individual engine modules. Each module implements this interface to:
 * 
 * 1. Declare its identity (source)
 * 2. Evaluate its own stress state (evaluate)
 * 
 * ARCHITECTURAL PRINCIPLE:
 * The contributor knows how to measure its own stress.
 * CEEM never contains module-specific stress calculation logic.
 * 
 * RESPONSIBILITY:
 * A contributor answers the question: "How stressed are you right now?"
 * 
 * The evaluation logic is entirely module-specific. For example:
 * 
 * - Projectiles might consider: count, collision cost, simulation complexity
 * - Rendering might consider: draw calls, visible sprites, particles
 * - Physics might consider: active bodies, collision pairs, constraint count
 * 
 * CEEM simply asks all registered contributors and collects their reports.
 * 
 * LIFECYCLE:
 * 1. Module creates contributor implementation
 * 2. Module registers contributor with CEEM
 * 3. CEEM calls evaluate() each frame (or as needed)
 * 4. Contributor returns StressReport with current state
 */
public interface StressContributor {
    
    /**
     * Returns this contributor's stress source identifier.
     * 
     * This identity is used by CEEM to track and correlate stress reports.
     * The same StressSourceID instance should be returned consistently.
     * 
     * @return the module's stress source identity
     */
    StressSourceID source();
    
    /**
     * Evaluates the current stress state of this module.
     * 
     * This method is called by CEEM during stress evaluation cycles.
     * The contributor should:
     * 
     * 1. Examine its internal state
     * 2. Calculate stress magnitude based on module-specific metrics
     * 3. Determine appropriate stress level
     * 4. Provide diagnostic information
     * 5. Return a complete StressReport
     * 
     * NULL CONTRACT:
     * May return null to indicate "no stress report this frame". This is useful
     * when a module is temporarily inactive or unable to evaluate. CEEM will
     * silently skip null reports without failing the evaluation cycle.
     * 
     * Use null sparingly: prefer returning a report with magnitude 0.0 and
     * NOMINAL level when the module is active but unstressed.
     * 
     * PERFORMANCE CONSIDERATION:
     * This method should be lightweight. If stress calculation is expensive,
     * consider caching results or using incremental updates.
     * 
     * CONTEXT USAGE:
     * The provided context contains environmental information (frame time,
     * frame number, etc.) that may be useful for contextualizing measurements.
     * 
     * @param context environmental context for evaluation
     * @return a stress report representing current state, or null if no report
     */
    StressReport evaluate(StressContext context);
}
