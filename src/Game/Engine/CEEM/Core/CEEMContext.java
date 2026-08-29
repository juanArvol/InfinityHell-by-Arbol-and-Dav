package Game.Engine.CEEM.Core;

import Game.Engine.CEEM.Stress.StressContext;

/**
 * Concrete implementation of StressContext provided by CEEM.
 * 
 * This is an internal CEEM class that implements the StressContext contract.
 * Contributors receive this through the interface, maintaining decoupling.
 */
final class CEEMContext implements StressContext {
    
    private final double deltaTime;
    private final long frameNumber;
    
    CEEMContext(double deltaTime, long frameNumber) {
        this.deltaTime = deltaTime;
        this.frameNumber = frameNumber;
    }
    
    @Override
    public double deltaTime() {
        return deltaTime;
    }
    
    @Override
    public long frameNumber() {
        return frameNumber;
    }
}
