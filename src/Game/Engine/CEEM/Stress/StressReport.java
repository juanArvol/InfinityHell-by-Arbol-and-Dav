package Game.Engine.CEEM.Stress;

import Game.Engine.CEEM.Identity.StressSourceID;

/**
 * Immutable report of a module's computational stress state.
 * 
 * StressReport encapsulates the result of a contributor's self-evaluation.
 * It represents the answer to: "How stressed are you?"
 * 
 * This is a domain object that flows from contributor to CEEM,
 * carrying diagnostic and measurement information.
 * 
 * ARCHITECTURAL PRINCIPLES:
 * - Immutable: constructed once, never modified
 * - Source-identified: always knows which module produced it
 * - Self-contained: carries its own interpretation (level + magnitude)
 * - Diagnostic-enabled: provides contextual information for analysis
 * 
 * DESIGN NOTE:
 * The report contains both magnitude (0.0 to 1.0+) and level (NOMINAL to EMERGENCY).
 * This dual representation allows both quantitative and qualitative reasoning.
 */
public final class StressReport {
    
    private final StressSourceID source;
    private final double magnitude;
    private final StressLevel level;
    private final String diagnostic;
    
    /**
     * Constructs a complete stress report.
     * 
     * @param source the module that generated this report
     * @param magnitude normalized stress intensity (0.0 = none, 1.0 = nominal limit)
     * @param level qualitative categorization of stress
     * @param diagnostic human-readable context about the stress source
     */
    public StressReport(
            StressSourceID source,
            double magnitude,
            StressLevel level,
            String diagnostic) {
        
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (magnitude < 0.0) {
            throw new IllegalArgumentException("Magnitude cannot be negative");
        }
        if (level == null) {
            throw new IllegalArgumentException("Level cannot be null");
        }
        
        this.source = source;
        this.magnitude = magnitude;
        this.level = level;
        this.diagnostic = diagnostic != null ? diagnostic : "";
    }
    
    /**
     * Returns the source module that produced this report.
     * 
     * @return the stress source identifier
     */
    public StressSourceID source() {
        return source;
    }
    
    /**
     * Returns the normalized stress magnitude.
     * 
     * Magnitude interpretation:
     * - 0.0 to 0.5: light load
     * - 0.5 to 1.0: approaching capacity
     * - 1.0+: exceeding nominal capacity
     * 
     * The exact mapping is module-specific.
     * 
     * @return stress magnitude
     */
    public double magnitude() {
        return magnitude;
    }
    
    /**
     * Returns the qualitative stress level.
     * 
     * @return stress categorization
     */
    public StressLevel level() {
        return level;
    }
    
    /**
     * Returns diagnostic information about the stress.
     * 
     * This is human-readable context for debugging and analysis.
     * Examples: "2000 active projectiles", "150 draw calls", "collision grid saturated"
     * 
     * @return diagnostic string (never null, may be empty)
     */
    public String diagnostic() {
        return diagnostic;
    }
    
    @Override
    public String toString() {
        return String.format(
            "StressReport[source=%s, magnitude=%.2f, level=%s, diagnostic=%s]",
            source.name(),
            magnitude,
            level,
            diagnostic
        );
    }
}
