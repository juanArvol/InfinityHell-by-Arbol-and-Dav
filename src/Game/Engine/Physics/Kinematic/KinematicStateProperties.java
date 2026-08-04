package Game.Engine.Physics.Kinematic;

import Game.Engine.Physics.Core.PropertyDescriptor;

/**
 * Catálogo de PropertyDescriptors para las magnitudes cinemáticas derivadas.
 *
 * ── HRFC-030 — Integración entre Kinematic Physics y World Physics ────────
 *
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * KinematicStateProperties es el catálogo de acceso al estado cinemático
 * dentro del PhysicalState de una entidad.
 *
 * Estas propiedades son escritas exclusivamente por KinematicPhysicsInterpreter,
 * después de cada paso de Kinematic Physics y antes de que PhysicsCoordinator
 * evalúe las relaciones del mundo.
 *
 * World Physics las consume como cualquier otra propiedad del PhysicalState:
 * leyendo su valor mediante view.get(KinematicStateProperties.SPEED) en
 * evaluadores especializados.
 *
 * ── PRINCIPIO FUNDAMENTAL ─────────────────────────────────────────────────
 * Kinematic Physics produce estas magnitudes. World Physics las consume.
 * Ninguno de los dos sistemas conoce al otro. Solo comparten este catálogo.
 *
 * ── PROPIEDADES INCLUIDAS ────────────────────────────────────────────────
 *
 *   SPEED             → módulo de la velocidad actual (|v|), en u/s.
 *   VELOCITY_X_SIGNED → componente X de velocidad, con signo, en u/s.
 *   VELOCITY_Y_SIGNED → componente Y de velocidad, con signo, en u/s.
 *   ACCELERATION      → cambio de velocidad en el frame (|v|-|v₀|)/dt, en u/s².
 *   MOMENTUM          → cantidad de movimiento (mass × |v|), en kg·u/s.
 *   KINETIC_ENERGY    → energía cinética actual (½ × mass × v²).
 *   DELTA_KINETIC_ENERGY → cambio de KE este frame. Negativo = disipación.
 *   FRICTION_FACTOR   → coeficiente de fricción de la superficie actual.
 *   ON_GROUND         → 1.0 si está en suelo, 0.0 si está en el aire.
 *
 * ── CONSUMIDORES CORRECTOS ────────────────────────────────────────────────
 *   view.get(KinematicStateProperties.SPEED)
 *   view.get(KinematicStateProperties.KINETIC_ENERGY)
 *   view.get(KinematicStateProperties.FRICTION_FACTOR)
 *   view.get(KinematicStateProperties.DELTA_KINETIC_ENERGY)
 *
 * ── EJEMPLO DE RELACIÓN EMERGENTE ────────────────────────────────────────
 *
 *   Fricción → calor:
 *     Q ≈ μ × N × Δx ≈ frictionFactor × mass × |Δv|
 *     → EvaluatorRegistry.FRICTION_THERMAL consume FRICTION_FACTOR y SPEED
 *       para producir un delta de TEMPERATURE.
 *
 *   Impacto → deformación:
 *     Impulso = |ΔMomentum| = mass × |Δv|
 *     → EvaluatorRegistry.KINETIC_DISSIPATION consume DELTA_KINETIC_ENERGY
 *       para producir un delta de PRESSURE o DEFORMATION_ENERGY.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Añadir una nueva magnitud:
 *   1. Crear un PropertyDescriptor aquí con un id único.
 *   2. Añadir el campo en KinematicState y calcularlo en KinematicState.from().
 *   3. KinematicPhysicsInterpreter la escribe al PhysicalState.
 *   4. Crear un evaluador que la consuma y registrarlo en EvaluatorRegistry.
 *
 *   Ningún sistema existente cambia.
 *
 * ── INVARIANTE ────────────────────────────────────────────────────────────
 *   ✗ No contiene lógica de simulación.
 *   ✗ No referencia Physics2D.
 *   ✗ No referencia evaluadores concretos.
 *   ✓ Es un catálogo estático de PropertyDescriptors.
 *   ✓ Las propiedades son escritas por el Interpreter, leídas por evaluadores.
 */
public final class KinematicStateProperties {

    private KinematicStateProperties() {}

    // ── Velocidad ─────────────────────────────────────────────────────────

    /**
     * Módulo de la velocidad actual en u/s.
     * Siempre ≥ 0. Producido cada frame por KinematicPhysicsInterpreter.
     *
     * Consumidores relevantes:
     *   → FrictionThermalEvaluator  (calor por fricción)
     *   → KineticDissipationEvaluator (disipación de energía)
     *   → BernoulliEvaluator          (presión dinámica en fluidos)
     */
    public static final PropertyDescriptor SPEED =
        new PropertyDescriptor("kinematic_speed", 0.0,
            0.0, Double.POSITIVE_INFINITY, false,
            "Módulo de la velocidad actual de la entidad (|v|) en u/s");

    /**
     * Componente X de velocidad, con signo, en u/s.
     * Positivo = derecha. Negativo = izquierda.
     */
    public static final PropertyDescriptor VELOCITY_X_SIGNED =
        new PropertyDescriptor("kinematic_velocity_x", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Componente X de la velocidad con signo, en u/s");

    /**
     * Componente Y de velocidad, con signo, en u/s.
     * Positivo = abajo (sistema coordenado AWT). Negativo = arriba.
     */
    public static final PropertyDescriptor VELOCITY_Y_SIGNED =
        new PropertyDescriptor("kinematic_velocity_y", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Componente Y de la velocidad con signo, en u/s");

    // ── Dinámica ──────────────────────────────────────────────────────────

    /**
     * Aceleración estimada para este frame, en u/s².
     * (|v_actual| - |v_anterior|) / deltaTime.
     * Positiva = aceleró. Negativa = desaceleró (frenado, impacto, fricción).
     *
     * Consumidores relevantes:
     *   → KineticDissipationEvaluator (fuerte desaceleración → disipación)
     */
    public static final PropertyDescriptor ACCELERATION =
        new PropertyDescriptor("kinematic_acceleration", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Aceleración estimada del frame actual (Δv/Δt) en u/s²");

    /**
     * Cantidad de movimiento actual: mass × |v|, en kg·u/s.
     * Relevante para calcular transferencia de energía en impactos.
     *
     * Consumidores relevantes:
     *   → KineticDissipationEvaluator (impulso = |ΔMomentum|)
     */
    public static final PropertyDescriptor MOMENTUM =
        new PropertyDescriptor("kinematic_momentum", 0.0,
            0.0, Double.POSITIVE_INFINITY, false,
            "Cantidad de movimiento (mass × |v|) en kg·u/s");

    // ── Energía ───────────────────────────────────────────────────────────

    /**
     * Energía cinética actual: ½ × mass × v², en unidades relativas.
     * Siempre ≥ 0.
     *
     * Consumidores relevantes:
     *   → KineticDissipationEvaluator (energía total disponible para disipación)
     */
    public static final PropertyDescriptor KINETIC_ENERGY =
        new PropertyDescriptor("kinematic_kinetic_energy", 0.0,
            0.0, Double.POSITIVE_INFINITY, false,
            "Energía cinética actual ½·m·v² en unidades relativas");

    /**
     * Cambio de energía cinética en este frame.
     *
     *   Negativo → el objeto perdió energía (frenado, impacto, fricción).
     *              World Physics produce calor, presión, deformación.
     *   Positivo → el objeto ganó energía (aceleración, impulso).
     *
     * Esta es la propiedad más importante para la producción de fenómenos:
     * cualquier proceso que disipe energía cinética puede generar consecuencias
     * físicas reales a través de las relaciones del mundo.
     *
     * Consumidores relevantes:
     *   → KineticDissipationEvaluator (convierte -ΔKE en ΔTemperature, ΔPressure)
     *   → FrictionThermalEvaluator    (combina con FRICTION_FACTOR)
     */
    public static final PropertyDescriptor DELTA_KINETIC_ENERGY =
        new PropertyDescriptor("kinematic_delta_ke", 0.0,
            Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false,
            "Cambio de energía cinética este frame (KE_actual - KE_anterior)");

    // ── Superficie y fricción ─────────────────────────────────────────────

    /**
     * Coeficiente de fricción de la superficie de contacto actual.
     * 0.0 en el aire o sin superficie. Derivado de SurfaceMaterial.getFriction().
     *
     * Consumidores relevantes:
     *   → FrictionThermalEvaluator (Q ≈ μ × |v| × mass × dt → ΔTemperature)
     */
    public static final PropertyDescriptor FRICTION_FACTOR =
        new PropertyDescriptor("kinematic_friction_factor", 0.0,
            0.0, Double.POSITIVE_INFINITY, false,
            "Coeficiente de fricción de la superficie de contacto actual");

    /**
     * Indicador de contacto con el suelo.
     * 1.0 = en suelo. 0.0 = en el aire o sin contacto.
     *
     * Representa la componente binaria de ContactInformation del pipeline.
     * Los evaluadores pueden usarlo para condicionarse: solo hay fricción
     * cuando ON_GROUND = 1.0.
     */
    public static final PropertyDescriptor ON_GROUND =
        new PropertyDescriptor("kinematic_on_ground", 0.0,
            0.0, 1.0, true,
            "1.0 si la entidad está en contacto con el suelo, 0.0 si está en el aire");
}
