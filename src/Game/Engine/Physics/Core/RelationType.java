package Game.Engine.Physics.Core;

/**
 * Identifica el evaluador especializado que implementa una PhysicalRelation.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 * ── HRFC-027 — Auditoría de Consistencia Arquitectónica ──────────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * RelationType es la clave de despacho que PhysicsSolver usa para obtener
 * el RelationEvaluator correcto desde el EvaluatorRegistry.
 *
 * Cada constante nombra un fenómeno físico real cuya matemática vive
 * exclusivamente en el evaluador correspondiente.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene ninguna lógica física.
 *   ✗ No contiene parámetros ni constantes numéricas.
 *   ✓ Solo es un identificador de despacho hacia el evaluador correcto.
 *
 * ── EVALUADORES CORRESPONDIENTES ─────────────────────────────────────────
 *   FOURIER              → FourierEvaluator             (Thermal)
 *   OHM                  → OhmEvaluator                 (Electrical)
 *   PASCAL               → PascalEvaluator              (Mechanical)
 *   BERNOULLI            → BernoulliEvaluator           (Fluid)
 *   NEWTON               → NewtonEvaluator              (Gravity / Kinematic)
 *   HOOKE                → HookeEvaluator               (Mechanical)
 *   ARCHIMEDES           → ArchimedesEvaluator          (MaterialState)
 *   STOKES               → StokesEvaluator              (MaterialState)
 *   FICK                 → FickEvaluator                (Fluid / MaterialState)
 *   SCHWARZSCHILD        → SchwarzschildEvaluator       (Gravity)
 *   PLANCK               → PlanckEvaluator              (Radiation / Quantum / MaterialState)
 *   JOULE                → JouleEvaluator               (Electrical)
 *   EVENT_HORIZON        → EventHorizonEvaluator        (Gravity)
 *   RADIATION_THERMAL    → RadiationThermalEvaluator    (Thermal)
 *   AMBIENT_DISSIPATION  → AmbientDissipationEvaluator  (Thermal / Electrical / Fluid)
 *   FRICTION_THERMAL     → FrictionThermalEvaluator     (Kinematic → Thermal)   HRFC-030
 *   KINETIC_DISSIPATION  → KineticDissipationEvaluator  (Kinematic → Mechanical) HRFC-030
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un nuevo fenómeno:
 *   1. Añadir una constante aquí.
 *   2. Implementar su RelationEvaluator en el dominio correspondiente.
 *   3. Registrar en EvaluatorRegistry.defaults().
 *
 *   Ningún archivo existente se modifica salvo EvaluatorRegistry.
 */
public enum RelationType {

    // ── Térmica ───────────────────────────────────────────────────────────

    /** Conducción de calor entre pares — ley de Fourier. */
    FOURIER,

    /** Disipación ambiental genérica (térmica, eléctrica, fluídica). */
    AMBIENT_DISSIPATION,

    // ── Eléctrica ─────────────────────────────────────────────────────────

    /** Transferencia de carga entre pares — ley de Ohm. */
    OHM,

    /** Calentamiento por efecto Joule — Q = I² · R · t. */
    JOULE,

    // ── Mecánica ──────────────────────────────────────────────────────────

    /** Expansión volumétrica por temperatura — ley de Pascal. */
    PASCAL,

    /** Disipación de exceso de presión por compresibilidad — ley de Hooke. */
    HOOKE,

    // ── Fluídica ──────────────────────────────────────────────────────────

    /** Difusión de masa entre pares — principio de Bernoulli. */
    BERNOULLI,

    /** Difusión de masa por gradiente — ley de Fick. */
    FICK,

    // ── Gravitacional / Cinemática ────────────────────────────────────────

    /** Aceleración por fuerza externa — segunda ley de Newton. */
    NEWTON,

    /** Atracción gravitacional relativista — métrica de Schwarzschild. */
    SCHWARZSCHILD,

    /** Absorción discontinua al cruzar el horizonte de eventos. */
    EVENT_HORIZON,

    // ── Estado del material ───────────────────────────────────────────────

    /** Empuje hidrostático — principio de Arquímedes. */
    ARCHIMEDES,

    /** Resistencia viscosa y cohesión — ley de Stokes. */
    STOKES,

    // ── Radiación / Cuántica ──────────────────────────────────────────────

    /**
     * Transferencia de radiación entre pares y fenómenos cuánticos
     * — ley de radiación de Planck.
     *
     * Reutilizado por: RadiationRelations (transferencia de radiación),
     *                  QuantumRelations (colapso de función de onda),
     *                  MaterialStateRelations (transición a plasma).
     */
    PLANCK,

    /** Conversión de radiación absorbida en calor. */
    RADIATION_THERMAL,

    // ── Cinemática → Térmica  (HRFC-030) ─────────────────────────────────

    /**
     * Generación de calor por fricción entre una entidad en movimiento y
     * la superficie de contacto.
     *
     * Fenómeno: Q ≈ μ × N × Δx ≈ frictionFactor × mass × gravity × |v| × dt
     *
     * Entradas (KinematicStateProperties):
     *   SPEED, FRICTION_FACTOR, ON_GROUND
     * Salidas (ThermalProperties):
     *   TEMPERATURE (delta positivo → calentamiento)
     *
     * Evaluador: FrictionThermalEvaluator
     */
    FRICTION_THERMAL,

    // ── Cinemática → Mecánica (HRFC-030) ─────────────────────────────────

    /**
     * Disipación de energía cinética en fenómenos mecánicos y térmicos.
     *
     * Modela la conversión de la pérdida de energía cinética (frenado brusco,
     * impacto, deceleración extrema) en incremento de temperatura, presión
     * u otras propiedades del mundo.
     *
     * Fenómeno: |ΔKE| → ΔTemperature + ΔPressure  (partición configurable)
     *
     * Entradas (KinematicStateProperties):
     *   DELTA_KINETIC_ENERGY, ACCELERATION
     * Salidas (ThermalProperties + MechanicalProperties):
     *   TEMPERATURE (calentamiento por disipación inelástica)
     *   PRESSURE    (incremento de presión local)
     *
     * Evaluador: KineticDissipationEvaluator
     */
    KINETIC_DISSIPATION
}
