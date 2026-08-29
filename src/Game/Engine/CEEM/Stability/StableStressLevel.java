package Game.Engine.CEEM.Stability;

import Game.Engine.CEEM.Identity.StressSourceID;
import Game.Engine.CEEM.Stress.StressLevel;

/**
 * Wrapper combining stress level with temporal stability information.
 * 
 * StableStressLevel extends the basic StressLevel concept with:
 * - Current (raw) level
 * - Stable (filtered) level
 * - Persistence information
 * - Transition readiness
 * 
 * This allows optimization logic to distinguish between:
 * - Transient stress spikes (ignore or handle lightly)
 * - Sustained stress (apply optimization)
 * 
 * USAGE PATTERN:
 * 
 * <pre>
 * StableStressLevel stable = history.getStableInfo(source);
 * 
 * if (stable.isStable() && stable.stableLevel() == StressLevel.HIGH) {
 *     // High stress has persisted long enough to warrant optimization
 *     policy.apply(report);
 * }
 * </pre>
 * 
 * ARCHITECTURAL PRINCIPLE:
 * 
 * This is a value object that flows out of StressHistory.
 * It does not modify StressLevel enum but provides additional context.
 */
public final class StableStressLevel {
    
    private final StressSourceID source;
    private final StressLevel rawLevel;
    private final StressLevel stableLevel;
    private final int framesAtLevel;
    private final int stabilityThreshold;
    private final double smoothedMagnitude;
    
    /**
     * Creates a stable stress level wrapper.
     * 
     * @param source the module this applies to
     * @param rawLevel the current (unfiltered) stress level
     * @param stableLevel the stable (filtered) stress level
     * @param framesAtLevel consecutive frames at raw level
     * @param stabilityThreshold frames required for level to be considered stable
     * @param smoothedMagnitude temporally smoothed stress magnitude
     */
    public StableStressLevel(
            StressSourceID source,
            StressLevel rawLevel,
            StressLevel stableLevel,
            int framesAtLevel,
            int stabilityThreshold,
            double smoothedMagnitude) {
        
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (rawLevel == null) {
            throw new IllegalArgumentException("Raw level cannot be null");
        }
        if (stableLevel == null) {
            throw new IllegalArgumentException("Stable level cannot be null");
        }
        
        this.source = source;
        this.rawLevel = rawLevel;
        this.stableLevel = stableLevel;
        this.framesAtLevel = framesAtLevel;
        this.stabilityThreshold = stabilityThreshold;
        this.smoothedMagnitude = smoothedMagnitude;
    }
    
    /**
     * Returns the module this stability info applies to.
     * 
     * @return stress source identity
     */
    public StressSourceID source() {
        return source;
    }
    
    /**
     * Returns the current raw stress level.
     * 
     * This is the level from the most recent evaluation,
     * without temporal filtering.
     * 
     * @return raw stress level
     */
    public StressLevel rawLevel() {
        return rawLevel;
    }
    
    /**
     * Returns the stable stress level.
     * 
     * This level only changes after stress has persisted at a new
     * level for at least stabilityThreshold frames.
     * 
     * Use this for optimization decisions to avoid oscillation.
     * 
     * @return stable stress level
     */
    public StressLevel stableLevel() {
        return stableLevel;
    }
    
    /**
     * Returns the number of consecutive frames at current raw level.
     * 
     * @return frame count
     */
    public int framesAtLevel() {
        return framesAtLevel;
    }
    
    /**
     * Returns the threshold for level stability.
     * 
     * @return frames required for stability
     */
    public int stabilityThreshold() {
        return stabilityThreshold;
    }
    
    /**
     * Returns the temporally smoothed stress magnitude.
     * 
     * This is a filtered version of the raw magnitude that
     * changes gradually over time.
     * 
     * @return smoothed magnitude
     */
    public double smoothedMagnitude() {
        return smoothedMagnitude;
    }
    
    /**
     * Checks if the current level is stable.
     * 
     * A level is stable if it has persisted for at least
     * stabilityThreshold frames.
     * 
     * @return true if current level is stable
     */
    public boolean isStable() {
        return framesAtLevel >= stabilityThreshold;
    }
    
    /**
     * Checks if raw and stable levels match.
     * 
     * When these match, it indicates sustained stress at that level.
     * 
     * @return true if raw and stable levels are the same
     */
    public boolean isConverged() {
        return rawLevel == stableLevel;
    }
    
    /**
     * Checks if stress is transitioning upward.
     * 
     * @return true if raw level exceeds stable level
     */
    public boolean isEscalating() {
        return rawLevel.ordinal() > stableLevel.ordinal();
    }
    
    /**
     * Checks if stress is transitioning downward.
     * 
     * @return true if raw level is below stable level
     */
    public boolean isDeescalating() {
        return rawLevel.ordinal() < stableLevel.ordinal();
    }
    
    /**
     * Returns the progress toward stability as a ratio.
     * 
     * - 0.0: just transitioned to new level
     * - 1.0: reached stability threshold
     * - >1.0: stable for multiple threshold periods
     * 
     * @return stability progress ratio
     */
    public double stabilityProgress() {
        if (stabilityThreshold == 0) {
            return 1.0;
        }
        return (double) framesAtLevel / stabilityThreshold;
    }
    
    @Override
    public String toString() {
        return String.format(
            "StableStressLevel[source=%s, raw=%s, stable=%s, frames=%d/%d, smoothed=%.2f]",
            source.name(),
            rawLevel,
            stableLevel,
            framesAtLevel,
            stabilityThreshold,
            smoothedMagnitude
        );
    }
}
