package Game.Enemys.Core.Transitions;

import Game.Enemys.Core.Contracts.PhaseTransition;
import Game.Enemys.Core.Enemy;
import Game.Engine.Components.HealthComponent;

/**
 * Transición de fase por umbral de vida.
 *
 * La más común en jefes: transiciona cuando la vida cae por debajo
 * de un porcentaje del máximo.
 *
 * Ejemplo: new HealthThresholdTransition(0.5) → transiciona al 50% de vida.
 *
 * ── Uso ──────────────────────────────────────────────────────────────────
 *   phaseController.addPhase(new Phase1(), new HealthThresholdTransition(0.5));
 *   phaseController.addPhase(new Phase2(), null); // fase final
 */
public final class HealthThresholdTransition implements PhaseTransition {

    private final double threshold;  // [0.0, 1.0]

    /**
     * @param threshold porcentaje de vida en el que se dispara la transición.
     *                  0.5 = transiciona cuando HP <= 50% del máximo.
     */
    public HealthThresholdTransition(double threshold) {
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold debe estar en [0.0, 1.0]");
        }
        this.threshold = threshold;
    }

    @Override
    public boolean shouldTransition(Enemy enemy) {
        HealthComponent hp = enemy.getComponent(HealthComponent.class);
        if (hp == null) return false;
        return hp.getHealthPercent() <= threshold;
    }
}
