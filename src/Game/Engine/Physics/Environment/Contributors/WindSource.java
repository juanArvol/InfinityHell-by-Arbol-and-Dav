package Game.Engine.Physics.Environment.Contributors;

import Game.Engine.Physics.Environment.EnvironmentalContributor;
import Game.Engine.Physics.Environment.EnvironmentState;

/**
 * Contributor que añade viento al ambiente.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Representa una fuente de viento que añade movimiento de aire al ambiente.
 *
 * Ejemplos de uso:
 *   - Viento horizontal: WindSource(10.0, 0.0)
 *   - Viento ascendente: WindSource(0.0, -5.0)
 *   - Viento descendente: WindSource(0.0, 8.0)
 *   - Viento diagonal: WindSource(7.0, 7.0)
 *
 * ── SEMÁNTICA VECTORIAL ──────────────────────────────────────────────────
 * Este contributor SUMA su contribución vectorialmente al viento existente.
 * No reemplaza el viento base.
 *
 *   wind_efectivo = wind_base + wind_contribution
 *
 * Múltiples WindSources se componen vectorialmente:
 *   WindSource(10, 0) + WindSource(0, 5) = wind(10, 5)
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Esta implementación es inmutable. El viento es constante.
 * Para vientos variables, crear una implementación mutable
 * (ej: OscillatingWindSource, DirectionalWindSource).
 *
 * ── EJEMPLO ───────────────────────────────────────────────────────────────
 *
 *   // Ambiente con viento horizontal fuerte
 *   Environment windyPlains = ComposedEnvironment.builder()
 *       .base(StandardAtmosphere.INSTANCE.current())
 *       .add(new WindSource(15.0, 0.0))  // viento derecha
 *       .build();
 *
 *   EnvironmentState state = windyPlains.current();
 *   // state.getWindX() = 0 + 15 = 15
 *   // state.getWindY() = 0 + 0 = 0
 */
public final class WindSource implements EnvironmentalContributor {

    private final double windX;
    private final double windY;

    /**
     * Crea una fuente de viento con las componentes especificadas.
     *
     * @param windX componente X del viento (positivo = derecha).
     * @param windY componente Y del viento (positivo = abajo, convención AWT).
     */
    public WindSource(double windX, double windY) {
        this.windX = windX;
        this.windY = windY;
    }

    /**
     * Crea una fuente de viento horizontal.
     *
     * @param windX componente X del viento (positivo = derecha).
     * @return contributor de viento horizontal.
     */
    public static WindSource horizontal(double windX) {
        return new WindSource(windX, 0.0);
    }

    /**
     * Crea una fuente de viento vertical.
     *
     * @param windY componente Y del viento (positivo = abajo).
     * @return contributor de viento vertical.
     */
    public static WindSource vertical(double windY) {
        return new WindSource(0.0, windY);
    }

    @Override
    public void contribute(EnvironmentState.Builder builder) {
        double currentX = builder.getWindX();
        double currentY = builder.getWindY();
        builder.windX(currentX + windX);
        builder.windY(currentY + windY);
    }

    @Override
    public String toString() {
        return String.format("WindSource[(%+.1f, %+.1f)]", windX, windY);
    }
}
