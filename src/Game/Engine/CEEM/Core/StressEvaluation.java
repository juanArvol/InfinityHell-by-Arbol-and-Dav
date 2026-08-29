package Game.Engine.CEEM.Core;

import Game.Engine.CEEM.Stress.StressReport;
import java.util.Collection;
import java.util.Collections;

/**
 * Immutable snapshot of stress evaluation results across all contributors.
 * 
 * StressEvaluation represents the collective stress state of the engine
 * at a specific moment in time.
 * 
 * This is a value object that flows out of CEEM.evaluate(), providing
 * a complete picture of system stress for analysis and decision-making.
 */
public final class StressEvaluation {
    
    private final Collection<StressReport> reports;
    private final long frameNumber;
    
    StressEvaluation(Collection<StressReport> reports, long frameNumber) {
        this.reports = Collections.unmodifiableCollection(reports);
        this.frameNumber = frameNumber;
    }
    
    /**
     * Returns all stress reports from this evaluation.
     * 
     * @return unmodifiable collection of reports
     */
    public Collection<StressReport> reports() {
        return reports;
    }
    
    /**
     * Returns the frame number when this evaluation was performed.
     * 
     * @return frame number
     */
    public long frameNumber() {
        return frameNumber;
    }
    
    /**
     * Returns the maximum stress magnitude across all reports.
     * 
     * This provides a quick assessment of peak stress.
     * 
     * @return maximum magnitude, or 0.0 if no reports
     */
    public double maxMagnitude() {
        return reports.stream()
            .mapToDouble(StressReport::magnitude)
            .max()
            .orElse(0.0);
    }
    
    /**
     * Returns the total count of reports.
     * 
     * @return number of contributors that reported
     */
    public int reportCount() {
        return reports.size();
    }
    
    @Override
    public String toString() {
        return String.format(
            "StressEvaluation[frame=%d, reports=%d, maxMagnitude=%.2f]",
            frameNumber,
            reportCount(),
            maxMagnitude()
        );
    }
}
