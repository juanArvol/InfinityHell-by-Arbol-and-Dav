package Game.Enemys.Core.Transitions;

import Game.Enemys.Core.Contracts.PhaseTransition;
import Game.Enemys.Core.Enemy;

/**
 * Transición de fase por duración fija.
 *
 * La fase dura exactamente N frames, luego transiciona.
 * El timer se reinicia automáticamente al ser evaluado después de agotarse,
 * por lo que puede reutilizarse si la fase se vuelve a activar.
 *
 * Ejemplo: new TimedTransition(300) → fase dura 300 frames (~5s a 60fps).
 *
 * ── Uso ──────────────────────────────────────────────────────────────────
 *   phaseController.addPhase(new IntroPhase(), new TimedTransition(180));
 *   phaseController.addPhase(new CombatPhase(), null);
 */
public final class TimedTransition implements PhaseTransition {

    private final int durationFrames;
    private int elapsed = 0;

    /**
     * @param durationFrames duración de la fase en frames.
     */
    public TimedTransition(int durationFrames) {
        if (durationFrames <= 0) {
            throw new IllegalArgumentException("durationFrames debe ser > 0");
        }
        this.durationFrames = durationFrames;
    }

    @Override
    public boolean shouldTransition(Enemy enemy) {
        elapsed++;
        if (elapsed >= durationFrames) {
            elapsed = 0;  // reset para reutilización
            return true;
        }
        return false;
    }
}
