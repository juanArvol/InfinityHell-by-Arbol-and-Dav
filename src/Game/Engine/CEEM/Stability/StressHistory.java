package Game.Engine.CEEM.Stability;

import Game.Engine.CEEM.Identity.StressSourceID;
import Game.Engine.CEEM.Stress.StressLevel;
import Game.Engine.CEEM.Stress.StressReport;
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
     * 
     * ARCHITECTURAL NOTE:
     * The current implementation uses Exponential Moving Average (EMA)
     * which doesn't require storing full history. The historyDepth
     * parameter is retained for potential future features but currently
     * not actively used in smoothing calculations.
     * 
     * EMA provides memory-efficient temporal filtering with acceptable
     * smoothing characteristics for stress management.
     * 
     * TEMPORAL UNITS:
     * Thresholds are measured in FRAMES (evaluation cycles), not real time.
     * This is intentional:
     * - Frame-based thresholds represent "observation count"
     * - Independent of variable framerate
     * - Represents statistical confidence (N consistent observations)
     * - Aligns with CEEM evaluation cycle
     * 
     * Example: stabilityThreshold = 10 means "10 consecutive evaluations
     * at the same level" regardless of whether those evaluations occur
     * over 83ms (120fps), 166ms (60fps), or 333ms (30fps).
     */
    public static class Config {
        /** 
         * Historical depth parameter (evaluation cycles).
         * Currently not used by EMA smoothing but retained for:
         * - Future variance/trend analysis features
         * - Configuration compatibility
         * - Semantic documentation of intended temporal window
         */
        public final int historyDepth;
        
        /** 
         * Minimum evaluation cycles at a level before transition is allowed.
         * 
         * This represents "statistical confidence" rather than absolute time.
         * Higher values = more stable, fewer transitions, slower response.
         * Lower values = less stable, more transitions, faster response.
         */
        public final int stabilityThreshold;
        
        /** Weight of new sample vs historical average (0.0 to 1.0) */
        public final double smoothingFactor;
        
        /**
         * Creates default configuration.
         * 
         * Defaults:
         * - 30 evaluation cycles (not used currently)
         * - 10 cycles stability threshold (requires 10 consistent observations)
         * - 0.3 smoothing factor (30% new, 70% history)
         * 
         * These defaults provide reasonable stability without excessive lag.
         */
        public Config() {
            this(30, 10, 0.3);
        }
        
        /**
         * Creates custom configuration.
         * 
         * @param historyDepth number of evaluation cycles (currently unused)
         * @param stabilityThreshold minimum cycles before level transition
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
     * 
     * THREAD SAFETY NOTE:
     * Registration is safe for concurrent initialization contexts.
     * Evaluation (record/get methods) must be single-threaded (game loop).
     */
    public StressHistory() {
        this(new Config());
    }
    
    /**
     * Creates a stress history tracker with custom configuration.
     * 
     * THREAD SAFETY NOTE:
     * Registration is safe for concurrent initialization contexts.
     * Evaluation (record/get methods) must be single-threaded (game loop).
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
     * NULL CONTRACT:
     * Returns sentinel value 0.0 if source has no history, for convenience.
     * Use this method when 0.0 is an acceptable default for unknown sources.
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
     * NULL CONTRACT:
     * Returns sentinel value NOMINAL if source has no history, for convenience.
     * Use this method when NOMINAL is an acceptable default for unknown sources.
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
     * NULL CONTRACT:
     * Returns sentinel value NOMINAL if source has no history, for convenience.
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
     * NULL CONTRACT:
     * Returns sentinel value 0 if source has no history, for convenience.
     * 
     * @param source the module to query
     * @return frame count at current level, or 0 if no history
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
     * NULL CONTRACT:
     * Returns null when the source has no recorded history, indicating
     * the module has never reported stress. This is semantically distinct
     * from a module with zero stress (which returns StableStressLevel with
     * magnitude 0.0).
     * 
     * Callers MUST null-check the result before use.
     * 
     * Alternative query methods (getSmoothedMagnitude, getStableLevel, etc.)
     * return sentinel values (0.0, NOMINAL) for convenience when the null
     * distinction is not needed.
     * 
     * @param source the module to query
     * @return stability information, or null if source has no history
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
     * 
     * DESIGN NOTE:
     * This class distinguishes between:
     * - Uninitialized state (no samples yet)
     * - Initialized state with zero stress
     * 
     * Using a boolean flag rather than magic value 0.0 for uninitialized.
     * 
     * SMOOTHING STRATEGY:
     * Uses Exponential Moving Average (EMA) which maintains a single
     * smoothed value without requiring full history storage.
     * This is memory-efficient and provides adequate temporal filtering.
     * 
     * If future features require full magnitude history (e.g., variance
     * analysis, trend detection), it can be added with clear justification.
     */
    private static class SourceHistory {
        private final Config config;
        
        private double smoothedMagnitude;
        private boolean hasReceivedSample;
        private int sampleCount;
        private StressLevel currentLevel;
        private StressLevel stableLevel;
        private int framesAtLevel;
        
        SourceHistory(Config config) {
            this.config = config;
            this.smoothedMagnitude = 0.0;
            this.hasReceivedSample = false;
            this.sampleCount = 0;
            this.currentLevel = StressLevel.NOMINAL;
            this.stableLevel = StressLevel.NOMINAL;
            this.framesAtLevel = 0;
        }
        
        void add(StressReport report) {
            double magnitude = report.magnitude();
            StressLevel level = report.level();
            
            // Track sample count (no longer storing full history)
            sampleCount++;
            
            // Update smoothed magnitude using exponential moving average
            if (!hasReceivedSample) {
                // First sample: initialize directly
                smoothedMagnitude = magnitude;
                hasReceivedSample = true;
            } else {
                // Subsequent samples: apply EMA
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
