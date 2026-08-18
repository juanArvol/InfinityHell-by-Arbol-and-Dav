package Game.Engine.Physics.Environment;

/**
 * Ambiente submarino: alta densidad, resistencia fluídica, presión elevada.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── CONDICIONES DE REFERENCIA ────────────────────────────────────────────
 * Este ambiente representa un entorno submarino con las siguientes
 * características:
 *
 *   Base fluídica:
 *     - Presión atmosférica: 2.0 atm (equivalente a ~10m profundidad)
 *     - Densidad del fluido: 1000.0 kg/m³ (agua)
 *     - Viscosidad del fluido: 0.001 (resistencia al movimiento)
 *     - Humedad: 1.0 (saturación completa)
 *     - Temperatura: 10°C (agua fría)
 *
 *   Fenómenos gravitacionales:
 *     - Gravedad reducida efectiva por empuje de Arquímedes
 *     - La flotabilidad emerge de la relación entre fluidDensity y entity.mass
 *
 *   Iluminación:
 *     - Luz atenuada: 0.3 (luz filtrada por agua)
 *
 * ── RESULTADO EFECTIVO ───────────────────────────────────────────────────
 * Las condiciones efectivas son:
 *   - Temperatura: 10°C
 *   - Presión: 2.0 atm
 *   - Densidad: 1000 kg/m³ (ArchimedesEvaluator usará esto)
 *   - Viscosidad: 0.001 (StokesEvaluator usará esto)
 *   - Gravedad: 9.8 m/s² (la flotabilidad emerge de las relaciones)
 *
 * ── FÍSICA ESPERADA ───────────────────────────────────────────────────────
 * Con este ambiente, los evaluadores físicos producirán:
 *
 *   ArchimedesEvaluator:
 *     - Empuje hacia arriba proporcional a fluidDensity y volumen del objeto
 *     - Objetos menos densos que el agua flotarán
 *     - Objetos más densos se hundirán lentamente
 *
 *   StokesEvaluator:
 *     - Resistencia viscosa proporcional a velocidad
 *     - Movimiento más lento que en aire
 *     - Velocidad terminal reducida
 *
 *   ThermalRelations:
 *     - Transferencia térmica más rápida que en aire
 *     - Los objetos tienden a 10°C (temperatura del agua)
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 * Estos valores pertenecen a UnderwaterEnvironment, NO a la infraestructura.
 * Son valores de referencia de este ambiente específico.
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * Este ambiente es principalmente estático (no usa ComposedEnvironment)
 * porque las condiciones submarinas son relativamente uniformes.
 *
 * Para ambientes submarinos con corrientes, usar ComposedEnvironment:
 *   ComposedEnvironment.builder()
 *       .base(UnderwaterEnvironment.INSTANCE.current())
 *       .add(WindSource.horizontal(5.0))  // corriente marina
 *       .build()
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   Environment env = UnderwaterEnvironment.INSTANCE;
 *   // o
 *   Environment env = UnderwaterEnvironment.create();
 *
 *   SimulationContext ctx = SimulationContext.builder(physical)
 *       .environment(env)
 *       .build();
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * Inmutable → thread-safe por diseño.
 */
public final class UnderwaterEnvironment implements Environment {

    // ── Condiciones fluídicas base ────────────────────────────────────────

    private static final double WATER_TEMPERATURE        = 10.0;   // agua fría
    private static final double WATER_PRESSURE           = 2.0;    // ~10m profundidad
    private static final double WATER_HUMIDITY           = 1.0;    // saturación total
    private static final double WATER_DENSITY            = 1000.0; // agua (kg/m³)
    private static final double WATER_VISCOSITY          = 0.001;  // resistencia al movimiento
    private static final double GRAVITY_INFLUENCE_X      = 1.0;    // sin modificación X
    private static final double GRAVITY_INFLUENCE_Y      = 1.0;    // sin modificación Y (flotabilidad por Arquímedes)
    private static final double ILLUMINANCE              = 0.3;    // luz atenuada

    // ── Estado cacheado ───────────────────────────────────────────────────

    /**
     * Estado ambiental inmutable de este ambiente.
     * Construido una sola vez y reutilizado en todas las consultas.
     */
    private static final EnvironmentState STATE = EnvironmentState.builder()
        .ambientTemperature(WATER_TEMPERATURE)
        .atmosphericPressure(WATER_PRESSURE)
        .ambientHumidity(WATER_HUMIDITY)
        .fluidDensity(WATER_DENSITY)
        .fluidViscosity(WATER_VISCOSITY)
        .gravityInfluenceX(GRAVITY_INFLUENCE_X)
        .gravityInfluenceY(GRAVITY_INFLUENCE_Y)
        .illuminance(ILLUMINANCE)
        .build();

    // ── Singleton ─────────────────────────────────────────────────────────

    /** Instancia única de UnderwaterEnvironment. */
    public static final UnderwaterEnvironment INSTANCE = new UnderwaterEnvironment();

    /** Factory para consistencia con otros ambientes. */
    public static UnderwaterEnvironment create() {
        return INSTANCE;
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private UnderwaterEnvironment() {}

    // ── Environment interface ─────────────────────────────────────────────

    @Override
    public EnvironmentState current() {
        return STATE;
    }

    @Override
    public String getName() {
        return "UnderwaterEnvironment";
    }

    @Override
    public String toString() {
        return String.format(
            "UnderwaterEnvironment[temp=%.0f°C, pressure=%.1fatm, density=%.0fkg/m³, viscosity=%.4f]",
            WATER_TEMPERATURE,
            WATER_PRESSURE,
            WATER_DENSITY,
            WATER_VISCOSITY
        );
    }
}
