package Game.Engine.CEEM.Stability;

import Game.Engine.CEEM.Identity.StressSourceID;
import Game.Engine.CEEM.Stress.StressLevel;
import Game.Engine.CEEM.Stress.StressReport;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks historical stress data to enable temporal stability in optimization decisions.
 * 
 * StressHistory prevents rapid oscillation between optimization states by
 * maintaining a rolling window of stress measurements and providing filtered values.
 * 
 * PROBLEM ADDRESSED:
 * 
 * Without history, stress evaluation can produce oscillating behavior:
 * <pre>
 * Frame 1: stress=1.2 → HIGH → optimize()
 * Frame 2: stress=0.8 → MODERATE → restore()
 * Frame 3: stress=1.1 → HIGH → optimize()
 * Frame 4: stress=0.9 → MODERATE → restore()
 * </pre>
 * 
 * This creates visual artifacts and performance instability.
 * 
 * SOLUTION:
 * 
 * StressHistory provides temporal smoothing through:
 * 1. Rolling window of recent measurements
 * 2. Smoothed magnitude (moving average)
 * 3. Stable level (requires persistence before transition)
 * 4. Hysteresis support (different thresholds for up/down transitions)
 * 
 * ARCHITECTURAL PRINCIPLE:
 * 
 * History is module-specific. Each stress source has its own history,
 * allowing per-module stability characteristics without coupling.
 * 
 * DESIGN PATTERN:
 * 
 * <pre>
 *   StressReport → StressHistory → StableStressLevel
 *       raw            filter         stabilized
 * </pre>
 * 
 * CONFIGURATION:
 * 
 * History depth and smoothing parameters can be tuned per deployment.
 * Deeper history = more stability, but slower response to real stress changes.
 */
public final class StressHistory {
    
    /**
     * Configuration for stress history behavior.
     */
    public static class Config {
        /** Number of frames to retain in history */
        public final int historyDepth;
        
        /** Minimum frames at a level before transition is allowed */
        public final int stabilityThreshold;
        
        /** Weight of new sample vs historical average (0.0 to 1.0) */
        public final double smoothingFactor;
        
        /**
         * Creates default configuration.
         * 
         * Defaults:
         * - 30 frame history (0.5s at 60fps)
         * - 10 frame stability threshold
         * - 0.3 smoothing factor (30% new, 70% history)
         */
        public Config() {
            this(30, 10, 0.3);
        }
        
        /**
         * Creates custom configuration.
         * 
         * @param historyDepth number of frames to retain
         * @param stabilityThreshold minimum frames before level transition
         * @param smoothingFactor weight of new samples (0.0 to 1.0)
         */
        public Config(int historyDepth, int stabilityThreshold, double smoothingFactor) {
            if (historyDepth < 1) {
                throw new IllegalArgumentException("History depth must be positive");
            }
            if (stabilityThreshold < 0) {
                throw new IllegalArgumentException("Stability threshold cannot be negative");
            }
            if (smoothingFactor < 0.0 || smoothingFactor > 1.0) {
                throw new IllegalArgumentException("Smoothing factor must be in [0.0, 1.0]");
            }
            
            this.historyDepth = historyDepth;
            this.stabilityThreshold = stabilityThreshold;
            this.smoothingFactor = smoothingFactor;
        }
    }
    
    private final Config config;
    private final Map<StressSourceID, SourceHistory> histories;
    
    /**
     * Creates a stress history tracker with default configuration.
     */
    public StressHistory() {
        this(new Config());
    }
    
    /**
     * Creates a stress history tracker with custom configuration.
     * 
     * @param config history behavior configuration
     */
    public StressHistory(Config config) {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        this.config = config;
        this.histories = new ConcurrentHashMap<>();
    }
    
    /**
     * Records a new stress report.
     * 
     * This should be called each frame after stress evaluation to
     * maintain historical context.
     * 
     * @param report the stress report to record
     */
    public void record(StressReport report) {
        if (report == null) {
            return;
        }
        
        SourceHistory history = histories.computeIfAbsent(
            report.source(),
            k -> new SourceHistory(config)
        );
        
        history.add(report);
    }
    
    /**
     * Returns the smoothed stress magnitude for a source.
     * 
     * This is a temporally filtered value that changes gradually
     * rather than jumping with each frame.
     * 
     * @param source the module to query
     * @return smoothed magnitude, or 0.0 if no history
     */
    public double getSmoothedMagnitude(StressSourceID source) {
        SourceHistory history = histories.get(source);
        return history != null ? history.smoothedMagnitude : 0.0;
    }
    
    /**
     * Returns the stable stress level for a source.
     * 
     * This level only changes when stress has persisted at a new level
     * for at least stabilityThreshold frames.
     * 
     * @param source the module to query
     * @return stable stress level, or NOMINAL if no history
     */
    public StressLevel getStableLevel(StressSourceID source) {
        SourceHistory history = histories.get(source);
        return history != null ? history.stableLevel : StressLevel.NOMINAL;
    }
    
    /**
     * Returns the raw (most recent) stress level for a source.
     * 
     * @param source the module to query
     * @return most recent level, or NOMINAL if no history
     */
    public StressLevel getRawLevel(StressSourceID source) {
        SourceHistory history = histories.get(source);
        return history != null ? history.currentLevel : StressLevel.NOMINAL;
    }
    
    /**
     * Returns the number of consecutive frames at current level.
     * 
     * Useful for understanding stability state.
     * 
     * @param source the module to query
     * @return frame count at current level
     */
    public int getFramesAtLevel(StressSourceID source) {
        SourceHistory history = histories.get(source);
        return history != null ? history.framesAtLevel : 0;
    }
    
    /**
     * Clears history for a specific source.
     * 
     * Useful when a module is reset or reinitialized.
     * 
     * @param source the module to clear
     */
    public void clear(StressSourceID source) {
        histories.remove(source);
    }
    
    /**
     * Clears all history.
     */
    public void clearAll() {
        histories.clear();
    }
    
    /**
     * Returns the number of sources being tracked.
     * 
     * @return tracked source count
     */
    public int trackedSourceCount() {
        return histories.size();
    }
    
    /**
     * Returns complete stability information for a source.
     * 
     * This provides a comprehensive view of the source's stability state,
     * combining raw level, stable level, persistence, and smoothed magnitude.
     * 
     * @param source the module to query
     * @return stability information, or null if no history
     */
    public StableStressLevel getStableInfo(StressSourceID source) {
        SourceHistory history = histories.get(source);
        if (history == null) {
            return null;
        }
        
        return new StableStressLevel(
            source,
            history.currentLevel,
            history.stableLevel,
            history.framesAtLevel,
            config.stabilityThreshold,
            history.smoothedMagnitude
        );
    }
    
    /**
     * Per-source history tracking.
     */
    private static class SourceHistory {
        private final Config config;
        private final LinkedList<Double> magnitudeHistory;
        
        private double smoothedMagnitude;
        private StressLevel currentLevel;
        private StressLevel stableLevel;
        private int framesAtLevel;
        
        SourceHistory(Config config) {
            this.config = config;
            this.magnitudeHistory = new LinkedList<>();
            this.smoothedMagnitude = 0.0;
            this.currentLevel = StressLevel.NOMINAL;
            this.stableLevel = StressLevel.NOMINAL;
            this.framesAtLevel = 0;
        }
        
        void add(StressReport report) {
            double magnitude = report.magnitude();
            StressLevel level = report.level();
            
            // Update magnitude history
            magnitudeHistory.addLast(magnitude);
            if (magnitudeHistory.size() > config.historyDepth) {
                magnitudeHistory.removeFirst();
            }
            
            // Update smoothed magnitude using exponential moving average
            if (smoothedMagnitude == 0.0) {
                smoothedMagnitude = magnitude;
            } else {
                smoothedMagnitude = config.smoothingFactor * magnitude + 
                                  (1.0 - config.smoothingFactor) * smoothedMagnitude;
            }
            
            // Track level persistence
            if (level == currentLevel) {
                framesAtLevel++;
            } else {
                currentLevel = level;
                framesAtLevel = 1;
            }
            
            // Update stable level only after sufficient persistence
            if (framesAtLevel >= config.stabilityThreshold) {
                stableLevel = currentLevel;
            }
        }
    }
}
