package Game.Engine.World;

import Game.Engine.GameObjects;
import Game.Engine.World.Fields.WorldFieldSystem;
import Game.Engine.World.Influences.InfluenceSystem;
import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.PhysicsCoordinator;
import Game.Engine.World.Physics.PropertyDependencyGraph;
import Game.Engine.World.Solver.CoreRelations;
import Game.Engine.World.Solver.RelationRegistry;
import java.util.List;

/**
 * Orquestador del World Simulation Core.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * WorldSimulation coordina el ciclo de simulación física del mundo.
 * No contiene lógica física. No conoce fenómenos. No conoce propiedades.
 * No conoce relaciones concretas.
 *
 * Su única responsabilidad es ejecutar, en orden garantizado, los tres
 * sistemas que producen el estado físico de cada frame:
 *
 *   [1] InfluenceSystem    — modificaciones directas (magia, auras, poderes)
 *   [2] WorldFieldSystem   — campos espaciales continuos
 *   [3] PhysicsCoordinator — resolución property-driven del estado físico
 *
 * ── FLUJO DEFINITIVO (HRFC-022) ───────────────────────────────────────────
 *
 *   PropertyDependencyGraph (relaciones físicas del universo)
 *       ↓
 *   PhysicsCoordinator
 *       PropertyResolver.resolve() → ResolutionPlan
 *       LawResolver.evaluate(plan, objects, deltaTime)
 *           ↓ EvaluatorRegistry.get(relationType) → RelationEvaluator
 *           ↓ evaluator.evaluate(relation, views, deltaTime)
 *       ↓
 *   PhysicalState actualizado — única fuente de verdad
 *       ↓
 *   Gameplay observa el estado resultante
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * PhysicsCoordinator es el único responsable de producir comportamiento físico.
 * Ningún otro sistema modifica directamente el PhysicalState de los objetos.
 * Gameplay únicamente observa el estado resultante y decide sus consecuencias.
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 *
 *   // Mundo con las relaciones físicas fundamentales
 *   WorldSimulation sim = WorldSimulation.withDefaults();
 *
 *   // Mundo vacío (sin simulación física activa)
 *   WorldSimulation sim = WorldSimulation.empty();
 *
 *   // Mundo con física personalizada
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .registerAll(new RelationRegistry().registerAll(CoreRelations.all()))
 *       .register(gravityRelation)
 *       .build();
 *
 *   // Mundo con relaciones de un mod
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .registerAll(new RelationRegistry().registerAll(CoreRelations.all()))
 *       .registerAll(new RelationRegistry().registerAll(ModRelations.all()))
 *       .build();
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un fenómeno nuevo:
 *
 *   1. Definir un PropertyDescriptor con el id de la nueva propiedad.
 *   2. Registrar ese PropertyDescriptor en el PhysicalState del objeto.
 *   3. Crear una PhysicalRelation con su RelationType y propiedades participantes.
 *   4. Registrar la dependencia en el grafo: graph.addRelation(...)
 *   5. Registrar la relación en el Builder o en runtime via coordinator().
 *
 *   WorldSimulation no cambia. PhysicsCoordinator no cambia.
 *   El Engine no aprende ningún concepto nuevo.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class WorldSimulation {

    private final InfluenceSystem    influenceSystem;
    private final WorldFieldSystem   fieldSystem;
    private final PhysicsCoordinator coordinator;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private WorldSimulation(PhysicsCoordinator coordinator) {
        this.influenceSystem = new InfluenceSystem();
        this.fieldSystem     = new WorldFieldSystem();
        this.coordinator     = coordinator;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * WorldSimulation vacío — sin relaciones físicas registradas.
     * Útil para mundos sin simulación física activa (hubs, cutscenes, menús).
     */
    public static WorldSimulation empty() {
        return builder().build();
    }

    /**
     * WorldSimulation con las diez relaciones físicas fundamentales de CoreRelations.
     *
     * Las relaciones registradas son:
     *   [1]  Expansión volumétrica        temperatura → presión
     *   [2]  Conducción térmica           temperatura entre pares (radio 32)
     *   [3]  Transferencia eléctrica      carga entre pares (radio 32)
     *   [4]  Difusión fluídica            humedad entre pares (radio 32)
     *   [5]  Disipación térmica ambiental temperatura → equilibrio
     *   [6]  Disipación eléctrica         carga → equilibrio
     *   [7]  Disipación fluídica          humedad → equilibrio
     *   [8]  Corrección exceso térmico    temperatura excess > 500
     *   [9]  Corrección exceso eléctrico  carga excess > 10
     *   [10] Liberación en saturación     humedad excess > 0.6
     *
     * @return WorldSimulation configurado con las relaciones físicas fundamentales.
     */
    public static WorldSimulation withDefaults() {
        return builder()
            .registerAll(new RelationRegistry().registerAll(CoreRelations.all()))
            .build();
    }

    // ── Update — entry point del game loop ────────────────────────────────

    /**
     * Ejecuta el ciclo completo de simulación para este frame.
     *
     * Orden garantizado e invariante:
     *   1. InfluenceSystem  — modificaciones directas sobre objetos concretos
     *   2. WorldFieldSystem — campos espaciales continuos
     *   3. PhysicsCoordinator — resolución property-driven del estado físico
     *
     * @param objects   lista de objetos activos en el mundo este frame.
     * @param deltaTime tiempo transcurrido desde el último frame, en segundos.
     */
    public void update(List<GameObjects> objects, double deltaTime) {
        influenceSystem.update();
        fieldSystem.update(objects);
        coordinator.simulate(objects, deltaTime);
    }

    /**
     * Sobrecarga con deltaTime fijo de 1/60 s.
     *
     * @param objects lista de objetos activos en el mundo este frame.
     */
    public void update(List<GameObjects> objects) {
        update(objects, 1.0 / 60.0);
    }

    // ── Acceso a subsistemas ──────────────────────────────────────────────

    /**
     * Sistema de influencias.
     *
     * @return el InfluenceSystem de este mundo.
     */
    public InfluenceSystem influences() { return influenceSystem; }

    /**
     * Sistema de campos espaciales.
     *
     * @return el WorldFieldSystem de este mundo.
     */
    public WorldFieldSystem fields() { return fieldSystem; }

    /**
     * El PhysicsCoordinator del mundo.
     *
     * Usar para registrar relaciones adicionales o dependencias en runtime:
     *   world.coordinator().register(gravityRelation);
     *   world.coordinator().graph().addRelation(tempProp, pressureProp, rel, "thermal");
     *   world.coordinator().registerAll(bossPhysicsRegistry);
     *
     * @return el PhysicsCoordinator de este mundo.
     */
    public PhysicsCoordinator coordinator() { return coordinator; }

    // ── Limpieza de estado transitorio ────────────────────────────────────

    /**
     * Elimina todos los campos e influencias activos.
     * Llamar al cambiar de mundo o escena para evitar estados persistentes.
     *
     * No elimina las relaciones del Coordinator — son la configuración fija
     * del mundo y permanecen activas para el siguiente uso.
     */
    public void clearTransientState() {
        fieldSystem.clear();
        influenceSystem.clear();
        coordinator.forceFullEvaluation();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de WorldSimulation.
     *
     * Permite componer el núcleo de simulación con exactamente el grafo de
     * dependencias y las relaciones físicas que cada mundo necesita.
     *
     * El conocimiento físico se inyecta exclusivamente como instancias de
     * PhysicalRelation. El Builder no conoce fenómenos, dominios ni propiedades.
     */
    public static final class Builder {

        private final PhysicsCoordinator.Builder coordinatorBuilder =
            PhysicsCoordinator.builder();

        private Builder() {}

        /**
         * Establece el grafo de dependencias físicas del universo.
         *
         * @param graph el grafo de dependencias. Ignorado si null.
         * @return this.
         */
        public Builder graph(PropertyDependencyGraph graph) {
            coordinatorBuilder.graph(graph);
            return this;
        }

        /**
         * Registra una PhysicalRelation individual.
         *
         * @param relation la relación declarativa. Ignorada si null.
         * @return this.
         */
        public Builder register(PhysicalRelation relation) {
            coordinatorBuilder.register(relation);
            return this;
        }

        /**
         * Registra todas las relaciones de un RelationRegistry.
         *
         * Ejemplo:
         *   .registerAll(new RelationRegistry().registerAll(CoreRelations.all()))
         *   .registerAll(new RelationRegistry().registerAll(GameplayRelations.all()))
         *
         * @param registry el registro de relaciones. Ignorado si null o vacío.
         * @return this.
         */
        public Builder registerAll(RelationRegistry registry) {
            coordinatorBuilder.registerAll(registry);
            return this;
        }

        /** Construye el WorldSimulation con la configuración acumulada. */
        public WorldSimulation build() {
            return new WorldSimulation(coordinatorBuilder.build());
        }
    }
}
