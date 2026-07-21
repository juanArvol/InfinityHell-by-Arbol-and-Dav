package Game.Engine.World;

import Game.Engine.GameObjects;
import Game.Engine.World.Fields.WorldFieldSystem;
import Game.Engine.World.Influences.InfluenceSystem;
import Game.Engine.World.Physics.PhysicsLaw;
import Game.Engine.World.Solver.CoreLaws;
import Game.Engine.World.Solver.LawRegistry;
import Game.Engine.World.Solver.PhysicsSolver;
import java.util.List;

/**
 * Orquestador del World Simulation Core.
 *
 * ── HRFC-019 — Eliminación Definitiva del Modelo Orientado a Tipos de Ley ─
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * WorldSimulation coordina el ciclo de simulación física del mundo.
 * No contiene lógica física. No conoce fenómenos. No conoce propiedades.
 * No conoce leyes concretas.
 *
 * Su única responsabilidad es ejecutar, en orden garantizado, los tres
 * sistemas que producen el estado físico de cada frame:
 *
 *   [1] InfluenceSystem   — modificaciones directas (magia, auras, poderes)
 *   [2] WorldFieldSystem  — campos espaciales continuos
 *   [3] PhysicsSolver     — resolución declarativa del estado físico
 *
 * ── FLUJO DEFINITIVO ──────────────────────────────────────────────────────
 *
 *   LawRegistry (instancias de PhysicsLaw)
 *       ↓
 *   PhysicsSolver.registerAll(registry)
 *       ↓
 *   para cada frame → solver.solve(objects, deltaTime):
 *       for (PhysicsLaw law : laws)
 *           law.solve(worldContext)
 *       ↓
 *   PhysicalState actualizado — única fuente de verdad
 *       ↓
 *   Gameplay observa el estado resultante
 *
 * ── INVARIANTE CENTRAL ────────────────────────────────────────────────────
 * PhysicsSolver es el único responsable de producir comportamiento físico.
 * Ningún otro sistema modifica directamente el PhysicalState de los objetos.
 * Gameplay únicamente observa el estado resultante y decide sus consecuencias.
 *
 * ── COMPOSICIÓN ──────────────────────────────────────────────────────────
 * WorldSimulation no impone ninguna configuración concreta. Todo se inyecta
 * mediante el Builder. Un mundo puede registrar cualquier combinación de leyes
 * de cualquier catálogo (CoreLaws, GameplayLaws, BossLaws, ModLaws...).
 *
 *   // Mundo con las 10 leyes físicas fundamentales
 *   WorldSimulation sim = WorldSimulation.withDefaults();
 *
 *   // Mundo vacío (sin simulación física activa)
 *   WorldSimulation sim = WorldSimulation.empty();
 *
 *   // Mundo con física personalizada
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .registerAll(new LawRegistry().registerAll(CoreLaws.all()))
 *       .register(gravityLaw)
 *       .register(magnetismLaw)
 *       .build();
 *
 *   // Mundo con leyes de un mod
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .registerAll(new LawRegistry().registerAll(CoreLaws.all()))
 *       .registerAll(new LawRegistry().registerAll(ModLaws.all()))
 *       .build();
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un fenómeno nuevo (radiación, magnetismo, gravedad, plasma,
 * superconductividad, efectos cuánticos, radio de Schwarzschild...):
 *
 *   1. Definir un PropertyDescriptor con el id de la nueva propiedad.
 *   2. Registrar ese PropertyDescriptor en el PhysicalState del objeto.
 *   3. Crear una PhysicsLaw con la ecuación del fenómeno.
 *   4. Registrarla en el Builder o en runtime via solver().
 *
 *   WorldSimulation no cambia. PhysicsSolver no cambia.
 *   El Engine no aprende ningún concepto nuevo.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 * No es thread-safe. Usar exclusivamente desde el game loop thread.
 */
public final class WorldSimulation {

    private final InfluenceSystem  influenceSystem;
    private final WorldFieldSystem fieldSystem;
    private final PhysicsSolver    solver;

    // ── Constructor privado — usar Builder ────────────────────────────────

    private WorldSimulation(Builder b) {
        this.influenceSystem = new InfluenceSystem();
        this.fieldSystem     = new WorldFieldSystem();
        this.solver          = b.solver;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Punto de entrada del Builder. */
    public static Builder builder() { return new Builder(); }

    /**
     * WorldSimulation vacío — sin leyes físicas registradas.
     * Útil para mundos sin simulación física activa (hubs, cutscenes, menús).
     */
    public static WorldSimulation empty() {
        return builder().build();
    }

    /**
     * WorldSimulation con las diez leyes físicas fundamentales de CoreLaws.
     *
     * Las leyes registradas son:
     *   [1]  Expansión volumétrica        temperatura → presión
     *   [2]  Transferencia térmica        entre pares dentro de radio 32
     *   [3]  Transferencia eléctrica      entre pares dentro de radio 32
     *   [4]  Difusión fluídica            entre pares dentro de radio 32
     *   [5]  Disipación térmica ambiental temperatura → equilibrio
     *   [6]  Disipación eléctrica         carga → equilibrio
     *   [7]  Disipación fluídica          humedad → equilibrio
     *   [8]  Disipación de exceso térmico corrección energética umbral 500
     *   [9]  Disipación de exceso eléctrico corrección de carga umbral 10
     *   [10] Liberación en saturación     corrección fluídica umbral 0.6
     *
     * Para configuración personalizada usar el Builder directamente.
     *
     * @return WorldSimulation configurado con las leyes físicas fundamentales.
     */
    public static WorldSimulation withDefaults() {
        return builder()
            .registerAll(new LawRegistry().registerAll(CoreLaws.all()))
            .build();
    }

    // ── Update — entry point del game loop ────────────────────────────────

    /**
     * Ejecuta el ciclo completo de simulación para este frame.
     *
     * Orden garantizado e invariante:
     *   1. InfluenceSystem  — modificaciones directas sobre objetos concretos
     *   2. WorldFieldSystem — campos espaciales continuos sobre todos los objetos
     *   3. PhysicsSolver    — resolución declarativa del estado físico
     *
     * @param objects   lista de objetos activos en el mundo este frame.
     * @param deltaTime tiempo transcurrido desde el último frame, en segundos.
     */
    public void update(List<GameObjects> objects, double deltaTime) {
        influenceSystem.update();
        fieldSystem.update(objects);
        solver.solve(objects, deltaTime);
    }

    // ── Acceso a subsistemas ──────────────────────────────────────────────

    /**
     * Sistema de influencias.
     * Usar desde Gameplay para aplicar modificaciones directas sobre objetos
     * (magia, auras, poderes, debuffs con duración).
     *
     * @return el InfluenceSystem de este mundo.
     */
    public InfluenceSystem influences() { return influenceSystem; }

    /**
     * Sistema de campos espaciales.
     * Usar desde Gameplay para añadir campos de calor, electricidad, gravedad,
     * u otros efectos de área.
     *
     * @return el WorldFieldSystem de este mundo.
     */
    public WorldFieldSystem fields() { return fieldSystem; }

    /**
     * El PhysicsSolver del mundo.
     *
     * Usar para registrar leyes adicionales en runtime:
     *   world.solver().addLaw(gravityLaw);
     *   world.solver().registerAll(bossPhysicsRegistry);
     *
     * @return el PhysicsSolver de este mundo.
     */
    public PhysicsSolver solver() { return solver; }

    // ── Limpieza de estado transitorio ────────────────────────────────────

    /**
     * Elimina todos los campos e influencias activos.
     * Llamar al cambiar de mundo o escena para evitar estados persistentes.
     *
     * No elimina las leyes del Solver — son la configuración fija del mundo
     * y permanecen activas para el siguiente uso.
     */
    public void clearTransientState() {
        fieldSystem.clear();
        influenceSystem.clear();
    }

    // ═════════════════════════════════════════════════════════════════════
    // Builder
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Builder de WorldSimulation.
     *
     * Permite componer el núcleo de simulación con exactamente las leyes
     * físicas que cada mundo necesita, de cualquier catálogo.
     *
     * El conocimiento físico se inyecta exclusivamente como instancias de
     * PhysicsLaw. El Builder no conoce fenómenos, dominios ni propiedades.
     */
    public static final class Builder {

        private final PhysicsSolver solver = new PhysicsSolver();

        private Builder() {}

        /**
         * Registra una ley física individual en el Solver.
         *
         * @param law la ley declarativa. Ignorado si null.
         * @return this (para encadenado).
         */
        public Builder register(PhysicsLaw law) {
            solver.addLaw(law);
            return this;
        }

        /**
         * Registra todas las leyes de un LawRegistry en el Solver.
         *
         * Ejemplo:
         *   .registerAll(new LawRegistry().registerAll(CoreLaws.all()))
         *   .registerAll(new LawRegistry().registerAll(GameplayLaws.all()))
         *
         * @param registry el registro de leyes. Ignorado si null o vacío.
         * @return this (para encadenado).
         */
        public Builder registerAll(LawRegistry registry) {
            solver.registerAll(registry);
            return this;
        }

        /** Construye el WorldSimulation con la configuración acumulada. */
        public WorldSimulation build() {
            return new WorldSimulation(this);
        }
    }
}
