package Game.Engine.Physics.Environment;

/**
 * Ambiente de vacío: espacio exterior, microgravedad.
 *
 * ── HRFC-FASE2 — Declarative Environment Ownership ───────────────────────
 *
 * ── CONDICIONES DE REFERENCIA ────────────────────────────────────────────
 * Este ambiente declara las condiciones del vacío espacial:
 *
 *   Térmicas:
 *     - Temperatura ambiente: -270°C relativo (espacio profundo)
 *
 *   Atmosféricas:
 *     - Presión atmosférica: 0.0 (vacío perfecto)
 *     - Humedad relativa: 0.0 (sin fluido)
 *     - Sin viento (0, 0)
 *     - Densidad del fluido: 0.0 (vacío)
 *     - Viscosidad: 0.0 (sin fluido)
 *
 *   Influencia Gravitacional:
 *     - Factor de influencia: 0.0 (anula gravedad de entidades)
 *     - Microgravedad: las entidades NO experimentan su gravedad propia
 *
 *   Campos externos:
 *     - Sin campos eléctricos (0, 0)
 *     - Sin campo magnético (0)
 *
 *   Radiación e iluminación:
 *     - Sin radiación ambiental: 0.0
 *     - Sin iluminación: 0.0 (oscuridad total)
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 * Estos valores pertenecen a VacuumEnvironment, NO a la infraestructura.
 * Son valores de referencia de este ambiente específico.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Este ambiente es completamente estático. current() retorna siempre
 * la misma instancia cacheada de EnvironmentState.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   Environment env = VacuumEnvironment.INSTANCE;
 *   // o
 *   Environment env = VacuumEnvironment.create();
 *
 *   SimulationContext ctx = SimulationContext.builder(physical)
 *       .environment(env)
 *       .build();
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class VacuumEnvironment implements Environment {

    // ── Condiciones de referencia de este ambiente ────────────────────────

    private static final double AMBIENT_TEMPERATURE      = -270.0; // espacio profundo
    private static final double ATMOSPHERIC_PRESSURE     = 0.0;    // vacío
    private static final double AMBIENT_HUMIDITY         = 0.0;    // sin fluido
    private static final double WIND_X                   = 0.0;    // sin fluido
    private static final double WIND_Y                   = 0.0;    // sin fluido
    private static final double FLUID_DENSITY            = 0.0;    // vacío
    private static final double FLUID_VISCOSITY          = 0.0;    // sin fluido
    private static final double GRAVITY_INFLUENCE_X      = 0.0;    // anula gravedad X
    private static final double GRAVITY_INFLUENCE_Y      = 0.0;    // anula gravedad Y (microgravedad)
    private static final double ELECTRIC_FIELD_X         = 0.0;    // sin campo
    private static final double ELECTRIC_FIELD_Y         = 0.0;    // sin campo
    private static final double MAGNETIC_FIELD_Z         = 0.0;    // sin campo
    private static final double AMBIENT_RADIATION        = 0.0;    // sin radiación
    private static final double ILLUMINANCE              = 0.0;    // oscuridad

    // ── Estado cacheado ───────────────────────────────────────────────────

    /**
     * Estado ambiental inmutable de este ambiente.
     * Construido una sola vez y reutilizado en todas las consultas.
     */
    private static final EnvironmentState STATE = EnvironmentState.builder()
        .ambientTemperature(AMBIENT_TEMPERATURE)
        .atmosphericPressure(ATMOSPHERIC_PRESSURE)
        .ambientHumidity(AMBIENT_HUMIDITY)
        .windX(WIND_X)
        .windY(WIND_Y)
        .fluidDensity(FLUID_DENSITY)
        .fluidViscosity(FLUID_VISCOSITY)
        .gravityInfluenceX(GRAVITY_INFLUENCE_X)
        .gravityInfluenceY(GRAVITY_INFLUENCE_Y)
        .electricFieldX(ELECTRIC_FIELD_X)
        .electricFieldY(ELECTRIC_FIELD_Y)
        .magneticFieldZ(MAGNETIC_FIELD_Z)
        .ambientRadiation(AMBIENT_RADIATION)
        .illuminance(ILLUMINANCE)
        .build();

    // ── Singleton ─────────────────────────────────────────────────────────

    /** Instancia única de VacuumEnvironment. */
    public static final VacuumEnvironment INSTANCE = new VacuumEnvironment();

    /** Factory para consistencia con otros ambientes. */
    public static VacuumEnvironment create() {
        return INSTANCE;
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private VacuumEnvironment() {}

    // ── Environment interface ─────────────────────────────────────────────

    @Override
    public EnvironmentState current() {
        return STATE;
    }

    @Override
    public String getName() {
        return "VacuumEnvironment";
    }

    @Override
    public String toString() {
        return "VacuumEnvironment[temp=" + AMBIENT_TEMPERATURE
            + ", pressure=" + ATMOSPHERIC_PRESSURE
            + ", gravityInfluence=" + GRAVITY_INFLUENCE_Y + "]";
    }
}
