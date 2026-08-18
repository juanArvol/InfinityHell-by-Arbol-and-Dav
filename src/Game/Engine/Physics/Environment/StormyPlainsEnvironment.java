package Game.Engine.Physics.Environment;

import Game.Engine.Physics.Environment.Contributors.OscillatingWind;

/**
 * Ambiente de llanuras tormentosas con vientos oscilantes.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── CONDICIONES DE REFERENCIA ────────────────────────────────────────────
 * Este ambiente representa llanuras abiertas con condiciones tormentosas:
 *
 *   Base atmosférica (similar a StandardAtmosphere):
 *     - Temperatura: 15°C (más frío por tormenta)
 *     - Presión: 0.95 atm (baja presión de tormenta)
 *     - Humedad: 0.9 (aire húmedo)
 *     - Factor de influencia gravitacional: 1.0 (sin modificación)
 *
 *   Fenómenos dinámicos:
 *     - Vientos oscilantes horizontales (ráfagas)
 *     - Periodo: 4 segundos
 *     - Amplitud: 12 u/s
 *     - Base: 8 u/s (viento constante subyacente)
 *
 *   Iluminación:
 *     - Luz reducida: 0.6 (nubes de tormenta)
 *
 * ── DINAMISMO ─────────────────────────────────────────────────────────────
 * Este ambiente es DINÁMICO. El viento oscila con el tiempo, produciendo
 * diferentes EnvironmentState en cada consulta.
 *
 * IMPORTANTE: Debe actualizarse cada frame:
 *
 *   Environment env = StormyPlainsEnvironment.INSTANCE;
 *
 *   // En game loop:
 *   StormyPlainsEnvironment.INSTANCE.update(deltaTime);
 *   EnvironmentState state = env.current();  // condiciones actuales
 *
 * ── RESULTADO EFECTIVO ───────────────────────────────────────────────────
 * Las condiciones efectivas varían con el tiempo:
 *   - Temperatura: 15°C (constante)
 *   - Presión: 0.95 atm (constante)
 *   - windX: oscila entre 8-12 = -4 y 8+12 = 20 con periodo 4s
 *   - windY: 0 (sin viento vertical)
 *   - Gravedad: 9.8 m/s² (constante)
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * Este ambiente utiliza ComposedEnvironment con un contributor mutable
 * (OscillatingWind) para producir condiciones dinámicas.
 *
 * Estructura:
 *   Base conditions (estáticas)
 *     +
 *   OscillatingWind (dinámico)
 *     ↓
 *   EnvironmentState actual
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   Environment env = StormyPlainsEnvironment.INSTANCE;
 *
 *   SimulationContext ctx = SimulationContext.builder(physical)
 *       .environment(env)
 *       .build();
 *
 *   // En game loop:
 *   StormyPlainsEnvironment.INSTANCE.update(deltaTime);
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * NO thread-safe debido al contributor mutable.
 * Usar exclusivamente desde el game loop thread.
 */
public final class StormyPlainsEnvironment implements Environment {

    // ── Condiciones atmosféricas base ─────────────────────────────────────

    private static final double BASE_TEMPERATURE     = 15.0;  // frío de tormenta
    private static final double ATMOSPHERIC_PRESSURE = 0.95;  // baja presión
    private static final double AMBIENT_HUMIDITY     = 0.9;   // aire húmedo
    private static final double FLUID_DENSITY            = 1.2;   // aire normal
    private static final double GRAVITY_INFLUENCE_Y      = 1.0;   // sin modificación (normal)
    private static final double ILLUMINANCE              = 0.6;   // luz reducida

    // ── Fenómenos dinámicos ───────────────────────────────────────────────

    private static final double WIND_AMPLITUDE       = 12.0;  // amplitud de ráfagas
    private static final double WIND_BASE            = 8.0;   // viento subyacente
    private static final double WIND_PERIOD          = 4.0;   // 4 segundos

    // ── Contributor mutable ───────────────────────────────────────────────

    /**
     * Viento oscilante que produce las ráfagas dinámicas.
     * Debe actualizarse cada frame con update(deltaTime).
     */
    private final OscillatingWind wind = OscillatingWind.horizontal(
        WIND_AMPLITUDE,
        WIND_BASE,
        WIND_PERIOD
    );

    // ── Ambiente compuesto ────────────────────────────────────────────────

    /**
     * Condiciones base (estáticas) antes de aplicar fenómenos dinámicos.
     */
    private static final EnvironmentState BASE = EnvironmentState.builder()
        .ambientTemperature(BASE_TEMPERATURE)
        .atmosphericPressure(ATMOSPHERIC_PRESSURE)
        .ambientHumidity(AMBIENT_HUMIDITY)
        .fluidDensity(FLUID_DENSITY)
        .gravityInfluenceY(GRAVITY_INFLUENCE_Y)
        .illuminance(ILLUMINANCE)
        .build();

    /**
     * Ambiente compuesto que combina base + viento dinámico.
     * Se construye una sola vez, pero produce diferentes estados
     * según el tiempo transcurrido en el contributor.
     */
    private final Environment composed = ComposedEnvironment.builder("StormyPlains")
        .base(BASE)
        .add(wind)
        .build();

    // ── Singleton ─────────────────────────────────────────────────────────

    /** Instancia única de StormyPlainsEnvironment. */
    public static final StormyPlainsEnvironment INSTANCE = new StormyPlainsEnvironment();

    /** Factory para consistencia con otros ambientes. */
    public static StormyPlainsEnvironment create() {
        return INSTANCE;
    }

    // ── Constructor privado ───────────────────────────────────────────────

    private StormyPlainsEnvironment() {}

    // ── Actualización dinámica ────────────────────────────────────────────

    /**
     * Actualiza el tiempo interno de los fenómenos dinámicos.
     *
     * IMPORTANTE: Debe llamarse cada frame antes de consultar current().
     *
     * @param deltaTime tiempo transcurrido desde el último frame (en segundos).
     */
    public void update(double deltaTime) {
        wind.update(deltaTime);
    }

    /**
     * Resetea el tiempo de los fenómenos dinámicos a cero.
     */
    public void reset() {
        wind.reset();
    }

    // ── Environment interface ─────────────────────────────────────────────

    @Override
    public EnvironmentState current() {
        return composed.current();
    }

    @Override
    public String getName() {
        return "StormyPlainsEnvironment";
    }

    @Override
    public String toString() {
        EnvironmentState state = current();
        return String.format(
            "StormyPlainsEnvironment[temp=%.0f°C, pressure=%.2fatm, windX=%.1f (dynamic)]",
            state.getAmbientTemperature(),
            state.getAtmosphericPressure(),
            state.getWindX()
        );
    }
}
