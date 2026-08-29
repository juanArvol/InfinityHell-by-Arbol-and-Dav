/**
 * Temporal stability mechanisms for CEEM stress evaluation.
 * 
 * <h2>Purpose</h2>
 * 
 * This package provides temporal filtering and hysteresis mechanisms to prevent
 * rapid oscillation in optimization decisions caused by momentary stress fluctuations.
 * 
 * <h2>Core Components</h2>
 * 
 * <ul>
 * <li>{@link Game.Engine.CEEM.Stability.StressHistory} - Tracks stress over time with smoothing</li>
 * <li>{@link Game.Engine.CEEM.Stability.StableStressLevel} - Combines raw and stable levels with persistence data</li>
 * </ul>
 * 
 * <h2>Problem: Optimization Oscillation</h2>
 * 
 * <p>
 * Without temporal stability, optimization can oscillate rapidly:
 * </p>
 * 
 * <pre>
 * Frame 100: stress=1.2 → HIGH → optimize() activated
 * Frame 101: stress=0.8 → MODERATE → restore() called  (optimization reduced stress)
 * Frame 102: stress=1.1 → HIGH → optimize() activated  (stress returns)
 * Frame 103: stress=0.9 → MODERATE → restore() called
 * Frame 104: stress=1.0 → HIGH → optimize() activated
 * ...
 * </pre>
 * 
 * <p>
 * This produces:
 * </p>
 * <ul>
 * <li>Visual artifacts (effects toggling on/off)</li>
 * <li>Performance instability</li>
 * <li>Poor user experience</li>
 * <li>Wasted CPU cycles</li>
 * </ul>
 * 
 * <h2>Solution: Temporal Filtering + Hysteresis</h2>
 * 
 * <h3>1. Temporal Filtering (StressHistory)</h3>
 * 
 * <p>
 * StressHistory maintains a rolling window of stress measurements and provides:
 * </p>
 * 
 * <ul>
 * <li><strong>Smoothed magnitude:</strong> Exponential moving average of raw values</li>
 * <li><strong>Stable level:</strong> Level that persists for minimum frame count</li>
 * <li><strong>Persistence tracking:</strong> How long current level has been maintained</li>
 * </ul>
 * 
 * <pre>
 * StressHistory history = new StressHistory();
 * 
 * // Each frame
 * history.record(stressReport);
 * 
 * // Get filtered values
 * double smoothed = history.getSmoothedMagnitude(source);
 * StressLevel stable = history.getStableLevel(source);
 * </pre>
 * 
 * <h3>2. Hysteresis (Different Thresholds)</h3>
 * 
 * <p>
 * CEEM uses different thresholds for activation vs deactivation:
 * </p>
 * 
 * <pre>
 *                NOMINAL  MODERATE    HIGH    CRITICAL  EMERGENCY
 * Magnitude:       0.0      0.5       1.0       1.5       2.0+
 *                   │        │         │         │         │
 *                   ▼        ▼         ▼         ▼         ▼
 * Activate:                          ┌──────────────────────┐
 *                                    │   Optimization ON    │
 *                                    └──────────────────────┘
 * 
 * Maintain:                 ┌────────────────────────────────┐
 *                           │    Keep Current State         │
 *                           └────────────────────────────────┘
 * 
 * Deactivate:    ┌─────────┐
 *                │   OFF   │
 *                └─────────┘
 * </pre>
 * 
 * <p>
 * MODERATE acts as a "neutral zone" - no state changes occur.
 * </p>
 * 
 * <h2>Configuration</h2>
 * 
 * <p>
 * StressHistory behavior is configurable:
 * </p>
 * 
 * <pre>
 * StressHistory.Config config = new StressHistory.Config(
 *     30,   // history depth (frames)
 *     10,   // stability threshold (frames)
 *     0.3   // smoothing factor (0.0-1.0)
 * );
 * 
 * CEEM ceem = new CEEM(config);
 * </pre>
 * 
 * <h3>Parameter Effects:</h3>
 * 
 * <table border="1" cellpadding="5">
 * <tr>
 *   <th>Parameter</th>
 *   <th>Higher Value</th>
 *   <th>Lower Value</th>
 * </tr>
 * <tr>
 *   <td>History Depth</td>
 *   <td>More stable, slower response</td>
 *   <td>Less stable, faster response</td>
 * </tr>
 * <tr>
 *   <td>Stability Threshold</td>
 *   <td>Changes less frequent</td>
 *   <td>Changes more frequent</td>
 * </tr>
 * <tr>
 *   <td>Smoothing Factor</td>
 *   <td>Tracks current values closely</td>
 *   <td>More historical smoothing</td>
 * </tr>
 * </table>
 * 
 * <h2>Usage Example</h2>
 * 
 * <h3>Basic Usage (Automatic)</h3>
 * <pre>
 * CEEM ceem = new CEEM();
 * 
 * // Each frame
 * ceem.updateTiming(deltaTime);
 * StressEvaluation eval = ceem.evaluateAndOptimize();
 * // Optimization uses stable levels automatically
 * </pre>
 * 
 * <h3>Manual Inspection</h3>
 * <pre>
 * StableStressLevel stable = ceem.getStabilityInfo(ProjectilesSourceID.PROJECTILES);
 * 
 * if (stable != null) {
 *     System.out.println("Raw level: " + stable.rawLevel());
 *     System.out.println("Stable level: " + stable.stableLevel());
 *     System.out.println("Frames at level: " + stable.framesAtLevel());
 *     System.out.println("Is stable: " + stable.isStable());
 *     System.out.println("Is escalating: " + stable.isEscalating());
 * }
 * </pre>
 * 
 * <h3>Progressive UI Feedback</h3>
 * <pre>
 * // Use smoothed magnitude for gradual UI indicators
 * double smoothed = ceem.getSmoothedMagnitude(source);
 * stressBar.setValue((int)(smoothed * 100));
 * </pre>
 * 
 * <h2>Stability States</h2>
 * 
 * <p>
 * A StableStressLevel can be in one of several states:
 * </p>
 * 
 * <h3>Converged (Stable)</h3>
 * <pre>
 * Raw: HIGH, Stable: HIGH, Frames: 15/10
 * → Stress is HIGH and has been for 15 frames
 * → Safe to apply optimization
 * </pre>
 * 
 * <h3>Escalating</h3>
 * <pre>
 * Raw: HIGH, Stable: MODERATE, Frames: 3/10
 * → Stress is rising but not yet stable at HIGH
 * → Wait before applying optimization
 * </pre>
 * 
 * <h3>De-escalating</h3>
 * <pre>
 * Raw: MODERATE, Stable: HIGH, Frames: 5/10
 * → Stress is falling but optimization still active
 * → Wait before restoring
 * </pre>
 * 
 * <h3>Unstable</h3>
 * <pre>
 * Raw: HIGH, Stable: NOMINAL, Frames: 2/10
 * → Just transitioned, insufficient history
 * → Conservative approach: wait for stability
 * </pre>
 * 
 * <h2>Optimization Decision Flow</h2>
 * 
 * <pre>
 *                    ┌──────────────┐
 *                    │ Stress Report│
 *                    └──────┬───────┘
 *                           │
 *                           ▼
 *                    ┌──────────────┐
 *                    │ Record in    │
 *                    │ History      │
 *                    └──────┬───────┘
 *                           │
 *                           ▼
 *                    ┌──────────────┐
 *                    │ Get Stable   │
 *                    │ Level        │
 *                    └──────┬───────┘
 *                           │
 *              ┌────────────┼────────────┐
 *              ▼            ▼            ▼
 *         ┌────────┐  ┌─────────┐  ┌────────┐
 *         │NOMINAL │  │MODERATE │  │HIGH+   │
 *         │        │  │         │  │        │
 *         │Restore │  │Maintain │  │Optimize│
 *         └────────┘  └─────────┘  └────────┘
 * </pre>
 * 
 * <h2>Design Rationale</h2>
 * 
 * <h3>Why Per-Module History?</h3>
 * <p>
 * Different modules have different stress characteristics:
 * </p>
 * <ul>
 * <li>Projectiles: Can spike rapidly</li>
 * <li>Rendering: Changes gradually</li>
 * <li>Physics: Oscillates with gameplay</li>
 * </ul>
 * <p>
 * Per-module history allows each to stabilize independently.
 * </p>
 * 
 * <h3>Why Exponential Moving Average?</h3>
 * <p>
 * EMA gives more weight to recent measurements while maintaining
 * historical context. This balances responsiveness with stability.
 * </p>
 * 
 * <h3>Why Stability Threshold?</h3>
 * <p>
 * Requiring persistence prevents transient spikes from triggering
 * optimization. At 60fps, a 10-frame threshold means ~166ms of
 * sustained stress before action.
 * </p>
 * 
 * <h2>Thread Safety</h2>
 * 
 * <p>
 * StressHistory uses concurrent collections for safe registration.
 * Evaluation and recording are single-threaded (called from game loop).
 * </p>
 * 
 * @see Game.Engine.CEEM.Stability.StressHistory
 * @see Game.Engine.CEEM.Stability.StableStressLevel
 */
package Game.Engine.CEEM.Stability;
