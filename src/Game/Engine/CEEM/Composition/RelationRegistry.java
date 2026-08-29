package Game.Engine.CEEM.Composition;

import Game.Engine.CEEM.Core.CEEMDiagnostics;
import Game.Engine.CEEM.Identity.StressSourceID;
import Game.Engine.CEEM.Stress.StressReport;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing module relationships in CEEM.
 * 
 * RelationRegistry provides dynamic, additive registration of relationships
 * between modules without requiring CEEM to know about specific relations.
 * 
 * ARCHITECTURAL PRINCIPLE: ADDITIVE EXTENSIBILITY
 * 
 * Relations are registered, not hardcoded. This allows:
 * - Adding new relations without modifying CEEM core
 * - Modules to declare relationships independently
 * - Optional participation (relations exist only when registered)
 * - Future relation types to be added seamlessly
 * 
 * DESIGN PATTERN:
 * 
 * <pre>
 *   CEEM
 *     │
 *     └── RelationRegistry
 *            │
 *            ├── ProjectileRenderRelation
 *            ├── ProjectilePhysicsRelation
 *            ├── EnemyAIRelation
 *            └── ... (future relations)
 * </pre>
 * 
 * Each relation is an independent component.
 * Adding a new relation is simply: registry.register(newRelation)
 * 
 * EVALUATION MODEL:
 * 
 * Relations are only evaluated when both participating modules
 * are currently registered as stress contributors.
 * 
 * If Projectiles exists but Rendering doesn't, ProjectileRenderRelation
 * is dormant (registered but not evaluated).
 * 
 * LIFECYCLE:
 * 
 * 1. Relation implementation is created
 * 2. relation.register() adds it to registry
 * 3. Each frame, registry evaluates applicable relations
 * 4. Evaluation results inform optimization decisions
 * 
 * THREAD SAFETY:
 * 
 * Uses concurrent collections to allow registration from
 * different initialization contexts.
 * 
 * IDENTITY PRINCIPLE:
 * 
 * Relations are identified by their typed StressSourceID pair,
 * not by string concatenation. This maintains type safety and
 * consistency with CEEM's architectural principle:
 * "Identity is typed, never string-based."
 */
public final class RelationRegistry {
    
    /**
     * Typed key for relation identity based on source pair.
     * 
     * This ensures relation identity is based on typed StressSourceID
     * instances, not on string concatenation.
     * 
     * Relations are directional: (A → B) is distinct from (B → A).
     */
    private static final class RelationKey {
        private final StressSourceID primary;
        private final StressSourceID secondary;
        
        RelationKey(StressSourceID primary, StressSourceID secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RelationKey)) return false;
            RelationKey other = (RelationKey) obj;
            return primary.equals(other.primary) && secondary.equals(other.secondary);
        }
        
        @Override
        public int hashCode() {
            return 31 * primary.hashCode() + secondary.hashCode();
        }
    }
    
    private final Map<RelationKey, ModuleRelation> relations;
    private final Set<StressSourceID> activeModules;
    private final Map<StressSourceID, StressReport> cachedReports;
    
    /**
     * Creates a new relation registry.
     */
    public RelationRegistry() {
        this.relations = new ConcurrentHashMap<>();
        this.activeModules = ConcurrentHashMap.newKeySet();
        this.cachedReports = new ConcurrentHashMap<>();
    }
    
    /**
     * Registers a module relationship.
     * 
     * The relation will be evaluated when both participating modules
     * are active as stress contributors.
     * 
     * IDEMPOTENCY:
     * Registering the same relation multiple times replaces the previous instance.
     * Relations are keyed by their typed source pair to prevent duplicates.
     * 
     * NULL CONTRACT:
     * Relation must not be null. This indicates a programming error and will
     * fail fast with IllegalArgumentException.
     * 
     * @param relation the module relationship to register
     * @throws IllegalArgumentException if relation is null
     */
    public void register(ModuleRelation relation) {
        if (relation == null) {
            throw new IllegalArgumentException("Relation cannot be null");
        }
        
        RelationKey key = new RelationKey(relation.primarySource(), relation.secondarySource());
        relations.put(key, relation);
    }
    
    /**
     * Unregisters a module relationship.
     * 
     * NULL CONTRACT:
     * Both source IDs must be non-null. If either is null, the operation
     * silently skips without error. This is a permissive design for cleanup
     * scenarios.
     * 
     * @param primarySource the primary source of the relation
     * @param secondarySource the secondary source of the relation
     */
    public void unregister(StressSourceID primarySource, StressSourceID secondarySource) {
        if (primarySource != null && secondarySource != null) {
            RelationKey key = new RelationKey(primarySource, secondarySource);
            relations.remove(key);
        }
    }
    
    /**
     * Updates the set of active modules.
     * 
     * This should be called by CEEM after evaluating stress contributors
     * to inform the registry which modules are currently participating.
     * 
     * Relations involving inactive modules will not be evaluated.
     * 
     * @param modules collection of currently active stress sources
     */
    public void updateActiveModules(Collection<StressSourceID> modules) {
        activeModules.clear();
        if (modules != null) {
            activeModules.addAll(modules);
        }
    }
    
    /**
     * Updates cached stress reports for relation evaluation.
     * 
     * Relations query these reports through RelationContext to understand
     * the current stress state of their participating modules.
     * 
     * @param reports collection of current stress reports
     */
    public void updateStressReports(Collection<StressReport> reports) {
        cachedReports.clear();
        if (reports != null) {
            for (StressReport report : reports) {
                cachedReports.put(report.source(), report);
            }
        }
    }
    
    /**
     * Evaluates all applicable relations.
     * 
     * A relation is applicable if both its primary and secondary sources
     * are currently active modules.
     * 
     * ARCHITECTURAL NOTE:
     * Relations always return an evaluation (never null).
     * Inactive or irrelevant relationships return influence = 0.0
     * 
     * @param frameNumber current frame number
     * @param deltaTime frame delta time in seconds
     * @return collection of relation evaluations
     */
    public Collection<RelationEvaluation> evaluateRelations(long frameNumber, double deltaTime) {
        RelationContext context = new RelationContextImpl(frameNumber, deltaTime, cachedReports);
        
        List<RelationEvaluation> evaluations = new ArrayList<>();
        
        for (ModuleRelation relation : relations.values()) {
            // Only evaluate if both modules are active
            if (activeModules.contains(relation.primarySource()) && 
                activeModules.contains(relation.secondarySource())) {
                
                try {
                    RelationEvaluation evaluation = relation.evaluate(context);
                    
                    // Validate contract: evaluate() must never return null
                    if (evaluation == null) {
                        CEEMDiagnostics.critical(
                            "ModuleRelation.evaluate() returned null for " + relation.description() + ". " +
                            "This violates the contract. Relations must return evaluation with influence=0.0 if inactive."
                        );
                        continue;
                    }
                    
                    evaluations.add(evaluation);
                } catch (Exception e) {
                    // Log but don't fail entire evaluation
                    CEEMDiagnostics.error(
                        "Error evaluating relation " + relation.description(),
                        e
                    );
                }
            }
        }
        
        return evaluations;
    }
    
    /**
     * Returns all registered relations.
     * 
     * This creates a defensive copy to ensure the returned collection
     * is a true snapshot, unaffected by subsequent registrations or
     * unregistrations.
     * 
     * @return unmodifiable snapshot of relations
     */
    public Collection<ModuleRelation> getRelations() {
        return Collections.unmodifiableList(new ArrayList<>(relations.values()));
    }
    
    /**
     * Returns relations involving a specific module.
     * 
     * Useful for querying which relationships affect a particular module.
     * 
     * @param source the module to query
     * @return relations where source is primary or secondary
     */
    public Collection<ModuleRelation> getRelationsFor(StressSourceID source) {
        if (source == null) {
            return Collections.emptyList();
        }
        
        return relations.values().stream()
            .filter(r -> r.primarySource().equals(source) || r.secondarySource().equals(source))
            .collect(Collectors.toList());
    }
    
    /**
     * Returns the number of registered relations.
     * 
     * @return relation count
     */
    public int relationCount() {
        return relations.size();
    }
    
    /**
     * Checks if a relation is registered between two modules.
     * 
     * @param primarySource the primary source
     * @param secondarySource the secondary source
     * @return true if relation exists
     */
    public boolean hasRelation(StressSourceID primarySource, StressSourceID secondarySource) {
        RelationKey key = new RelationKey(primarySource, secondarySource);
        return relations.containsKey(key);
    }
    
    /**
     * Internal implementation of RelationContext.
     * 
     * This class bridges between CEEM's cached state and the
     * RelationContext interface that relations consume.
     */
    private static class RelationContextImpl implements RelationContext {
        
        private final long frameNumber;
        private final double deltaTime;
        private final Map<StressSourceID, StressReport> reports;
        
        RelationContextImpl(
                long frameNumber, 
                double deltaTime,
                Map<StressSourceID, StressReport> reports) {
            this.frameNumber = frameNumber;
            this.deltaTime = deltaTime;
            this.reports = reports;
        }
        
        @Override
        public long frameNumber() {
            return frameNumber;
        }
        
        @Override
        public StressReport getStressReport(StressSourceID source) {
            return reports.get(source);
        }
        
        @Override
        public double deltaTime() {
            return deltaTime;
        }
    }
}
