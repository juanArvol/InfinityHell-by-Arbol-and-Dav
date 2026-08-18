package Game.Engine.Physics.Environment;

import Game.Engine.Physics.Environment.Contributors.RadiationSource;
import Game.Engine.Physics.Environment.Contributors.ThermalSource;
import Game.Engine.Physics.Environment.Contributors.WindSource;

/**
 * Ambiente infernal: calor extremo, vientos turbulentos, radiación intensa.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── CONDICIONES DE REFERENCIA ────────────────────────────────────────────
 * Este ambiente representa un entorno infernal con las siguientes
 * características compuestas:
 *
 *   Base atmosférica:
 *     - Presión atmosférica: 1.2 atm (denso por gases volcánicos)
 *     - Densidad del aire: 1.5 kg/m³ (aire caliente + ceniza)
 *     - Humedad: 0.0 (aire seco extremo)
 *     - Factor de influencia gravitacional: 1.0 (sin modificación)
 *
 *   Fenómenos térmicos:
 *     - Fuentes de lava: +500°C sobre temperatura base
 *     - Radiación térmica intensa: 0.9
 *
 *   Fenómenos atmosféricos:
 *     - Vientos turbulentos horizontales: 12 u/s
 *     - Corrientes ascendentes de calor: -3 u/s (hacia arriba)
 *
 * ── RESULTADO EFECTIVO ───────────────────────────────────────────────────
 * Las condiciones efectivas son:
 *   - Temperatura ambiente: 520°C (base 20 + lava 500)
 *   - Presión: 1.2 atm
 *   - Viento: (12, -3) u/s
 *   - Radiación: 0.9
 *   - Gravedad: (0, 9.8) u/s²
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 * Estos valores pertenecen a HellEnvironment, NO a la infraestructura.
 * Son valores de referencia de este ambiente específico.
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * Este ambiente utiliza ComposedEnvironment para combinar:
 *   1. Condiciones atmosféricas base (presión, densidad, gravedad)
 *   2. ThermalSource (lava)
 *   3. RadiationSource (radiación térmica)
 *   4. WindSource horizontal (vientos turbulentos)
 *   5. WindSource vertical (corrientes térmicas)
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Este ambiente es estático. current() retorna siempre las mismas condiciones
 * efectivas. Para ambientes infernales variables (tormentas de fuego, etc.),
 * crear contributors dinámicos o un ambiente diferente.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   Environment env = HellEnvironment.INSTANCE;
 *   // o
 *   Environment env = HellEnvironment.create();
 *
 *   SimulationContext ctx = SimulationContext.builder(physical)
 *       .environment(env)
 *       .build();
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class HellEnvironment implements Environment {

    // ── Condiciones base ──────────────────────────────────────────────────

    private static final double BASE_TEMPERATURE         = 20.0;   // temperatura ref antes de lava
    private static final double ATMOSPHERIC_PRESSURE     = 1.2;    // aire denso volcánico
    private static final double AMBIENT_HUMIDITY         = 0.0;    // aire seco
    private static final double FLUID_DENSITY            = 1.5;    // aire + ceniza
    private static final double FLUID_VISCOSITY          = 0.0;    // despreciable
    private static final double GRAVITY_INFLUENCE_X      = 1.0;    // sin modificación X
    private static final double GRAVITY_INFLUENCE_Y      = 1.0;    // sin modificación Y (normal)
    private static final double ILLUMINANCE              = 0.7;    // luz tenue (fuego)

    // ── Fenómenos ambientales ─────────────────────────────────────────────

    private static final double LAVA_TEMPERATURE     = 500.0;  // +500°C por lava
    private static final double THERMAL_RADIATION    = 0.9;    // radiación intensa
    private static final double TURBULENT_WIND_X     = 12.0;   // vientos horizontales
    private static final double THERMAL_UPDRAFT_Y    = -3.0;   // corrientes ascendentes

    // ── Ambiente compuesto ────────────────────────────────────────────────

    /**
     * Ambiente interno que combina base + fenómenos.
     * Se construye una sola vez y se cachea.
     */
    private static final Environment COMPOSED = ComposedEnvironment.builder("HellEnvironment")
        .base(buildBaseConditions())
        .add(new ThermalSource(LAVA_TEMPERATURE))
        .add(new RadiationSource(THERMAL_RADIATION))
        .add(WindSource.horizontal(TURBULENT_WIND_X))
        .add(WindSource.vertical(THERMAL_UPDRAFT_Y))
        .build();

    /**
     * Construye las condiciones atmosféricas base del infierno
     * (antes de aplicar fenómenos como lava, viento, radiación).
     */
    private static EnvironmentState buildBaseConditions() {
        return EnvironmentState.builder()
            .ambientTemperature(BASE_TEMPERATURE)
            .atmosphericPressure(ATMOSPHERIC_PRESSURE)
            .ambientHumidity(AMBIENT_HUMIDITY)
            .fluidDensity(FLUID_DENSITY)
            .fluidViscosity(FLUID_VISCOSITY)
            .gravityInfluenceX(GRAVITY_INFLUENCE_X)
            .gravityInfluenceY(GRAVITY_INFLUENCE_Y)
            .illuminance(ILLUMINANCE)
            .build();
    }

    // ── Singleton ─────────────────────────────────────────────────────────

    /** Instancia única de HellEnvironment. */
    public static final HellEnvironment INSTANCE = new HellEnvironment();

    /** Factory para consistencia con otros ambientes. */
    public static HellEnvironment create() {
        return INSTANCE;
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private HellEnvironment() {}

    // ── Environment interface ─────────────────────────────────────────────

    @Override
    public EnvironmentState current() {
        return COMPOSED.current();
    }

    @Override
    public String getName() {
        return "HellEnvironment";
    }

    @Override
    public String toString() {
        EnvironmentState state = current();
        return String.format(
            "HellEnvironment[temp=%.0f°C, pressure=%.1fatm, wind=(%.1f,%.1f), radiation=%.2f]",
            state.getAmbientTemperature(),
            state.getAtmosphericPressure(),
            state.getWindX(),
            state.getWindY(),
            state.getAmbientRadiation()
        );
    }
}
