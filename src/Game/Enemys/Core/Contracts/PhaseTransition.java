package Game.Enemys.Core.Contracts;

import Game.Enemys.Core.Enemy;

/**
 * Condición de transición entre fases.
 *
 * Una PhaseTransition evalúa cada frame si el Enemy debe cambiar de fase.
 * Vive desacoplada de la lógica de la fase para que las condiciones sean
 * reutilizables entre enemigos distintos.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   El método shouldTransition() ahora recibe deltaTime para permitir que
 *   las transiciones temporales (TimedTransition) gestionen su estado
 *   basándose en tiempo real en lugar de frames.
 *
 *   Transiciones no temporales (HealthThresholdTransition, ManualTransition)
 *   ignoran el parámetro deltaTime.
 *
 * ── Ejemplos ─────────────────────────────────────────────────────────────
 *   HealthThresholdTransition(0.5)  — transiciona cuando HP < 50%.
 *   TimedTransition(3.0)            — transiciona después de 3.0 segundos.
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
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * Recibe deltaTime para que transiciones temporales puedan acumular
     * tiempo de forma independiente del framerate.
     *
     * Transiciones basadas en estado (HealthThreshold, etc.) ignoran deltaTime.
     *
     * @param enemy el Enemy evaluado.
     * @param deltaTime tiempo real del simulation step en segundos
     * @return true si debe salir de la fase actual.
     */
    boolean shouldTransition(Enemy enemy, double deltaTime);
}
