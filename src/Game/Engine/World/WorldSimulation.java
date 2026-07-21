package Game.Engine.World;

import Game.Engine.GameObjects;
import Game.Engine.World.Fields.WorldFieldSystem;
import Game.Engine.World.Influences.InfluenceSystem;
import Game.Engine.World.Physics.PhysicsConstraint;
import Game.Engine.World.Physics.PhysicsEquation;
import Game.Engine.World.Solver.CoreEquations;
import Game.Engine.World.Solver.PairEquation;
import Game.Engine.World.Solver.PhysicsSolver;
import java.util.List;

/**
 * Orquestador del World Simulation Core.
 *
 * ── HRFC-017 — Consolidación Definitiva del Modelo Declarativo ────────────
 *
 * ── FILOSOFÍA ─────────────────────────────────────────────────────────────
 * WorldSimulation coordina el ciclo de simulación física del mundo.
 * No contiene lógica física. No conoce fenómenos. No conoce dominios concretos.
 *
 * Su única responsabilidad es ejecutar, en orden garantizado, los cuatro
 * sistemas que producen el estado físico de cada frame:
 *
 *   [1] InfluenceSystem   — modificaciones directas (magia, auras, poderes)
 *   [2] WorldFieldSystem  — campos espaciales continuos
 *   [3] PhysicsSolver     — resolución declarativa del estado físico
 *
 * El conocimiento físico del mundo reside exclusivamente en los datos
 * registrados en PhysicsSolver: ecuaciones, ecuaciones de par y restricciones.
 * WorldSimulation no necesita ningún cambio cuando se añade un nuevo fenómeno.
 *
 * ── FLUJO DEFINITIVO ──────────────────────────────────────────────────────
 *
 *   PhysicsSolver
 *       ↓
 *   lee PhysicalProperty   (qué propiedades tiene cada objeto)
 *       ↓
 *   lee MaterialProperty   (constantes físicas del material)
 *       ↓
 *   aplica PhysicsEquation (leyes físicas intra-objeto)
 *       ↓
 *   aplica PairEquation    (transferencias entre pares de objetos)
 *       ↓
 *   aplica PhysicsConstraint (correcciones de equilibrio y disipación)
 *       ↓
 *   actualiza PhysicalState  (única fuente de verdad)
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
 * mediante el Builder. Un mundo sin una propiedad concreta simplemente no
 * registra las ecuaciones de esa propiedad.
 *
 *   WorldSimulation sim = WorldSimulation.builder()
 *       .addEquation(CoreEquations.thermalExpansion(0.05))
 *       .addPairEquation(CoreEquations.thermalTransfer())
 *       .addPairEquation(CoreEquations.electricalTransfer())
 *       .addPairEquation(CoreEquations.fluidTransfer())
 *       .addConstraint(CoreEquations.thermalAmbientDissipation(0.0, 0.05))
 *       .addConstraint(CoreEquations.electricalAmbientDissipation(0.02))
 *       .addConstraint(CoreEquations.fluidAmbientDissipation(0.0, 0.005))
 *       .addConstraint(CoreEquations.thermalDissipation(500.0, 0.1))
 *       .addConstraint(CoreEquations.chargeDissipation(10.0, 0.08))
 *       .addConstraint(CoreEquations.fluidSaturationRelease(0.6, 0.05))
 *       .build();
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un nuevo fenómeno físico:
 *   1. Definir una PhysicalProperty en un catálogo propio.
 *   2. Registrar las ecuaciones y restricciones correspondientes.
 *   3. Añadirlas al Builder de WorldSimulation.
 *   → WorldSimulation no cambia. PhysicsSolver no cambia. El Engine no aprende nada nuevo.
 *
 * ── INTEGRACIÓN ──────────────────────────────────────────────────────────
 * WorldSimulation implementa Runnable para integrarse con el game loop.
 * update(objects) es el único método de entrada.
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
     * WorldSimulation vacío — sin ecuaciones, pares ni restricciones.
     * Útil para mundos sin simulación física activa (hubs, cutscenes).
     */
    public static WorldSimulation empty() {
        return builder().build();
    }

    /**
     * WorldSimulation con las ecuaciones y restricciones fundamentales
     * de los tres dominios físicos base: térmica, eléctrica y fluídica.
     *
     * Ecuaciones intra-objeto registradas:
     *   - thermalExpansion(0.05)         — temperatura → presión
     *
     * Ecuaciones de par registradas:
     *   - thermalTransfer()              — intercambio de temperatura entre vecinos
     *   - electricalTransfer()           — propagación de carga entre conductores
     *   - fluidTransfer()                — difusión de humedad entre vecinos
     *
     * Restricciones registradas:
     *   - thermalAmbientDissipation      — temperatura converge hacia 0
     *   - electricalAmbientDissipation   — carga converge hacia 0
     *   - fluidAmbientDissipation        — humedad converge hacia ambiente seco
     *   - thermalDissipation(500, 0.1)   — disipa exceso de energía acumulada
     *   - chargeDissipation(10, 0.08)    — disipa exceso de carga acumulada
     *   - fluidSaturationRelease(0.6, 0.05) — libera humedad en saturación
     *
     * Para mundos con configuración personalizada usar el Builder directamente.
     */
    public static WorldSimulation withDefaults() {
        return builder()
            // ── Ecuaciones intra-objeto ───────────────────────────────────
            .addEquation(CoreEquations.thermalExpansion(0.05))
            // ── Ecuaciones de par ─────────────────────────────────────────
            .addPairEquation(CoreEquations.thermalTransfer())
            .addPairEquation(CoreEquations.electricalTransfer())
            .addPairEquation(CoreEquations.fluidTransfer())
            // ── Restricciones de disipación ambiental ─────────────────────
            .addConstraint(CoreEquations.thermalAmbientDissipation(0.0, 0.05))
            .addConstraint(CoreEquations.electricalAmbientDissipation(0.02))
            .addConstraint(CoreEquations.fluidAmbientDissipation(0.0, 0.005))
            // ── Restricciones de umbral ───────────────────────────────────
            .addConstraint(CoreEquations.thermalDissipation(500.0, 0.1))
            .addConstraint(CoreEquations.chargeDissipation(10.0, 0.08))
            .addConstraint(CoreEquations.fluidSaturationRelease(0.6, 0.05))
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
     * @param objects lista de objetos activos en el mundo este frame.
     */
    public void update(List<GameObjects> objects) {
        influenceSystem.update();
        fieldSystem.update(objects);
        solver.solve(objects);
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
     * Usar para registrar ecuaciones y restricciones adicionales en runtime
     * (p.ej. al activar una zona especial, un jefe con física custom, etc.).
     *
     * @return el PhysicsSolver de este mundo.
     */
    public PhysicsSolver solver() { return solver; }

    // ── Limpieza de estado transitorio ────────────────────────────────────

    /**
     * Elimina todos los campos e influencias activos.
     * Llamar al cambiar de mundo o escena para evitar estados persistentes.
     * No elimina las ecuaciones ni restricciones del Solver — son la configuración
     * fija del mundo y permanecen para el siguiente uso.
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
     * Permite componer el núcleo de simulación con exactamente las ecuaciones,
     * pares y restricciones que cada mundo necesita.
     * El conocimiento físico se inyecta como datos — no como clases de fenómeno.
     */
    public static final class Builder {

        private final PhysicsSolver solver = new PhysicsSolver();

        private Builder() {}

        /**
         * Registra una ecuación física intra-objeto en el Solver.
         *
         * @param equation la ecuación declarativa. Ignorado si null.
         * @return this (para encadenado).
         */
        public Builder addEquation(PhysicsEquation<?, ?> equation) {
            solver.addEquation(equation);
            return this;
        }

        /**
         * Registra una ecuación de transferencia entre pares de objetos en el Solver.
         *
         * @param pairEquation la ecuación de par declarativa. Ignorado si null.
         * @return this (para encadenado).
         */
        public Builder addPairEquation(PairEquation<?> pairEquation) {
            solver.addPairEquation(pairEquation);
            return this;
        }

        /**
         * Registra una restricción física en el Solver.
         *
         * @param constraint la restricción declarativa. Ignorado si null.
         * @return this (para encadenado).
         */
        public Builder addConstraint(PhysicsConstraint<?> constraint) {
            solver.addConstraint(constraint);
            return this;
        }

        /** Construye el WorldSimulation con la configuración acumulada. */
        public WorldSimulation build() {
            return new WorldSimulation(this);
        }
    }
}
