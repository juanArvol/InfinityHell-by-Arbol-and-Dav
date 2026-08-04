package Game.Engine.World;

import Game.Engine.GameObjects;
import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.PhysicsCoordinator;
import Game.Engine.Physics.Core.PropertyDependencyGraph;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Electrical.ElectricalRelations;
import Game.Engine.Physics.Fluid.FluidRelations;
import Game.Engine.Physics.Kinematic.KinematicDerivedRelations;
import Game.Engine.Physics.Thermal.ThermalRelations;
import Game.Engine.World.Fields.WorldFieldSystem;
import Game.Engine.World.Influences.InfluenceSystem;

import java.util.List;

/**
 * Orquestador del World Simulation Core.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
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
 * ── FLUJO DEFINITIVO (HRFC-031) ───────────────────────────────────────────
 *
 *   Input
 *       ↓
 *   Kinematic Physics  (CollisionsSystem — 5 fases; parte de GameObjects.update())
 *       ↓
 *   KinematicBridge    (Component; también en GameObjects.update())
 *       ↓  KinematicState → SimulationContext.updateKinematic()
 *       ↓  StateSnapshot<KinematicState> avanzado (current → previous, new → current)
 *       ↓
 *   WorldSimulation.update()  ← ESTE SISTEMA
 *       [1] InfluenceSystem
 *       [2] WorldFieldSystem
 *       [3] PhysicsCoordinator
 *             PropertyResolver.resolve() → ResolutionPlan
 *             RelationResolver.evaluate(plan, objects, deltaTime)
 *               PhysicsSolver resuelve SimulationContextComponent primero,
 *               exponiendo SimulationContext via EvaluationView.context()
 *               FRICTION_THERMAL     → FrictionThermalEvaluator
 *                                      (lee desde context.kinematic/contact/material/environment)
 *               KINETIC_DISSIPATION  → KineticDissipationEvaluator
 *                                      (calcula deltaKE desde StateSnapshot)
 *               FOURIER / PASCAL / OHM / ...
 *                                      (leen desde PhysicalState via view.has/get/add)
 *       ↓
 *   PhysicalState actualizado — única fuente de verdad de propiedades físicas
 *       ↓
 *   Gameplay / Damage / Environment  observan el resultado
 *
 * ── NOTA SOBRE KinematicBridge ────────────────────────────────────────────
 * KinematicBridge es un Component que se ejecuta en GameObjects.update(),
 * ANTES de que WorldSimulation corra (ver WorldObjectsContainer, paso 1 vs 3).
 * Por tanto WorldSimulation no necesita coordinar KinematicBridge: ya recibe
 * el SimulationContext con el snapshot cinemático del frame actual actualizado.
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * PhysicsCoordinator es el único responsable de producir comportamiento físico.
 * Ningún otro sistema modifica directamente el PhysicalState de los objetos.
 * Gameplay únicamente observa el estado resultante y decide sus consecuencias.
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 *
 *   // Defaults + integración cinemática (HRFC-031) — opción recomendada
 *   WorldSimulation sim = WorldSimulation.withKinematicPhysics();
 *
 *   // Solo relaciones físicas fundamentales (sin integración cinemática)
 *   WorldSimulation sim = WorldSimulation.withDefaults();
 *
 *   // Mundo vacío (sin simulación física activa)
 *   WorldSimulation sim = WorldSimulation.empty();
 *
 *   // Mundo con física personalizada
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .registerAll(new RelationRegistry().registerAll(ThermalRelations.all()))
 *       .registerAll(new RelationRegistry().registerAll(ElectricalRelations.all()))
 *       .registerAll(new RelationRegistry().registerAll(FluidRelations.all()))
 *       .registerAll(new RelationRegistry().registerAll(KinematicDerivedRelations.all()))
 *       .register(gravityRelation)
 *       .build();
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un fenómeno nuevo del dominio físico clásico:
 *   1. Definir un PropertyDescriptor en el catálogo del dominio.
 *   2. Registrar ese PropertyDescriptor en el PhysicalState del objeto.
 *   3. Crear una PhysicalRelation con su RelationType.
 *   4. Implementar su RelationEvaluator y registrarlo en EvaluatorRegistry.
 *   5. Registrar la relación en el Builder o en runtime via coordinator().
 *
 * Añadir un fenómeno nuevo del dominio compuesto (cinemático, material, etc.):
 *   1. El evaluador lee desde view.context() los estados que necesite.
 *   2. Escribe resultados en PhysicalState via view.add() como siempre.
 *   3. Las entidades deben usar SimulationContextComponent.
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
     * WorldSimulation con las relaciones físicas fundamentales distribuidas por dominio.
     *
     * Las relaciones registradas son:
     *   Dominio térmico    → ThermalRelations (5 relaciones)
     *   Dominio eléctrico  → ElectricalRelations (4 relaciones)
     *   Dominio fluídico   → FluidRelations (3 relaciones)
     *
     * @return WorldSimulation configurado con las relaciones físicas fundamentales.
     */
    public static WorldSimulation withDefaults() {
        return builder()
            .registerAll(new RelationRegistry().registerAll(ThermalRelations.all()))
            .registerAll(new RelationRegistry().registerAll(ElectricalRelations.all()))
            .registerAll(new RelationRegistry().registerAll(FluidRelations.all()))
            .build();
    }

    /**
     * WorldSimulation con las relaciones físicas fundamentales más la integración
     * cinemática completa (HRFC-031).
     *
     * Las relaciones registradas son:
     *   Dominio térmico    → ThermalRelations     (5 relaciones)
     *   Dominio eléctrico  → ElectricalRelations  (4 relaciones)
     *   Dominio fluídico   → FluidRelations        (3 relaciones)
     *   Dominio cinemático → KinematicDerivedRelations (2 relaciones, prio 10-11):
     *                          FRICTION_HEAT            — calor por rozamiento
     *                          KINETIC_ENERGY_DISSIPATION — disipación → calor + presión
     *
     * Para que la integración cinemática sea efectiva, las entidades deben:
     *   1. Tener Physics2DComponent         (Kinematic Physics activo).
     *   2. Tener SimulationContextComponent (contexto compuesto HRFC-031).
     *   3. Tener KinematicBridge como Component.
     *
     * Patrón de ensamblado en Assembler:
     *   PhysicalState physical = PhysicalState.builder()
     *       .register(ThermalProperties.TEMPERATURE, 20.0)
     *       .register(MechanicalProperties.PRESSURE)
     *       .build();
     *   MaterialState material = MaterialState.builder()
     *       .frictionCoefficient(0.4).heatCapacity(500.0).build();
     *   SimulationContext ctx = KinematicStateAssembler.buildContext(physical, material);
     *   addComponent(new SimulationContextComponent(ctx));
     *   addComponent(new KinematicBridge());
     *
     * Uso recomendado para mundos con física emergente del movimiento:
     *   WorldSimulation sim = WorldSimulation.withKinematicPhysics();
     *
     * @return WorldSimulation con relaciones fundamentales + cinemáticas.
     */
    public static WorldSimulation withKinematicPhysics() {
        return builder()
            .registerAll(new RelationRegistry().registerAll(ThermalRelations.all()))
            .registerAll(new RelationRegistry().registerAll(ElectricalRelations.all()))
            .registerAll(new RelationRegistry().registerAll(FluidRelations.all()))
            .registerAll(new RelationRegistry().registerAll(KinematicDerivedRelations.all()))
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
     *
     * ── Patrones de composición ───────────────────────────────────────────
     *
     *   // Con física emergente del movimiento (HRFC-030):
     *   WorldSimulation.builder()
     *       .registerAll(new RelationRegistry().registerAll(ThermalRelations.all()))
     *       .registerAll(new RelationRegistry().registerAll(ElectricalRelations.all()))
     *       .registerAll(new RelationRegistry().registerAll(FluidRelations.all()))
     *       .registerAll(new RelationRegistry().registerAll(KinematicDerivedRelations.all()))
     *       .build();
     *
     *   // Con relaciones de un mod adicional:
     *   WorldSimulation.builder()
     *       .registerAll(new RelationRegistry().registerAll(ThermalRelations.all()))
     *       .registerAll(new RelationRegistry().registerAll(KinematicDerivedRelations.all()))
     *       .registerAll(new RelationRegistry().registerAll(ModRelations.all()))
     *       .build();
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
         *   .registerAll(new RelationRegistry().registerAll(ThermalRelations.all()))
         *   .registerAll(new RelationRegistry().registerAll(ElectricalRelations.all()))
         *   .registerAll(new RelationRegistry().registerAll(FluidRelations.all()))
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
