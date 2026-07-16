package Game.Enemys.Core.Contracts;

import Game.Enemys.Core.Enemy;

/**
 * Condición de transición entre fases.
 *
 * Una PhaseTransition evalúa cada frame si el Enemy debe cambiar de fase.
 * Vive desacoplada de la lógica de la fase para que las condiciones sean
 * reutilizables entre enemigos distintos.
 *
 * ── Ejemplos ─────────────────────────────────────────────────────────────
 *   HealthThresholdTransition(0.5)  — transiciona cuando HP < 50%.
 *   TimedTransition(300)            — transiciona después de 300 frames.
 *   ManualTransition                — transiciona cuando se llama trigger().
 *
 * ── Uso en EnemyPhaseController ─────────────────────────────────────────
 *   controller.addPhase(phase1, new HealthThresholdTransition(0.5));
 *   controller.addPhase(phase2, null); // fase final, sin condición de salida
 */
public interface PhaseTransition {

    /**
     * Evalúa si se debe transicionar fuera de la fase actual.
     *
     * @param enemy el Enemy evaluado.
     * @return true si debe salir de la fase actual.
     */
    boolean shouldTransition(Enemy enemy);
}
