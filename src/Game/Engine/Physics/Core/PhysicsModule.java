package Game.Engine.Physics.Core;

/**
 * Unidad de registro de un dominio físico en el simulador del mundo.
 *
 * ── HRFC — Cierre del Refactor Arquitectónico ─────────────────────────────
 * ── HRFC — Eliminación del último punto de composición central (PhysicsEvaluators)
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * PhysicsModule es el único contrato que un dominio físico necesita
 * implementar para integrarse completamente en el simulador.
 *
 * Cada módulo encapsula dos registros:
 *   1. {@link #registerRelations(RelationRegistry)}  — relaciones declarativas del dominio.
 *   2. {@link #registerEvaluators(EvaluatorRegistry)} — evaluadores especializados del dominio.
 *
 * Un módulo conoce únicamente su propio dominio. Nunca conoce los de otros.
 *
 * ── PRINCIPIO DE DISEÑO ───────────────────────────────────────────────────
 * Antes de esta evolución, WorldSimulation.Builder concentraba los
 * evaluadores de todos los dominios en PhysicsEvaluators.all().
 * Eso significaba un punto de modificación obligatorio cada vez que
 * aparecía un dominio nuevo.
 *
 * Con este contrato el conocimiento queda completamente distribuido:
 * cada dominio sabe qué relaciones produce y qué evaluadores las resuelven.
 * WorldSimulation solo invoca el contrato; no aprende física.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para añadir un nuevo dominio al simulador:
 *   1. Crear XxxModule que implemente PhysicsModule.
 *   2. Implementar registerRelations() con las relaciones del dominio.
 *   3. Implementar registerEvaluators() con los evaluadores del dominio.
 *   4. Instalar: WorldSimulation.builder().module(new XxxModule()).build()
 *
 * Ningún archivo existente necesita modificarse.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene lógica de simulación.
 *   ✗ No conoce otros módulos.
 *   ✓ Registra únicamente las relaciones de su propio dominio.
 *   ✓ Registra únicamente los evaluadores de su propio dominio.
 */
public interface PhysicsModule {

    /**
     * Registra las relaciones declarativas de este dominio.
     *
     * El módulo llama a registry.registerAll(...) o registry.register(...)
     * con las relaciones de su catálogo. No hace nada más.
     *
     * @param relations el registro donde añadir las relaciones. Nunca null.
     */
    void registerRelations(RelationRegistry relations);

    /**
     * Registra los evaluadores especializados de este dominio.
     *
     * El módulo llama a evaluators.register(RelationType.X, new XEvaluator())
     * para cada evaluador que pertenece a su dominio. No hace nada más.
     *
     * @param evaluators el registro donde añadir los evaluadores. Nunca null.
     */
    void registerEvaluators(EvaluatorRegistry evaluators);
}
