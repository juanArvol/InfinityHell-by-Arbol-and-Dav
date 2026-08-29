package Game.Engine.CEEM.Identity;

/**
 * Base abstraction for module identification in the engine.
 * 
 * ModuleID provides a typed identity system for engine modules,
 * replacing string-based identification with compile-time safe contracts.
 * 
 * This is the foundational abstraction for CEEM's module identity system.
 * Modules that wish to participate in the stress management system must
 * extend this through StressSourceID.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * Identity is typed, never string-based.
 */
public interface ModuleID {
    
    /**
     * Returns the canonical name of this module.
     * 
     * This method exists solely for diagnostic and logging purposes.
     * CEEM internal logic must never switch on or compare string names.
     * 
     * @return the module's canonical identifier
     */
    String name();
}
