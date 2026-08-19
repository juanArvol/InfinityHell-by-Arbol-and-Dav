package Game.Engine.Physics.SimulaticWorld;

import Game.Engine.GameObjects;
import Game.Engine.Physics.Core.PhysicalRelation;
import Game.Engine.Physics.Core.PhysicsCoordinator;
import Game.Engine.Physics.Core.PhysicsModule;
import Game.Engine.Physics.Core.PhysicsPropertyDependencyGraph;
import Game.Engine.Physics.Core.RelationRegistry;
import Game.Engine.Physics.Electrical.ElectricalModule;
import Game.Engine.Physics.Fluid.FluidModule;
import Game.Engine.Physics.Gravity.GravityModule;
import Game.Engine.Physics.Kinematic.KinematicModule;
import Game.Engine.Physics.Mechanical.MechanicalModule;
import Game.Engine.Physics.SimulaticWorld.Fields.WorldFieldSystem;
import Game.Engine.Physics.SimulaticWorld.Influences.InfluenceSystem;
import Game.Engine.Physics.Thermal.ThermalModule;

import java.util.List;

/**
 * Orquestador del World Simulation Core.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 * ── HRFC-031 — Descomposición de PhysicalState en SimulationContext ───────
 * ── HRFC — Cierre del Refactor Arquitectónico ─────────────────────────────
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
 * ── FLUJO DEFINITIVO ──────────────────────────────────────────────────────
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
 *       ↓
 *   PhysicalState actualizado — única fuente de verdad de propiedades físicas
 *       ↓
 *   Gameplay / Damage / Environment  observan el resultado
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * PhysicsCoordinator es el único responsable de producir comportamiento físico.
 * Ningún otro sistema modifica directamente el PhysicalState de los objetos.
 * Gameplay únicamente observa el estado resultante y decide sus consecuencias.
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 *
 *   // Mundo vacío (sin simulación física activa)
 *   WorldSimulation sim = WorldSimulation.empty();
 *
 *   // Física fundamental sin integración cinemática
 *   WorldSimulation sim = WorldSimulation.withDefaults();
 *
 *   // Física fundamental + integración cinemática (recomendado para la mayoría de mundos)
 *   WorldSimulation sim = WorldSimulation.withKinematicPhysics();
 *
 *   // Composición explícita con módulos
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .module(new ThermalModule())
 *       .module(new ElectricalModule())
 *       .module(new FluidModule())
 *       .module(new MechanicalModule())
 *       .module(new GravityModule())
 *       .module(new KinematicModule())
 *       .build();
 *
 *   // Composición con módulos de dominio adicionales
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .module(new ThermalModule())
 *       .module(new MechanicalModule())
 *       .module(new GravityModule())
 *       .module(new KinematicModule())
 *       .module(new BossPhysicsModule())   // módulo de gameplay
 *       .build();
 *
 * ── RESPONSABILIDAD DE COMPOSICIÓN ───────────────────────────────────────
 * El Builder es el único punto donde se ensamblan los módulos. Cada módulo
 * registra únicamente los evaluadores de su propio dominio. Cuando las
 * relaciones de un dominio declaran un RelationType cuyo evaluador no está
 * registrado por ese mismo módulo, la composición debe incluir el módulo
 * que lo registra.
 *
 * Los módulos nunca se conocen entre ellos. Solo conocen sus propias leyes.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un nuevo dominio:
 *   1. Implementar PhysicsModule en el paquete del dominio.
 *   2. Instalar el módulo con .module(new NuevoDominioModule()).
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
     * WorldSimulation con los dominios físicos fundamentales.
     *
     * Módulos instalados:
     *   ThermalModule     — conducción, disipación ambiental, expansión volumétrica, corrección
     *   ElectricalModule  — transferencia, disipación ambiental, corrección, efecto Joule
     *   FluidModule       — difusión, disipación ambiental, liberación en saturación
     *   MechanicalModule  — presión, compresibilidad, elasticidad
     *
     * Esta combinación garantiza que todos los RelationType utilizados por
     * las relaciones de los cuatro dominios tienen un evaluador registrado.
     *
     * @return WorldSimulation configurado con los dominios físicos fundamentales.
     */
    public static WorldSimulation withDefaults() {
        return builder()
            .module(new ThermalModule())
            .module(new ElectricalModule())
            .module(new FluidModule())
            .module(new MechanicalModule())
            .build();
    }

    /**
     * WorldSimulation con los dominios físicos fundamentales más integración
     * cinemática completa.
     *
     * Módulos instalados:
     *   ThermalModule     — dominio térmico completo
     *   ElectricalModule  — dominio eléctrico completo
     *   FluidModule       — dominio fluídico completo
     *   MechanicalModule  — dominio mecánico completo
     *   GravityModule     — gravitación relativista y dinámica newtoniana
     *   KinematicModule   — gravedad uniforme, calor por rozamiento, disipación cinética
     *
     * Esta combinación garantiza que todos los RelationType utilizados por
     * las relaciones de los seis dominios tienen un evaluador registrado.
     *
     * Para que la integración cinemática sea efectiva, las entidades deben:
     *   1. Tener Physics2DComponent         (Kinematic Physics activo).
     *   2. Tener SimulationContextComponent (contexto compuesto).
     *   3. Tener KinematicBridge como Component.
     *
     * @return WorldSimulation con dominios fundamentales + cinemáticos.
     */
    public static WorldSimulation withKinematicPhysics() {
        return builder()
            .module(new ThermalModule())
            .module(new ElectricalModule())
            .module(new FluidModule())
            .module(new MechanicalModule())
            .module(new GravityModule())
            .module(new KinematicModule())
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
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * @param objects   lista de objetos activos en el mundo este frame.
     * @param deltaTime tiempo transcurrido desde el último frame, en segundos.
     */
    public void update(List<GameObjects> objects, double deltaTime) {
        influenceSystem.update();
        fieldSystem.update(objects);
        coordinator.simulate(objects, deltaTime);
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
     * Usar para registrar relaciones o módulos adicionales en runtime:
     *   world.coordinator().register(specialBossRelation);
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
     * ── RESPONSABILIDAD ──────────────────────────────────────────────────
     * El Builder es el único punto de composición del sistema. Su responsabilidad
     * es ensamblar los módulos que participan en la simulación y garantizar que
     * todos los RelationType utilizados por las relaciones registradas disponen
     * de un evaluador.
     *
     * Cada módulo registra únicamente los evaluadores de su propio dominio.
     * Cuando las relaciones de un módulo declaran un RelationType cuyo evaluador
     * no registra ese mismo módulo, la composición debe incluir el módulo que
     * sí lo registra. Esa responsabilidad recae exclusivamente en quien construye
     * el WorldSimulation.
     *
     * ── API CANÓNICA ──────────────────────────────────────────────────────
     * {@link #module(PhysicsModule)} es la forma estándar de instalar dominios.
     * {@link #register(PhysicalRelation)} y {@link #registerAll(RelationRegistry)}
     * sirven para relaciones individuales que no pertenecen a ningún módulo
     * permanente — por ejemplo, relaciones de jefes, eventos o gameplay.
     *
     * ── REQUERIMIENTOS DE RelationType ───────────────────────────────────
     * Módulos que declaran relaciones con RelationType sin evaluador propio:
     *
     *   ThermalModule       — RelationType.PASCAL, RelationType.HOOKE
     *   ElectricalModule    — RelationType.AMBIENT_DISSIPATION, RelationType.HOOKE
     *   FluidModule         — RelationType.AMBIENT_DISSIPATION
     *   KinematicModule     — RelationType.NEWTON
     *   MaterialStateModule — RelationType.FICK, RelationType.PLANCK
     *   ElectromagneticModule — RelationType.OHM
     *   QuantumModule       — RelationType.PLANCK
     *
     * ── PATRONES DE COMPOSICIÓN ───────────────────────────────────────────
     *
     *   // Física fundamental completa:
     *   WorldSimulation.builder()
     *       .module(new ThermalModule())
     *       .module(new ElectricalModule())
     *       .module(new FluidModule())
     *       .module(new MechanicalModule())
     *       .build();
     *
     *   // Física fundamental + cinemática:
     *   WorldSimulation.builder()
     *       .module(new ThermalModule())
     *       .module(new ElectricalModule())
     *       .module(new FluidModule())
     *       .module(new MechanicalModule())
     *       .module(new GravityModule())
     *       .module(new KinematicModule())
     *       .build();
     *
     *   // Módulo de gameplay con módulos base seleccionados:
     *   WorldSimulation.builder()
     *       .module(new ThermalModule())
     *       .module(new MechanicalModule())
     *       .module(new BossPhysicsModule())
     *       .build();
     *
     *   // Relación de gameplay sin módulo permanente:
     *   WorldSimulation.builder()
     *       .module(new ThermalModule())
     *       .module(new MechanicalModule())
     *       .register(specialBossRelation)
     *       .build();
     */
    public static final class Builder {

        private final PhysicsCoordinator.Builder coordinatorBuilder =
            PhysicsCoordinator.builder();

        private Builder() {}

        /**
         * Instala un módulo de dominio físico.
         *
         * Invoca los dos contratos del módulo en orden:
         *   1. module.registerRelations(...)  — relaciones declarativas del dominio.
         *   2. module.registerEvaluators(...) — evaluadores especializados del dominio.
         *
         * WorldSimulation no conoce qué relaciones ni qué evaluadores contiene
         * el módulo. Solo invoca el contrato.
         *
         * @param module el módulo a instalar. Ignorado si null.
         * @return this.
         */
        public Builder module(PhysicsModule module) {
            if (module == null) return this;
            RelationRegistry registry = new RelationRegistry();
            module.registerRelations(registry);
            coordinatorBuilder.registerAll(registry);
            module.registerEvaluators(coordinatorBuilder.evaluators());
            return this;
        }

        /**
         * Establece el grafo de dependencias físicas del universo.
         *
         * @param graph el grafo de dependencias. Ignorado si null.
         * @return this.
         */
        public Builder graph(PhysicsPropertyDependencyGraph graph) {
            coordinatorBuilder.graph(graph);
            return this;
        }

        /**
         * Registra una PhysicalRelation individual.
         * Usar para relaciones que no pertenecen a ningún módulo de dominio permanente.
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
         * Usar cuando se dispone de un registro ya construido externamente.
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
