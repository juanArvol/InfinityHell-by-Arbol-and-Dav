package Game.Engine.Physics.Environment.Contributors;

import Game.Engine.Physics.Environment.EnvironmentalContributor;
import Game.Engine.Physics.Environment.EnvironmentState;

/**
 * Contributor que añade radiación al ambiente.
 *
 * ── HRFC-FASE2.5 — Composed Environmental Influence ──────────────────────
 *
 * ── FENÓMENO ──────────────────────────────────────────────────────────────
 * Representa una fuente de radiación térmica que contribuye a la
 * transferencia de calor radiante en el ambiente.
 *
 * Ejemplos de uso:
 *   - Lava radiante: RadiationSource(0.8)
 *   - Fuego: RadiationSource(0.6)
 *   - Sol: RadiationSource(1.0)
 *   - Radiación nuclear: RadiationSource(2.0)
 *
 * ── SEMÁNTICA ADITIVA ─────────────────────────────────────────────────────
 * Este contributor SUMA su contribución a la radiación existente.
 * No reemplaza la radiación base.
 *
 *   radiation_efectiva = radiation_base + radiation_contribution
 *
 * ── USO EN RELACIONES FÍSICAS ────────────────────────────────────────────
 * La radiación ambiental es utilizada por:
 *   - PlanckEvaluator: intercambio radiativo entre objetos
 *   - RadiationThermalEvaluator: absorción/emisión de radiación
 *
 * Un ambiente con mayor radiación acelera la transferencia térmica
 * radiante entre objetos.
 *
 * ── INMUTABILIDAD ─────────────────────────────────────────────────────────
 * Esta implementación es inmutable. La radiación es constante.
 * Para fuentes radiantes variables, crear una implementación mutable.
 *
 * ── EJEMPLO ───────────────────────────────────────────────────────────────
 *
 *   // Ambiente infernal con radiación térmica intensa
 *   Environment hellEnv = ComposedEnvironment.builder()
 *       .base(StandardAtmosphere.INSTANCE.current())
 *       .add(new ThermalSource(500.0))      // calor de lava
 *       .add(new RadiationSource(0.9))      // radiación térmica intensa
 *       .build();
 *
 *   EnvironmentState state = hellEnv.current();
 *   // state.getAmbientRadiation() = 0 + 0.9 = 0.9
 */
public final class RadiationSource implements EnvironmentalContributor {

    private final double radiationContribution;

    /**
     * Crea una fuente de radiación con la contribución especificada.
     *
     * @param radiationContribution nivel de radiación a añadir [0, +∞).
     *                              Valores típicos: 0.0-2.0
     */
    public RadiationSource(double radiationContribution) {
        this.radiationContribution = Math.max(0.0, radiationContribution);
    }

    @Override
    public void contribute(EnvironmentState.Builder builder) {
        double current = builder.getAmbientRadiation();
        builder.ambientRadiation(current + radiationContribution);
    }

    @Override
    public String toString() {
        return String.format("RadiationSource[%.2f]", radiationContribution);
    }
}
