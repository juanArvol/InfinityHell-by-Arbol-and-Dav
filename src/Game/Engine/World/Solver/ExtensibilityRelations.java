package Game.Engine.World.Solver;

import Game.Engine.World.Physics.CoreProperties;
import Game.Engine.World.Physics.PhysicalRelation;
import Game.Engine.World.Physics.PropertyDescriptor;
import Game.Engine.World.Physics.RelationConstraint;
import Game.Engine.World.Physics.RelationType;

/**
 * Verificación de extensibilidad del World Simulation Core — HRFC-022.
 *
 * ── PROPÓSITO ─────────────────────────────────────────────────────────────
 * Este archivo demuestra que el Core cumple el invariante de HRFC-022:
 *
 *   Cualquier fenómeno físico nuevo se implementa creando nuevas propiedades
 *   y nuevas PhysicalRelation declarativas, registrando sus dependencias en
 *   el PropertyDependencyGraph.
 *   Nunca modificando el Coordinator, el Solver, los evaluadores existentes
 *   ni ningún otro componente del Core.
 *
 * ── REEMPLAZA A ExtensibilityLaws ────────────────────────────────────────
 * ExtensibilityLaws contenía PhysicsLaw con lambdas solve(WorldContext).
 * ExtensibilityRelations contiene PhysicalRelation puramente declarativas.
 * No existe ningún algoritmo, callback ni referencia al mundo aquí.
 *
 * ── NUEVAS PROPIEDADES ────────────────────────────────────────────────────
 * Los fenómenos nuevos solo requieren PropertyDescriptors nuevos.
 * No requieren nuevos tipos. No requieren nuevas clases base.
 * No requieren modificar CoreProperties.
 */
public final class ExtensibilityRelations {

    private ExtensibilityRelations() {}

    // ══════════════════════════════════════════════════════════════════════
    // NUEVAS PROPIEDADES
    // En un proyecto real vivirían en catálogos propios (GameplayProperties,
    // PlanetProperties, MagicProperties, etc.)
    // ══════════════════════════════════════════════════════════════════════

    /** Componente Y de velocidad. Afectada por gravedad. */
    public static final PropertyDescriptor VELOCITY_Y =
        PropertyDescriptor.of("velocity_y", 0.0, "Velocidad vertical en unidades/s");

    /** Componente X de velocidad. */
    public static final PropertyDescriptor VELOCITY_X =
        PropertyDescriptor.of("velocity_x", 0.0, "Velocidad horizontal en unidades/s");

    /** Masa del objeto en unidades del juego. */
    public static final PropertyDescriptor MASS =
        PropertyDescriptor.ofPositive("mass", 1.0, "Masa del objeto en kg relativos");

    /** Intensidad de campo magnético local. */
    public static final PropertyDescriptor MAGNETIC_FIELD =
        PropertyDescriptor.of("magnetic_field", 0.0, "Intensidad de campo magnético");

    /** Nivel de radiación acumulada. */
    public static final PropertyDescriptor RADIATION_LEVEL =
        PropertyDescriptor.ofPositive("radiation_level", 0.0,
            "Nivel de radiación ionizante acumulada");

    /** Coeficiente de absorción de radiación del material [0,1]. */
    public static final PropertyDescriptor RADIATION_ABSORPTION =
        PropertyDescriptor.ofBounded("radiation_absorption", 0.1, 0.0, 1.0,
            "Fracción de radiación que el material absorbe por frame");

    /** Temperatura crítica de superconductividad. */
    public static final PropertyDescriptor SUPERCONDUCTIVITY_THRESHOLD =
        PropertyDescriptor.of("superconductivity_threshold", Double.POSITIVE_INFINITY,
            "Temperatura por debajo de la cual el material es superconductor");

    /** Concentración de cristales precipitados [0,1]. */
    public static final PropertyDescriptor CRYSTAL_CONCENTRATION =
        PropertyDescriptor.ofBounded("crystal_concentration", 0.0, 0.0, 1.0,
            "Fracción de masa cristalizada");

    /** Tasa de cristalización del material [0,1]. */
    public static final PropertyDescriptor CRYSTALLIZATION_RATE =
        PropertyDescriptor.ofBounded("crystallization_rate", 0.0, 0.0, 1.0,
            "Velocidad de precipitación de cristales del material");

    /** Estado de plasma: 0 = normal, 1 = plasma total. */
    public static final PropertyDescriptor PLASMA_STATE =
        PropertyDescriptor.ofBounded("plasma_state", 0.0, 0.0, 1.0,
            "Fracción de ionización de plasma [0=normal, 1=plasma puro]");

    /** Temperatura de transición a plasma del material. */
    public static final PropertyDescriptor PLASMA_THRESHOLD =
        PropertyDescriptor.of("plasma_threshold", Double.POSITIVE_INFINITY,
            "Temperatura en la que el material alcanza estado de plasma");

    /** Tensión superficial del material líquido. */
    public static final PropertyDescriptor SURFACE_TENSION =
        PropertyDescriptor.ofPositive("surface_tension", 0.0,
            "Tensión superficial del material en estado líquido");

    /** Radio de Schwarzschild efectivo (en unidades del mundo). */
    public static final PropertyDescriptor SCHWARZSCHILD_RADIUS =
        PropertyDescriptor.ofPositive("schwarzschild_radius", 0.0,
            "Radio de Schwarzschild derivado de la masa del objeto");

    /** Espín cuántico del objeto. */
    public static final PropertyDescriptor QUANTUM_SPIN =
        PropertyDescriptor.of("quantum_spin", 0.0,
            "Número cuántico de espín del objeto");

    /** Función de onda: amplitud de probabilidad cuántica. */
    public static final PropertyDescriptor WAVE_FUNCTION =
        PropertyDescriptor.ofBounded("wave_function", 1.0, 0.0, 1.0,
            "Amplitud de la función de onda cuántica [0=colapsada, 1=superposición]");

    // ══════════════════════════════════════════════════════════════════════
    // GRAVEDAD
    // Fenómeno: aceleración vertical constante sobre objetos con velocity_y.
    // Tipo: NEWTON — F = m·a → Δv = g·dt
    // Evaluador: NewtonEvaluator
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Gravedad uniforme hacia abajo.
     * La restricción THRESHOLD_ABOVE con valor 9.8 codifica la aceleración
     * gravitacional que NewtonEvaluator aplica a las propiedades participantes.
     */
    public static final PhysicalRelation GRAVITY = PhysicalRelation.builder()
        .name("gravity")
        .relationType(RelationType.NEWTON)
        .participating(VELOCITY_Y)
        .constraint(RelationConstraint.thresholdAbove(VELOCITY_Y, 9.8))
        .priority(1)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // MAGNETISMO
    // Fenómeno: fuerza entre dipolos magnéticos dentro de un radio de acción.
    // Tipo: OHM — transferencia de campo proporcional a la diferencia de potencial
    // Evaluador: OhmEvaluator (los campos magnéticos se modelan como diferencia
    //            de potencial magnético entre pares)
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Fuerza magnética entre pares dentro de radio 128.
     * Cargas del mismo signo se repelen; signos opuestos se atraen.
     */
    public static final PhysicalRelation MAGNETISM = PhysicalRelation.builder()
        .name("magnetism")
        .relationType(RelationType.OHM)
        .participating(MAGNETIC_FIELD, VELOCITY_X, VELOCITY_Y)
        .constraint(RelationConstraint.maxDistance(128.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(80)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // RADIACIÓN
    // Fenómeno 1: transferencia de energía radiante entre pares.
    // Tipo: PLANCK — ley de radiación de cuerpo negro
    // Evaluador: PlanckEvaluator
    // PlanckEvaluator escribe FrameMagnitudes.ABSORBED_RADIATION en el receptor.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Transferencia de radiación entre pares dentro del radio de emisión.
     * PlanckEvaluator escribe la radiación absorbida en FrameState
     * para que RADIATION_THERMAL la convierta en calor en el mismo frame.
     * No existe ninguna propiedad puente en PhysicalState.
     */
    public static final PhysicalRelation RADIATION = PhysicalRelation.builder()
        .name("radiation")
        .relationType(RelationType.PLANCK)
        .participating(
            RADIATION_LEVEL,
            RADIATION_ABSORPTION)
        .constraint(RelationConstraint.maxDistance(96.0))
        .constraint(RelationConstraint.minDelta(1e-6))
        .priority(110)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // CONVERSIÓN TÉRMICA DE RADIACIÓN ABSORBIDA
    // Fenómeno 2: la radiación absorbida genera calor.
    // Tipo: RADIATION_THERMAL
    // Evaluador: RadiationThermalEvaluator
    // Lee FrameMagnitudes.ABSORBED_RADIATION escrito por PlanckEvaluator.
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Convierte la radiación absorbida este frame (FrameMagnitudes.ABSORBED_RADIATION)
     * en calor a través de la capacidad calorífica del material.
     * Evaluada después de RADIATION (prio 110 → 112).
     */
    public static final PhysicalRelation RADIATION_THERMAL = PhysicalRelation.builder()
        .name("radiation_thermal")
        .relationType(RelationType.RADIATION_THERMAL)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.HEAT_CAPACITY)
        .constraint(RelationConstraint.minDelta(1e-9))
        .priority(112)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // SUPERCONDUCTIVIDAD
    // Fenómeno: resistencia eléctrica cero por debajo de temperatura crítica.
    // Tipo: OHM — corrección de la disipación cuando T < umbral
    // Evaluador: OhmEvaluator (con restricción THRESHOLD_BELOW activa)
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Cuando la temperatura cae por debajo del umbral de superconductividad,
     * la disipación eléctrica queda cancelada (resistencia = 0).
     */
    public static final PhysicalRelation SUPERCONDUCTIVITY = PhysicalRelation.builder()
        .name("superconductivity")
        .relationType(RelationType.OHM)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.CHARGE,
            CoreProperties.ELECTRICAL_CONDUCTIVITY,
            SUPERCONDUCTIVITY_THRESHOLD)
        .constraint(RelationConstraint.thresholdBelow(CoreProperties.TEMPERATURE, 0.0))
        .constraint(RelationConstraint.propertyPresent(SUPERCONDUCTIVITY_THRESHOLD))
        .priority(60)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // CRISTALIZACIÓN
    // Fenómeno: precipitación de sólidos cuando temperatura baja y humedad alta.
    // Tipo: FICK — difusión de masa (humedad → cristal)
    // Evaluador: FickEvaluator (con restricción THRESHOLD_BELOW(T, 0))
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Cuando la temperatura es negativa y hay humedad suficiente,
     * la humedad se convierte en masa cristalizada liberando calor latente.
     */
    public static final PhysicalRelation CRYSTALLIZATION = PhysicalRelation.builder()
        .name("crystallization")
        .relationType(RelationType.FICK)
        .participating(
            CoreProperties.TEMPERATURE,
            CoreProperties.HUMIDITY,
            CRYSTALLIZATION_RATE,
            CRYSTAL_CONCENTRATION)
        .constraint(RelationConstraint.thresholdBelow(CoreProperties.TEMPERATURE, 0.0))
        .constraint(RelationConstraint.thresholdAbove(CoreProperties.HUMIDITY, 0.1))
        .priority(40)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // PLASMA
    // Fenómeno: ionización del material a temperaturas extremas.
    // Tipo: PLANCK — radiación de plasma y transición de estado
    // Evaluador: PlanckEvaluator (con restricción THRESHOLD_ABOVE activa)
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Cuando la temperatura supera el umbral del material, el estado de plasma
     * aumenta progresivamente amplificando conductividad y emitiendo radiación.
     */
    public static final PhysicalRelation PLASMA_TRANSITION = PhysicalRelation.builder()
        .name("plasma_transition")
        .relationType(RelationType.PLANCK)
        .participating(
            CoreProperties.TEMPERATURE,
            PLASMA_THRESHOLD,
            PLASMA_STATE,
            CoreProperties.ELECTRICAL_CONDUCTIVITY,
            RADIATION_LEVEL)
        .constraint(RelationConstraint.propertyPresent(PLASMA_THRESHOLD))
        .constraint(RelationConstraint.propertyPresent(PLASMA_STATE))
        .priority(35)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // TENSIÓN SUPERFICIAL
    // Fenómeno: cohesión entre objetos líquidos adyacentes.
    // Tipo: STOKES — resistencia viscosa y cohesión entre partículas fluidas
    // Evaluador: StokesEvaluator
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Los objetos líquidos se atraen mutuamente cuando están muy próximos.
     * Solo actúa a distancias muy cortas (≤ 8 unidades).
     */
    public static final PhysicalRelation SURFACE_TENSION_RELATION = PhysicalRelation.builder()
        .name("surface_tension")
        .relationType(RelationType.STOKES)
        .participating(
            SURFACE_TENSION,
            CoreProperties.VISCOSITY,
            VELOCITY_X,
            VELOCITY_Y)
        .constraint(RelationConstraint.maxDistance(8.0))
        .constraint(RelationConstraint.propertyPresent(SURFACE_TENSION))
        .priority(90)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // AGUJERO NEGRO — ATRACCIÓN GRAVITACIONAL
    // Fenómeno: atracción gravitacional entre cuerpos masivos.
    // Tipo: SCHWARZSCHILD — métrica de Schwarzschild (solo atracción)
    // Evaluador: SchwarzschildEvaluator
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Atracción gravitacional entre cuerpos masivos.
     * F = G·m_a·m_b / d²
     */
    public static final PhysicalRelation BLACK_HOLE_GRAVITY = PhysicalRelation.builder()
        .name("black_hole_gravity")
        .relationType(RelationType.SCHWARZSCHILD)
        .participating(MASS, VELOCITY_X, VELOCITY_Y)
        .constraint(RelationConstraint.propertyPresent(MASS))
        .priority(2)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // AGUJERO NEGRO — HORIZONTE DE EVENTOS
    // Fenómeno: absorción de velocidad al cruzar el radio de Schwarzschild.
    // Tipo: EVENT_HORIZON
    // Evaluador: EventHorizonEvaluator
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Absorción total de velocidad cuando un objeto cruza el horizonte.
     * Evaluada después de la atracción gravitacional (prio 2 → 3).
     */
    public static final PhysicalRelation BLACK_HOLE_HORIZON = PhysicalRelation.builder()
        .name("black_hole_horizon")
        .relationType(RelationType.EVENT_HORIZON)
        .participating(MASS, SCHWARZSCHILD_RADIUS, VELOCITY_X, VELOCITY_Y)
        .constraint(RelationConstraint.propertyPresent(MASS))
        .constraint(RelationConstraint.propertyPresent(SCHWARZSCHILD_RADIUS))
        .priority(3)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // EFECTOS CUÁNTICOS — COLAPSO DE FUNCIÓN DE ONDA
    // Fenómeno: superposición cuántica y colapso por proximidad.
    // Tipo: PLANCK — emisión/colapso cuántico
    // Evaluador: PlanckEvaluator (con restricción MAX_DISTANCE activa)
    // Archivos del Core modificados: NINGUNO.
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Cuando dos objetos con función de onda activa se acercan lo suficiente,
     * la función de onda de ambos colapsa progresivamente hacia 0.
     */
    public static final PhysicalRelation QUANTUM_WAVE_COLLAPSE = PhysicalRelation.builder()
        .name("quantum_wave_collapse")
        .relationType(RelationType.PLANCK)
        .participating(WAVE_FUNCTION, QUANTUM_SPIN)
        .constraint(RelationConstraint.maxDistance(16.0))
        .constraint(RelationConstraint.propertyPresent(WAVE_FUNCTION))
        .priority(200)
        .build();

    // ══════════════════════════════════════════════════════════════════════
    // COLECCIÓN COMPLETA
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Todas las relaciones de extensibilidad.
     *
     * @return array con las 11 relaciones de extensibilidad.
     */
    public static PhysicalRelation[] all() {
        return new PhysicalRelation[] {
            GRAVITY,
            MAGNETISM,
            RADIATION,
            RADIATION_THERMAL,
            SUPERCONDUCTIVITY,
            CRYSTALLIZATION,
            PLASMA_TRANSITION,
            SURFACE_TENSION_RELATION,
            BLACK_HOLE_GRAVITY,
            BLACK_HOLE_HORIZON,
            QUANTUM_WAVE_COLLAPSE
        };
    }
}
