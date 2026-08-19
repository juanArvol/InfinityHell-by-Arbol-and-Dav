package Game.Enemys.Core.Transitions;

import Game.Enemys.Core.Contracts.PhaseTransition;
import Game.Enemys.Core.Enemy;

/**
 * Transición de fase por duración fija en tiempo real.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   TimedTransition ahora usa tiempo real en segundos en lugar de frames.
 *   Esto garantiza que la duración de la fase es independiente del framerate.
 *
 *   ANTES (frame-based):
 *     elapsed++ en cada shouldTransition()
 *     durationFrames = 300 frames
 *     A 31 FPS: 300 frames = 9.68 segundos
 *     A 60 FPS: 300 frames = 5.00 segundos
 *     A 120 FPS: 300 frames = 2.50 segundos
 *
 *   AHORA (time-based):
 *     elapsed += deltaTime
 *     durationSeconds = 5.0 segundos
 *     A cualquier FPS: duración = 5.00 segundos
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *
 * La fase dura exactamente N segundos, luego transiciona.
 * El timer se reinicia automáticamente al transicionar, por lo que puede
 * reutilizarse si la fase se vuelve a activar.
 *
 * ── Uso ──────────────────────────────────────────────────────────────────
 *   // Fase dura 3 segundos
 *   phaseController.addPhase(new IntroPhase(), new TimedTransition(3.0));
 *   phaseController.addPhase(new CombatPhase(), null);
 *
 * ── Migración desde código legacy ─────────────────────────────────────────
 *   ANTES: new TimedTransition(180)  // 180 frames @ 60 FPS
 *   AHORA: new TimedTransition(3.0)  // 3.0 segundos
 *
 *   O usar factory:
 *   TimedTransition.fromFrames(180, 60)  // convierte 180 frames a segundos
 */
public final class TimedTransition implements PhaseTransition {

    private final double durationSeconds;
    private double elapsed = 0.0;

    /**
     * Constructor con duración en segundos.
     *
     * @param durationSeconds duración de la fase en segundos reales
     */
    public TimedTransition(double durationSeconds) {
        if (durationSeconds <= 0.0) {
            throw new IllegalArgumentException("durationSeconds debe ser > 0");
        }
        this.durationSeconds = durationSeconds;
    }

    /**
     * Factory method para compatibilidad con código legacy que usaba frames.
     *
     * @param durationFrames cantidad de frames que duraba la fase
     * @param targetFps framerate asumido por el código legacy (típicamente 60)
     * @return TimedTransition configurado con tiempo equivalente en segundos
     *
     * @deprecated Usar constructor con segundos directamente
     */
    @Deprecated
    public static TimedTransition fromFrames(int durationFrames, double targetFps) {
        return new TimedTransition(durationFrames / targetFps);
    }

    /**
     * Evalúa si se debe transicionar.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * Acumula tiempo real transcurrido en lugar de contar frames.
     * Al alcanzar durationSeconds, transiciona y resetea el contador.
     *
     * @param enemy el Enemy evaluado (no usado, pero requerido por interfaz)
     * @param deltaTime tiempo real del simulation step en segundos
     * @return true si el tiempo transcurrido alcanzó la duración configurada
     */
    @Override
    public boolean shouldTransition(Enemy enemy, double deltaTime) {
        elapsed += deltaTime;
        
        if (elapsed >= durationSeconds) {
            elapsed = 0.0;  // reset para reutilización
            return true;
        }
        
        return false;
    }

    /**
     * Consulta el tiempo transcurrido desde el inicio de la fase.
     *
     * @return tiempo acumulado en segundos
     */
    public double getElapsed() {
        return elapsed;
    }

    /**
     * Consulta la duración configurada de la fase.
     *
     * @return duración en segundos
     */
    public double getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Consulta el progreso de la fase [0.0, 1.0].
     *
     * @return 0.0 = inicio, 1.0 = punto de transición
     */
    public double getProgress() {
        return Math.min(1.0, elapsed / durationSeconds);
    }

    /**
     * Resetea manualmente el timer (útil para re-iniciar la fase).
     */
    public void reset() {
        elapsed = 0.0;
    }
}
