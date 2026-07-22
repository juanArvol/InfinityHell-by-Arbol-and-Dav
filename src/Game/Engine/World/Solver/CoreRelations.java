package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreProperties;
import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.RelationConstraint;
import Game.Engine.World.Physics.RelationType;

/**
 * Catálogo de las relaciones físicas fundamentales del World Simulation Core.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * CoreRelations es un catálogo externo al Engine Core.
 *
 * El PhysicsCoordinator no lo conoce. El RelationRegistry no lo produce.
 * Es simplemente un conjunto de instancias PhysicalRelation listas para
 * registrar en cualquier RelationRegistry o PhysicsCoordinator.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Estas relaciones no ejecutan comportamiento. Describen el universo.
 *
 * Cada relación declara:
 *   - su RelationType   → identifica qué fenómeno físico describe
 *   - sus propiedades participantes → qué propiedades involucra
 *   - sus restricciones → condiciones bajo las cuales aplica
 *   - su prioridad      → orden relativo de evaluación
 *
 * El procedimiento matemático correspondiente vive exclusivamente en el
 * evaluador especializado del sistema de resolución. Nunca aquí.
 *
 * ── REEMPLAZA A CoreLaws ──────────────────────────────────────────────────
 * CoreLaws contenía PhysicsLaw con lambdas solve(WorldContext).
 * CoreRelations contiene PhysicalRelation puramente declarativas.
 * No existe ningún algoritmo, callback ni referencia al mundo aquí.
 *
 * ── RELACIONES INCLUIDAS ──────────────────────────────────────────────────
 *
 *   [1]  VOLUMETRIC_EXPANSION         temperatura → presión              PASCAL
 *   [2]  THERMAL_CONDUCTION           temperatura ↔ temperatura (pares)  FOURIER
 *   [3]  ELECTRICAL_TRANSFER          carga ↔ carga (pares)              OHM
 *   [4]  FLUID_DIFFUSION              humedad ↔ humedad (pares)          BERNOULLI
 *   [5]  THERMAL_AMBIENT_DISSIPATION  temperatura → equilibrio           FOURIER
 *   [6]  ELECTRICAL_DISSIPATION       carga → equilibrio                 OHM
 *   [7]  FLUID_AMBIENT_DISSIPATION    humedad → equilibrio               FICK
 *   [8]  THERMAL_EXCESS_CORRECTION    corrección cuando energía > umbral HOOKE
 *   [9]  ELECTRICAL_EXCESS_CORRECTION corrección cuando carga > umbral   HOOKE
 *   [10] FLUID_SATURATION_RELEASE     corrección fluídica en saturación  FICK
 *
 * ── DEPENDENCIAS FÍSICAS DECLARADAS ──────────────────────────────────────
 * Las dependencias entre propiedades que estas relaciones describen deben
 * registrarse en el PropertyDependencyGraph del mundo:
 *
 *   temperatura → presión          (VOLUMETRIC_EXPANSION)
 *   temperatura → temperatura      (THERMAL_CONDUCTION entre pares)
 *   carga → carga                  (ELECTRICAL_TRANSFER entre pares)
 *   humedad → humedad              (FLUID_DIFFUSION entre pares)
 *   temperatura → temperatura      (THERMAL_AMBIENT_DISSIPATION)
 *   carga → carga                  (ELECTRICAL_DISSIPATION)
 *   humedad → humedad              (FLUID_AMBIENT_DISSIPATION)
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva relación física sin modificar nada del Core:
 *
 *   // 1. PropertyDescriptor de la nueva propiedad (en cualquier catálogo):
 *   PropertyDescriptor VELOCITY_Y =
 *       PropertyDescriptor.of("velocity_y", 0.0, "Velocidad vertical en u/s");
 *
 *   // 2. La relación referencia el descriptor directamente:
 *   PhysicalRelation GRAVITY = PhysicalRelation.builder()
 *       .name("gravity")
 *       .relationType(RelationType.NEWTON)
 *       .participating(VELOCITY_Y)
 *       .priority(1)
 *       .build();
 *
 *   // 3. Registrar en el coordinator (no se modifica nada del Core):
 *   world.coordinator().register(GRAVITY);
 */
public final class CoreRelations {

    private CoreRelations() {}

    // ── Relación 1: Expansión volumétrica ─────────────────────────────────

    /**
     * La temperatura genera presión interna proporcional a la incompresibilidad.
     *
     * Fenómeno: PASCAL (expansión volumétrica / ley de Pascal)
     * Dependencia física: temperatura → presión
     * Evaluador: PascalEvaluator
     * Restricciones: MIN_DELTA(1e-6) para ignorar temperaturas cercanas a 0
     */
    public static final PhysicalRelation VOLUMETRIC_EXPANSION = PhysicalRelation.builder()
        .name("volumetric_expansion")
        .relationType(RelationType.PASCAL)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.PRESSURE,
            CoreProperties.COMPRESSIBILITY)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(5)
        .build();

    // ── Relación 2: Conducción térmica entre pares ────────────────────────

    /**
     * El calor fluye del objeto más caliente al más frío por conducción.
     * La velocidad depende de la menor conductividad de los dos.
     * La inercia depende de la capacidad calorífica del receptor.
     *
     * Fenómeno: FOURIER (ley de conducción térmica de Fourier)
     * Dependencia física: temperatura → temperatura (entre pares)
     * Evaluador: FourierEvaluator
     * Restricciones: MAX_DISTANCE(32), MIN_DELTA(1e-6)
     */
    public static final PhysicalRelation THERMAL_CONDUCTION = PhysicalRelation.builder()
        .name("thermal_conduction")
        .relationType(RelationType.FOURIER)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.THERMAL_CONDUCTIVITY,
            CoreProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.maxDistance(32.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(100)
        .build();

    // ── Relación 3: Transferencia eléctrica entre pares ───────────────────

    /**
     * La carga eléctrica fluye del objeto con mayor potencial al de menor.
     * La velocidad depende de la conductividad eléctrica.
     *
     * OhmEvaluator escribe la corriente calculada en FrameMagnitudes.CURRENT
     * para que JOULE_HEATING la consuma en la misma pasada del frame.
     * No existe ninguna propiedad puente en PhysicalState.
     *
     * Fenómeno: OHM (ley de Ohm)
     * Dependencia física: carga → carga (entre pares)
     * Evaluador: OhmEvaluator
     * Restricciones: MAX_DISTANCE(32), MIN_DELTA(1e-6)
     */
    public static final PhysicalRelation ELECTRICAL_TRANSFER = PhysicalRelation.builder()
        .name("electrical_transfer")
        .relationType(RelationType.OHM)
        .participating(
            CoreProperties.CHARGE,
            CoreProperties.ELECTRICAL_CONDUCTIVITY)
        .constraint(RelationConstraint.maxDistance(32.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(100)
        .build();

    // ── Relación 4: Difusión fluídica entre pares ─────────────────────────

    /**
     * La humedad se difunde del objeto con mayor concentración al de menor.
     * La velocidad está modulada por la viscosidad del fluido.
     *
     * Fenómeno: BERNOULLI (principio de Bernoulli — flujo en fluidos)
     * Dependencia física: humedad → humedad (entre pares)
     * Evaluador: BernoulliEvaluator
     * Restricciones: MAX_DISTANCE(32), MIN_DELTA(1e-6)
     */
    public static final PhysicalRelation FLUID_DIFFUSION = PhysicalRelation.builder()
        .name("fluid_diffusion")
        .relationType(RelationType.BERNOULLI)
        .participating(
            CoreProperties.HUMIDITY,
            CoreProperties.HUMIDITY_ABSORPTION,
            CoreProperties.VISCOSITY)
        .constraint(RelationConstraint.maxDistance(32.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(100)
        .build();

    // ── Relación 5: Disipación térmica ambiental ──────────────────────────

    /**
     * La temperatura de cada objeto decae hacia el equilibrio ambiental (0).
     * La velocidad de disipación depende de la difusividad térmica del material.
     *
     * Fenómeno: AMBIENT_DISSIPATION
     * Propiedad que decae: TEMPERATURE
     * Coeficiente: THERMAL_DIFFUSIVITY
     * Evaluador: AmbientDissipationEvaluator
     */
    public static final PhysicalRelation THERMAL_AMBIENT_DISSIPATION = PhysicalRelation.builder()
        .name("thermal_ambient_dissipation")
        .relationType(RelationType.AMBIENT_DISSIPATION)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.THERMAL_DIFFUSIVITY)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    // ── Relación 6: Disipación eléctrica ambiental ────────────────────────

    /**
     * La carga de cada objeto decae hacia el equilibrio (0).
     * La velocidad de disipación depende de la conductividad eléctrica.
     *
     * Fenómeno: AMBIENT_DISSIPATION
     * Propiedad que decae: CHARGE
     * Coeficiente: ELECTRICAL_CONDUCTIVITY
     * Evaluador: AmbientDissipationEvaluator
     */
    public static final PhysicalRelation ELECTRICAL_DISSIPATION = PhysicalRelation.builder()
        .name("electrical_dissipation")
        .relationType(RelationType.AMBIENT_DISSIPATION)
        .participating(
            CoreProperties.CHARGE,
            CoreProperties.ELECTRICAL_CONDUCTIVITY)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    // ── Relación 7: Disipación fluídica ambiental ─────────────────────────

    /**
     * La humedad de cada objeto decae hacia el equilibrio (0).
     * La velocidad depende del coeficiente de absorción del material.
     *
     * Fenómeno: AMBIENT_DISSIPATION
     * Propiedad que decae: HUMIDITY
     * Coeficiente: HUMIDITY_ABSORPTION
     * Evaluador: AmbientDissipationEvaluator
     */
    public static final PhysicalRelation FLUID_AMBIENT_DISSIPATION = PhysicalRelation.builder()
        .name("fluid_ambient_dissipation")
        .relationType(RelationType.AMBIENT_DISSIPATION)
        .participating(
            CoreProperties.HUMIDITY,
            CoreProperties.HUMIDITY_ABSORPTION)
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    // ── Relación 8: Corrección de exceso térmico ──────────────────────────

    /**
     * Cuando la temperatura supera un umbral crítico (500 unidades), la energía
     * excess se disipa más agresivamente para evitar divergencia numérica.
     *
     * Fenómeno: HOOKE (ley de Hooke — fuerza restauradora elástica)
     * La presión térmica excess se corrige proporcionalmente a la compresibilidad.
     * Evaluador: HookeEvaluator
     * Restricciones: THRESHOLD_ABOVE(TEMPERATURE, 500)
     */
    public static final PhysicalRelation THERMAL_EXCESS_CORRECTION = PhysicalRelation.builder()
        .name("thermal_excess_correction")
        .relationType(RelationType.HOOKE)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.PRESSURE,
            CoreProperties.COMPRESSIBILITY)
        .constraint(RelationConstraint.thresholdAbove(CoreProperties.TEMPERATURE, 500.0))
        .priority(120)
        .build();

    // ── Relación 9: Corrección de exceso eléctrico ────────────────────────

    /**
     * Cuando la carga supera un umbral crítico (10 unidades), el exceso
     * se disipa para evitar divergencia numérica.
     *
     * Fenómeno: HOOKE (fuerza restauradora — corrección de exceso)
     * Evaluador: HookeEvaluator
     * Restricciones: THRESHOLD_ABOVE(CHARGE, 10)
     */
    public static final PhysicalRelation ELECTRICAL_EXCESS_CORRECTION = PhysicalRelation.builder()
        .name("electrical_excess_correction")
        .relationType(RelationType.HOOKE)
        .participating(
            CoreProperties.CHARGE,
            CoreProperties.PRESSURE,
            CoreProperties.COMPRESSIBILITY)
        .constraint(RelationConstraint.thresholdAbove(CoreProperties.CHARGE, 10.0))
        .priority(120)
        .build();

    // ── Relación 10: Liberación en saturación fluídica ────────────────────

    /**
     * Cuando la humedad supera el umbral de saturación (0.6), el exceso
     * se libera progresivamente hacia el ambiente.
     *
     * Fenómeno: FICK (difusión de masa excess hacia el ambiente)
     * Evaluador: FickEvaluator
     * Restricciones: THRESHOLD_ABOVE(HUMIDITY, 0.6)
     */
    public static final PhysicalRelation FLUID_SATURATION_RELEASE = PhysicalRelation.builder()
        .name("fluid_saturation_release")
        .relationType(RelationType.FICK)
        .participating(
            CoreProperties.HUMIDITY,
            CoreProperties.HUMIDITY_ABSORPTION)
        .constraint(RelationConstraint.thresholdAbove(CoreProperties.HUMIDITY, 0.6))
        .priority(115)
        .build();

    // ── Relación 11: Efecto Joule ─────────────────────────────────────────

    /**
     * La corriente eléctrica que fluyó este frame (leída desde FrameMagnitudes.CURRENT,
     * escrita por OhmEvaluator) genera calor proporcional a I².
     *
     * Evaluada después de ELECTRICAL_TRANSFER (prio 100 → 105).
     * No existe ninguna propiedad puente en PhysicalState.
     *
     * Composición vía FrameState:
     *   OHM   (prio 100) → ΔCharge + escribe FrameMagnitudes.CURRENT
     *   JOULE (prio 105) → lee FrameMagnitudes.CURRENT → ΔTemperature
     *
     * Fenómeno: JOULE (Q = I² · R · t)
     * Evaluador: JouleEvaluator
     */
    public static final PhysicalRelation JOULE_HEATING = PhysicalRelation.builder()
        .name("joule_heating")
        .relationType(RelationType.JOULE)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.minDelta(1e-9))
        .priority(105)
        .build();

    // ── Relación 12: Conversión térmica de radiación absorbida ────────────

    /**
     * La radiación absorbida este frame (leída desde FrameMagnitudes.ABSORBED_RADIATION,
     * escrita por PlanckEvaluator) se convierte en calor a través de la
     * capacidad calorífica del material.
     *
     * Evaluada después de las relaciones de tipo PLANCK (prio 110 → 112).
     * No existe ninguna propiedad puente en PhysicalState.
     *
     * Composición vía FrameState:
     *   PLANCK            (prio 110) → ΔRadiation + escribe FrameMagnitudes.ABSORBED_RADIATION
     *   RADIATION_THERMAL (prio 112) → lee FrameMagnitudes.ABSORBED_RADIATION → ΔTemperature
     *
     * Fenómeno: RADIATION_THERMAL (conversión de radiación en calor)
     * Evaluador: RadiationThermalEvaluator
     */
    public static final PhysicalRelation RADIATION_THERMAL_CONVERSION = PhysicalRelation.builder()
        .name("radiation_thermal_conversion")
        .relationType(RelationType.RADIATION_THERMAL)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.minDelta(1e-9))
        .priority(112)
        .build();

    // ── Colección completa ────────────────────────────────────────────────

    /**
     * Todas las relaciones físicas fundamentales del Core.
     *
     * @return array con las 12 relaciones físicas fundamentales.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            VOLUMETRIC_EXPANSION,
            THERMAL_CONDUCTION,
            ELECTRICAL_TRANSFER,
            FLUID_DIFFUSION,
            THERMAL_AMBIENT_DISSIPATION,
            ELECTRICAL_DISSIPATION,
            FLUID_AMBIENT_DISSIPATION,
            THERMAL_EXCESS_CORRECTION,
            ELECTRICAL_EXCESS_CORRECTION,
            FLUID_SATURATION_RELEASE,
            JOULE_HEATING,
            RADIATION_THERMAL_CONVERSION
        };
    }
}
