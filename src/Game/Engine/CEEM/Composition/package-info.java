/**
 * Inter-module relationship composition for CEEM.
 * 
 * <h2>Purpose</h2>
 * 
 * This package provides abstractions for expressing stress-relevant relationships
 * between engine modules without requiring direct coupling between module implementations.
 * 
 * <h2>Core Abstractions</h2>
 * 
 * <ul>
 * <li>{@link Game.Engine.CEEM.Composition.ModuleRelation} - Contract for module relationships</li>
 * <li>{@link Game.Engine.CEEM.Composition.RelationContext} - Context provided during relation evaluation</li>
 * <li>{@link Game.Engine.CEEM.Composition.RelationEvaluation} - Result of evaluating a relationship</li>
 * <li>{@link Game.Engine.CEEM.Composition.RelationRegistry} - Registry for dynamic relation management</li>
 * </ul>
 * 
 * <h2>Architectural Principle: Composition Over Centralization</h2>
 * 
 * <p>
 * Instead of embedding knowledge about module interactions in CEEM's core,
 * relationships are expressed as independent, composable components.
 * </p>
 * 
 * <h3>Anti-Pattern (Centralized):</h3>
 * <pre>
 * class CEEM {
 *     void evaluate() {
 *         projectileStress = evaluateProjectiles();
 *         renderStress = evaluateRender();
 *         
 *         // CEEM knows about specific module interactions
 *         if (projectileStress.high() && renderStress.moderate()) {
 *             // Handle Projectile → Render interaction
 *         }
 *         
 *         if (enemyStress.high() && physicsStress.moderate()) {
 *             // Handle Enemy → Physics interaction
 *         }
 *     }
 * }
 * </pre>
 * 
 * <h3>Pattern (Compositional):</h3>
 * <pre>
 * // Define relationship independently
 * class ProjectileRenderRelation implements ModuleRelation {
 *     public RelationEvaluation evaluate(RelationContext ctx) {
 *         StressReport projectileReport = ctx.getStressReport(ProjectilesSourceID.PROJECTILES);
 *         StressReport renderReport = ctx.getStressReport(RenderingSourceID.RENDERING);
 *         
 *         // Relation knows how Projectiles affects Rendering
 *         double influence = calculateInfluence(projectileReport, renderReport);
 *         return new RelationEvaluation(
 *             ProjectilesSourceID.PROJECTILES,
 *             RenderingSourceID.RENDERING,
 *             influence,
 *             "High projectile count increases render load"
 *         );
 *     }
 * }
 * 
 * // CEEM simply coordinates
 * class CEEM {
 *     void initialize() {
 *         relationRegistry.register(new ProjectileRenderRelation());
 *     }
 *     
 *     Collection&lt;RelationEvaluation&gt; evaluateRelations() {
 *         return relationRegistry.evaluateRelations(frameNumber, deltaTime);
 *     }
 * }
 * </pre>
 * 
 * <h2>Design Benefits</h2>
 * 
 * <ol>
 * <li><strong>Extensibility:</strong> New relations added without modifying CEEM core</li>
 * <li><strong>Modularity:</strong> Relations are independent components</li>
 * <li><strong>Optional:</strong> Relations only exist when both modules exist</li>
 * <li><strong>Type-safe:</strong> Relations use StressSourceID, not strings</li>
 * <li><strong>Testable:</strong> Relations can be tested in isolation</li>
 * </ol>
 * 
 * <h2>Usage Example</h2>
 * 
 * <h3>Step 1: Implement ModuleRelation</h3>
 * <pre>
 * public class ProjectileRenderRelation implements ModuleRelation {
 *     
 *     {@literal @}Override
 *     public StressSourceID primarySource() {
 *         return ProjectilesSourceID.PROJECTILES;
 *     }
 *     
 *     {@literal @}Override
 *     public StressSourceID secondarySource() {
 *         return RenderingSourceID.RENDERING;
 *     }
 *     
 *     {@literal @}Override
 *     public RelationEvaluation evaluate(RelationContext context) {
 *         StressReport projectileReport = context.getStressReport(primarySource());
 *         StressReport renderReport = context.getStressReport(secondarySource());
 *         
 *         if (projectileReport == null || renderReport == null) {
 *             return null; // One or both modules not active
 *         }
 *         
 *         // Calculate how projectile stress affects rendering
 *         double projectileMagnitude = projectileReport.magnitude();
 *         double renderMagnitude = renderReport.magnitude();
 *         
 *         // Example: projectiles contribute 30% to render stress
 *         double influence = projectileMagnitude * 0.3;
 *         
 *         String diagnostic = String.format(
 *             "Projectiles (%.2f) contributing %.2f to render stress",
 *             projectileMagnitude,
 *             influence
 *         );
 *         
 *         return new RelationEvaluation(
 *             primarySource(),
 *             secondarySource(),
 *             influence,
 *             diagnostic
 *         );
 *     }
 *     
 *     {@literal @}Override
 *     public String description() {
 *         return "Projectile count affects rendering workload";
 *     }
 * }
 * </pre>
 * 
 * <h3>Step 2: Register with CEEM</h3>
 * <pre>
 * CEEM ceem = new CEEM();
 * 
 * // Register modules first
 * ceem.registerContributor(projectileContributor);
 * ceem.registerContributor(renderContributor);
 * 
 * // Register relation
 * ceem.registerRelation(new ProjectileRenderRelation());
 * </pre>
 * 
 * <h3>Step 3: Evaluate Relations</h3>
 * <pre>
 * ceem.updateTiming(deltaTime);
 * StressEvaluation eval = ceem.evaluate();
 * 
 * // Get relational insights
 * Collection&lt;RelationEvaluation&gt; relations = ceem.evaluateRelations();
 * for (RelationEvaluation rel : relations) {
 *     System.out.println(rel.diagnostic());
 *     if (rel.influence() &gt; 0.5) {
 *         // High influence detected
 *     }
 * }
 * </pre>
 * 
 * <h2>Future Evolution</h2>
 * 
 * <p>
 * As the engine grows, additional relation types can be added without
 * architectural changes:
 * </p>
 * 
 * <ul>
 * <li>Projectile ↔ Physics (collision detection workload)</li>
 * <li>Enemy ↔ AI (decision complexity)</li>
 * <li>Render ↔ Particles (visual effects overhead)</li>
 * <li>Physics ↔ World (spatial partitioning pressure)</li>
 * </ul>
 * 
 * <p>
 * Each relation is simply registered and becomes part of CEEM's
 * compositional model.
 * </p>
 * 
 * <h2>Non-Requirements</h2>
 * 
 * <p>
 * Relations do NOT require:
 * </p>
 * <ul>
 * <li>Modifying CEEM.java</li>
 * <li>Modifying existing modules</li>
 * <li>Modifying existing relations</li>
 * <li>String-based identification</li>
 * <li>Null placeholders for missing modules</li>
 * </ul>
 * 
 * @see Game.Engine.CEEM.Composition.ModuleRelation
 * @see Game.Engine.CEEM.Composition.RelationRegistry
 */
package Game.Engine.CEEM.Composition;
