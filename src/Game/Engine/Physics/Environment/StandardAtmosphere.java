package Game.Engine.Physics.Environment;

/**
 * Ambiente estándar: atmósfera terrestre en condiciones normales.
 *
 * ── HRFC-FASE2 — Declarative Environment Ownership ───────────────────────
 *
 * ── CONDICIONES DE REFERENCIA ────────────────────────────────────────────
 * Este ambiente declara las condiciones típicas de una atmósfera terrestre
 * estable a nivel del mar en un día promedio:
 *
 *   Térmicas:
 *     - Temperatura ambiente: 20°C relativo (0 en escala del juego)
 *
 *   Atmosféricas:
 *     - Presión atmosférica: 1.0 (1 atm normalizado)
 *     - Humedad relativa: 60% (0.6)
 *     - Sin viento (0, 0)
 *     - Densidad del aire: 1.2 kg/m³ relativo
 *     - Viscosidad despreciable: 0.0
 *
 *   Influencia Gravitacional:
 *     - Factor de influencia: 1.0 (sin modificación, gravedad normal)
 *     - Las entidades experimentan su gravedad propia sin alteración
 *
 *   Campos externos:
 *     - Sin campos eléctricos (0, 0)
 *     - Sin campo magnético (0)
 *
 *   Radiación e iluminación:
 *     - Sin radiación ambiental: 0.0
 *     - Iluminación estándar: 1.0
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 * Estos valores pertenecen a StandardAtmosphere, NO a la infraestructura.
 * Son valores de referencia de este ambiente específico.
 *
 * Si otro ambiente necesita condiciones diferentes (Marte, espacio, lava),
 * declara sus propios valores sin modificar StandardAtmosphere.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Este ambiente es completamente estático. current() retorna siempre
 * la misma instancia cacheada de EnvironmentState.
 *
 * Para ambientes dinámicos (temperatura variable, viento cambiante),
 * crear una implementación diferente que construya el estado en current().
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   Environment env = StandardAtmosphere.INSTANCE;
 *   // o
 *   Environment env = StandardAtmosphere.create();
 *
 *   SimulationContext ctx = SimulationContext.builder(physical)
 *       .environment(env)
 *       .build();
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class StandardAtmosphere implements Environment {

    // ── Condiciones de referencia de este ambiente ────────────────────────

    private static final double AMBIENT_TEMPERATURE      = 0.0;    // 20°C relativo
    private static final double ATMOSPHERIC_PRESSURE     = 1.0;    // 1 atm
    private static final double AMBIENT_HUMIDITY         = 0.6;    // 60%
    private static final double WIND_X                   = 0.0;    // sin viento
    private static final double WIND_Y                   = 0.0;    // sin viento
    private static final double FLUID_DENSITY            = 1.2;    // aire kg/m³
    private static final double FLUID_VISCOSITY          = 0.0;    // despreciable
    private static final double GRAVITY_INFLUENCE_X      = 1.0;    // sin modificación X
    private static final double GRAVITY_INFLUENCE_Y      = 1.0;    // sin modificación Y (normal)
    private static final double ELECTRIC_FIELD_X         = 0.0;    // sin campo
    private static final double ELECTRIC_FIELD_Y         = 0.0;    // sin campo
    private static final double MAGNETIC_FIELD_Z         = 0.0;    // sin campo
    private static final double AMBIENT_RADIATION        = 0.0;    // sin radiación
    private static final double ILLUMINANCE              = 1.0;    // luz estándar

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

    /** Instancia única de StandardAtmosphere. */
    public static final StandardAtmosphere INSTANCE = new StandardAtmosphere();

    /** Factory para consistencia con otros ambientes. */
    public static StandardAtmosphere create() {
        return INSTANCE;
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private StandardAtmosphere() {}

    // ── Environment interface ─────────────────────────────────────────────

    @Override
    public EnvironmentState current() {
        return STATE;
    }

    @Override
    public String getName() {
        return "StandardAtmosphere";
    }

    @Override
    public String toString() {
        return "StandardAtmosphere[temp=" + AMBIENT_TEMPERATURE
            + ", pressure=" + ATMOSPHERIC_PRESSURE
            + ", gravityInfluence=" + GRAVITY_INFLUENCE_Y + "]";
    }
}
