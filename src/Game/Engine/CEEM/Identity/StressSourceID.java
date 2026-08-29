package Game.Engine.CEEM.Identity;

/**
 * Identifies a module as a potential source of computational stress.
 * 
 * StressSourceID extends ModuleID to mark modules that participate
 * in the CEEM stress evaluation and optimization lifecycle.
 * 
 * Each module that wishes to contribute stress information and receive
 * optimization coordination must define its own StressSourceID implementation.
 * 
 * ARCHITECTURAL PRINCIPLE:
 * StressSourceID is a marker extension. It carries no additional behavior
 * beyond ModuleID, but semantically declares: "this module participates in CEEM".
 * 
 * EXAMPLE HIERARCHY:
 * <pre>
 * ModuleID
 *    │
 *    └── StressSourceID
 *           │
 *           ├── ProjectilesSourceID
 *           ├── RenderingSourceID
 *           ├── PhysicsSourceID
 *           └── EnemiesSourceID
 * </pre>
 */
public interface StressSourceID extends ModuleID {
}
