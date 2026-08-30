package Game.Items.Types.Bullets.CEEM;

import Game.Engine.CEEM.Identity.StressSourceID;

/**
 * Typed identity for the Projectiles stress source.
 * 
 * This enum implements StressSourceID to provide compile-time safe
 * identification of the Projectiles module within CEEM.
 * 
 * ARCHITECTURAL NOTE:
 * Using a singleton enum ensures:
 * 1. Single instance (no duplicate registrations)
 * 2. Compile-time safety
 * 3. No string-based identity
 * 4. Efficient equality checks
 * 
 * IDENTITY PRINCIPLE:
 * The name() method returns a diagnostic string for logging.
 * CEEM internal logic must never switch on or compare names.
 * Identity is established through the typed enum instance itself.
 */
public enum ProjectilesSourceID implements StressSourceID {
    
    /**
     * The singleton instance representing the Projectiles stress source.
     */
    PROJECTILES;
    
    /**
     * Returns the canonical name of this module.
     * 
     * This method exists solely for diagnostic and logging purposes.
     * CEEM and other systems should use the typed identity, not string comparison.
     * 
     * @return the module's canonical identifier
     */
}
