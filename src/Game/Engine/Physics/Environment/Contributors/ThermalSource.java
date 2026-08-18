package Game.Engine.Physics.Environment.Contributors;

import Game.Engine.Physics.Environment.EnvironmentalContributor;
import Game.Engine.Physics.Environment.EnvironmentState;

/**
 * Contributor que añade temperatura al ambiente.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Representa una fuente térmica que eleva la temperatura ambiental.
 *
 * Ejemplos de uso:
 *   - Lava: ThermalSource(500.0)   → +500°C
 *   - Fuego: ThermalSource(300.0)  → +300°C
 *   - Caldera: ThermalSource(150.0) → +150°C
 *   - Hielo: ThermalSource(-50.0)  → -50°C (enfriamiento)
 *
 * ── SEMÁNTICA ADITIVA ─────────────────────────────────────────────────────
 * Este contributor SUMA su contribución a la temperatura existente.
 * No reemplaza la temperatura base.
 *
 *   temperature_efectiva = temperature_base + thermal_contribution
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Esta implementación es inmutable. La contribución térmica es fija.
 * Para fuentes térmicas variables, crear una implementación mutable o
 * paramétrica (ej: TimeBasedThermalSource).
 *
 * ── EJEMPLO ───────────────────────────────────────────────────────────────
 *
 *   // Ambiente con fuente de lava
 *   Environment hellEnv = ComposedEnvironment.builder()
 *       .base(StandardAtmosphere.INSTANCE.current())  // temp = 20
 *       .add(new ThermalSource(500.0))                // lava
 *       .build();
 *
 *   EnvironmentState state = hellEnv.current();
 *   // state.getAmbientTemperature() = 20 + 500 = 520
 */
public final class ThermalSource implements EnvironmentalContributor {

    private final double thermalContribution;

    /**
     * Crea una fuente térmica con la contribución especificada.
     *
     * @param thermalContribution temperatura a añadir. Puede ser negativa
     *                            para representar enfriamiento.
     */
    public ThermalSource(double thermalContribution) {
        this.thermalContribution = thermalContribution;
    }

    @Override
    public void contribute(EnvironmentState.Builder builder) {
        double current = builder.getAmbientTemperature();
        builder.ambientTemperature(current + thermalContribution);
    }

    @Override
    public String toString() {
        return String.format("ThermalSource[%+.1f°]", thermalContribution);
    }
}
