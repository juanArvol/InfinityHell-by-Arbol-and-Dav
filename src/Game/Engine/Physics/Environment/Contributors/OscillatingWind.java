package Game.Engine.Physics.Environment.Contributors;

import Game.Engine.Physics.Environment.EnvironmentalContributor;
import Game.Engine.Physics.Environment.EnvironmentState;

/**
 * Contributor de viento oscilante que varía sinusoidalmente con el tiempo.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Representa un viento que oscila en dirección e intensidad siguiendo
 * un patrón sinusoidal. Útil para simular ráfagas de viento, corrientes
 * alternantes, o condiciones atmosféricas cambiantes.
 *
 * ── DINAMISMO ─────────────────────────────────────────────────────────────
 * A diferencia de WindSource (inmutable), este contributor es MUTABLE.
 * Su contribución cambia cada vez que se invoca contribute(), basándose
 * en el tiempo transcurrido.
 *
 * Para usar correctamente, actualizar el tiempo antes de cada consulta:
 *
 *   OscillatingWind wind = new OscillatingWind(10.0, 0.0, 2.0, 0.0);
 *
 *   // En el game loop:
 *   wind.update(deltaTime);  // actualizar tiempo interno
 *   EnvironmentState state = environment.current();  // obtener condiciones
 *
 * ── PARÁMETROS ────────────────────────────────────────────────────────────
 *   amplitude  → amplitud máxima del viento
 *   baseX/Y    → componente base (constante)
 *   period     → periodo de oscilación en segundos
 *   phase      → desplazamiento de fase inicial
 *
 * ── EJEMPLO ───────────────────────────────────────────────────────────────
 *
 *   // Ambiente con viento oscilante
 *   OscillatingWind wind = new OscillatingWind(
 *       8.0,   // amplitud
 *       5.0,   // base X (viento constante derecha)
 *       0.0,   // base Y
 *       3.0,   // periodo 3 segundos
 *       0.0    // sin fase inicial
 *   );
 *
 *   Environment stormyPlains = ComposedEnvironment.builder("StormyPlains")
 *       .base(StandardAtmosphere.INSTANCE.current())
 *       .add(wind)
 *       .build();
 *
 *   // En game loop (60 FPS):
 *   wind.update(1.0 / 60.0);  // ~0.0167 segundos
 *
 *   // Resultado:
 *   // windX oscila entre 5-8 = -3 y 5+8 = 13 con periodo de 3s
 *
 * ── THREAD SAFETY ─────────────────────────────────────────────────────────
 * NO thread-safe. Debe actualizarse y consultarse desde el mismo thread
 * (típicamente el game loop thread).
 */
public final class OscillatingWind implements EnvironmentalContributor {

    private final double amplitudeX;
    private final double amplitudeY;
    private final double baseX;
    private final double baseY;
    private final double period;
    private final double phase;

    private double elapsedTime = 0.0;

    /**
     * Crea un viento oscilante con parámetros especificados.
     *
     * @param amplitudeX amplitud de oscilación en X.
     * @param amplitudeY amplitud de oscilación en Y.
     * @param baseX componente X base (constante).
     * @param baseY componente Y base (constante).
     * @param period periodo de oscilación en segundos.
     * @param phase desplazamiento de fase inicial en radianes.
     */
    public OscillatingWind(double amplitudeX, double amplitudeY,
                          double baseX, double baseY,
                          double period, double phase) {
        this.amplitudeX = amplitudeX;
        this.amplitudeY = amplitudeY;
        this.baseX = baseX;
        this.baseY = baseY;
        this.period = Math.max(0.1, period);  // evitar división por cero
        this.phase = phase;
    }

    /**
     * Crea un viento oscilante horizontal.
     *
     * @param amplitude amplitud de oscilación.
     * @param base componente base constante.
     * @param period periodo en segundos.
     * @return contributor de viento oscilante horizontal.
     */
    public static OscillatingWind horizontal(double amplitude, double base, double period) {
        return new OscillatingWind(amplitude, 0.0, base, 0.0, period, 0.0);
    }

    /**
     * Crea un viento oscilante vertical.
     *
     * @param amplitude amplitud de oscilación.
     * @param base componente base constante.
     * @param period periodo en segundos.
     * @return contributor de viento oscilante vertical.
     */
    public static OscillatingWind vertical(double amplitude, double base, double period) {
        return new OscillatingWind(0.0, amplitude, 0.0, base, period, 0.0);
    }

    /**
     * Actualiza el tiempo interno del viento oscilante.
     *
     * IMPORTANTE: Debe llamarse cada frame antes de consultar environment.current().
     *
     * @param deltaTime tiempo transcurrido desde la última actualización (en segundos).
     */
    public void update(double deltaTime) {
        elapsedTime += deltaTime;
    }

    /**
     * Resetea el tiempo interno a cero.
     */
    public void reset() {
        elapsedTime = 0.0;
    }

    @Override
    public void contribute(EnvironmentState.Builder builder) {
        // Calcular valor sinusoidal actual
        double omega = (2.0 * Math.PI) / period;  // frecuencia angular
        double t = omega * elapsedTime + phase;
        double sinValue = Math.sin(t);

        // Calcular contribución actual
        double windX = baseX + amplitudeX * sinValue;
        double windY = baseY + amplitudeY * sinValue;

        // Añadir a las condiciones existentes
        double currentX = builder.getWindX();
        double currentY = builder.getWindY();
        builder.windX(currentX + windX);
        builder.windY(currentY + windY);
    }

    @Override
    public String toString() {
        return String.format(
            "OscillatingWind[base=(%.1f,%.1f), amplitude=(%.1f,%.1f), period=%.1fs, t=%.2fs]",
            baseX, baseY, amplitudeX, amplitudeY, period, elapsedTime
        );
    }
}
