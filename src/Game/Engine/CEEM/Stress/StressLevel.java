package Game.Engine.CEEM.Stress;

/**
 * Discrete categorization of stress intensity.
 * 
 * StressLevel provides a qualitative interpretation of stress magnitude,
 * allowing systems to react to bands of pressure rather than precise thresholds.
 * 
 * The mapping from stress magnitude (a continuous value) to StressLevel
 * (discrete categories) is intentionally left to module-specific interpretation.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * StressLevel is descriptive, not prescriptive.
 * Each module determines what constitutes NOMINAL vs CRITICAL for its domain.
 */
public enum StressLevel {
    
    /**
     * Normal operational load.
     * No optimization required.
     */
    NOMINAL,
    
    /**
     * Elevated load, but within acceptable tolerances.
     * Monitoring recommended, optimization optional.
     */
    MODERATE,
    
    /**
     * Significant load approaching performance boundaries.
     * Optimization should be considered.
     */
    HIGH,
    
    /**
     * Critical load threatening performance targets.
     * Aggressive optimization required.
     */
    CRITICAL,
    
    /**
     * Extreme load causing observable degradation.
     * Emergency measures necessary.
     */
    EMERGENCY
}
