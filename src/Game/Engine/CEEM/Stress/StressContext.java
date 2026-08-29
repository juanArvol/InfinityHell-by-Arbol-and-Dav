package Game.Engine.CEEM.Stress;

/**
 * Contextual information provided to stress contributors during evaluation.
 * 
 * StressContext represents the information available to a module when
 * CEEM asks: "How stressed are you?"
 * 
 * This abstraction decouples contributors from CEEM internal implementation.
 * Contributors receive context, not direct CEEM references.
 * 
 * FUTURE EVOLUTION:
 * This interface may be extended to provide:
 * - Frame timing information
 * - Global engine statistics
 * - Computational budgets
 * - Simulation state
 * - Historical stress data
 * 
 * ARCHITECTURAL PRINCIPLE:
 * The context is read-only from the contributor's perspective.
 * It provides environmental information, never mutates CEEM state.
 */
public interface StressContext {
    
    /**
     * Returns the current frame's delta time in seconds.
     * 
     * This represents the actual time elapsed since the last frame,
     * which can be useful for contextualizing stress measurements.
     * 
     * @return delta time in seconds
     */
    double deltaTime();
    
    /**
     * Returns the current frame number.
     * 
     * Useful for time-series analysis and historical context.
     * 
     * @return current frame count
     */
    long frameNumber();
}
