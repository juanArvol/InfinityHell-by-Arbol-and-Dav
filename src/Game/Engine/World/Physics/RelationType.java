package Game.Engine.World.Physics;

/**
 * Identifica qué fenómeno físico describe una PhysicalRelation.
 *
 * ── HRFC-022 — Eliminación del Paradigma de Ley Ejecutable ───────────────
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * RelationType no representa una fórmula matemática.
 * RelationType no representa una expresión.
 * RelationType no representa una función.
 * RelationType no representa un algoritmo.
 *
 * RelationType únicamente identifica qué fenómeno físico está siendo
 * descrito por la PhysicalRelation que lo porta.
 *
 * El procedimiento matemático correspondiente a cada tipo vive exclusivamente
 * en el evaluador especializado del sistema de resolución:
 *
 *   FOURIER       → FourierEvaluator
 *   OHM           → OhmEvaluator
 *   PASCAL        → PascalEvaluator
 *   BERNOULLI     → BernoulliEvaluator
 *   NEWTON        → NewtonEvaluator
 *   HOOKE         → HookeEvaluator
 *   ARCHIMEDES    → ArchimedesEvaluator
 *   STOKES        → StokesEvaluator
 *   FICK          → FickEvaluator
 *   SCHWARZSCHILD → SchwarzschildEvaluator
 *   PLANCK        → PlanckEvaluator
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir un nuevo fenómeno físico:
 *   1. Añadir una constante aquí.
 *   2. Crear el evaluador correspondiente en Game.Engine.World.Solver.
 *   3. Registrarlo en EvaluatorRegistry.
 *
 * No se modifica ningún otro componente del Core.
 */
public enum RelationType {

    /**
     * Ley de Fourier — transferencia de calor por conducción.
     * q = −k · ∇T
     * Propiedades participantes: TEMPERATURE, THERMAL_CONDUCTIVITY, HEAT_CAPACITY
     */
    FOURIER,

    /**
     * Ley de Ohm — relación entre tensión, corriente y resistencia.
     * I = V / R  →  equivalente a transferencia de carga proporcional a
     * la diferencia de potencial y a la conductividad eléctrica.
     * Propiedades participantes: CHARGE, ELECTRICAL_CONDUCTIVITY
     */
    OHM,

    /**
     * Ley de Pascal — propagación de presión en fluidos incompresibles.
     * ΔP = ρ · g · Δh  →  aquí: presión propagada por diferencia de carga
     * y compresibilidad.
     * Propiedades participantes: PRESSURE, COMPRESSIBILITY, TEMPERATURE
     */
    PASCAL,

    /**
     * Principio de Bernoulli — conservación de energía en fluidos.
     * P + ½ρv² + ρgh = cte
     * Propiedades participantes: PRESSURE, VISCOSITY, HUMIDITY
     */
    BERNOULLI,

    /**
     * Segunda ley de Newton — dinámica de partículas bajo fuerzas externas.
     * F = m · a
     * Propiedades participantes: VELOCITY_X, VELOCITY_Y, MASS
     */
    NEWTON,

    /**
     * Ley de Hooke — deformación elástica proporcional a la fuerza aplicada.
     * F = −k · x
     * Propiedades participantes: PRESSURE, COMPRESSIBILITY
     */
    HOOKE,

    /**
     * Principio de Arquímedes — fuerza de empuje en fluidos.
     * F_b = ρ_fluido · V_sumergido · g
     * Propiedades participantes: HUMIDITY, VISCOSITY, MASS, VELOCITY_Y
     */
    ARCHIMEDES,

    /**
     * Ley de Stokes — resistencia viscosa a objetos en movimiento.
     * F_d = 6π · η · r · v
     * Propiedades participantes: VISCOSITY, VELOCITY_X, VELOCITY_Y
     */
    STOKES,

    /**
     * Primera ley de Fick — difusión de materia por gradiente de concentración.
     * J = −D · ∇C
     * Propiedades participantes: HUMIDITY, HUMIDITY_ABSORPTION
     */
    FICK,

    /**
     * Métrica de Schwarzschild — curvatura espacio-temporal por masa.
     * r_s = 2GM / c²  →  aquí: atracción gravitacional extrema y
     * horizonte de eventos.
     * Propiedades participantes: MASS, SCHWARZSCHILD_RADIUS, VELOCITY_X, VELOCITY_Y
     */
    SCHWARZSCHILD,

    /**
     * Ley de Planck — radiación de cuerpo negro y emisión cuántica.
     * B(λ,T) = (2hc²/λ⁵) / (e^(hc/λkT) − 1)
     * Simplificado como transferencia de radiación proporcional a temperatura.
     * Propiedades participantes: RADIATION_LEVEL, RADIATION_ABSORPTION, TEMPERATURE
     */
    PLANCK,

    /**
     * Horizonte de eventos — absorción total al cruzar el radio de Schwarzschild.
     * Cuando un objeto cruza el horizonte de eventos de un cuerpo masivo,
     * toda su velocidad es absorbida instantáneamente.
     * Propiedades participantes: MASS, SCHWARZSCHILD_RADIUS, VELOCITY_X, VELOCITY_Y
     */
    EVENT_HORIZON,

    /**
     * Efecto Joule — disipación de energía eléctrica como calor.
     * P = I² · R  →  Q = I² · R · t
     * Lee la corriente I desde FrameState (clave "current"),
     * calculada previamente por OhmEvaluator.
     * Produce ΔTemperature sobre las entidades con corriente en el frame.
     * Propiedades participantes: TEMPERATURE, HEAT_CAPACITY
     */
    JOULE,

    /**
     * Conversión térmica de radiación absorbida.
     * Q = R_absorbida · factor / C
     * Lee la radiación absorbida desde FrameState (clave "absorbed_radiation"),
     * calculada previamente por PlanckEvaluator.
     * Produce ΔTemperature sobre las entidades receptoras.
     * Propiedades participantes: TEMPERATURE, HEAT_CAPACITY
     */
    RADIATION_THERMAL,

    /**
     * Disipación ambiental — decaimiento de una propiedad extensiva hacia el
     * equilibrio ambiental (valor 0) a velocidad proporcional al coeficiente
     * de disipación del material. Aplicable a temperatura, carga, humedad, etc.
     * Propiedades participantes: prop_a_disipar, coeficiente_de_disipacion
     */
    AMBIENT_DISSIPATION
}
