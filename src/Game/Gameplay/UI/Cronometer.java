package Game.Gameplay.UI;

/**
 * Cronómetro simple para temporización de eventos en UI/gameplay.
 *
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 *
 * MIGRACIÓN: System.currentTimeMillis() → acumulación de deltaTime.
 *
 * ANTES:
 *   startTime = System.currentTimeMillis()
 *   elapsed = System.currentTimeMillis() - startTime
 *   Tiempo independiente del game loop
 *
 * AHORA:
 *   elapsed += deltaTime (propagado desde GameLoop)
 *   Tiempo coherente con la simulación
 *
 * VENTAJAS:
 *   - Consistente con el resto del sistema temporal
 *   - Pausa automática cuando el juego pausa
 *   - No desincroniza durante lag spikes
 *   - Testeable sin depender del reloj del sistema
 */
public class Cronometer {

    private double duration;         // duración total en segundos
    private double elapsed;          // tiempo acumulado en segundos
    private boolean running;

    public Cronometer() {
        this.running = false;
        this.elapsed = 0.0;
        this.duration = 0.0;
    }

    /**
     * Inicia el cronómetro por 'seconds' segundos.
     *
     * @param seconds duración en segundos
     */
    public void run(double seconds) {
        this.duration = seconds;
        this.elapsed  = 0.0;
        this.running  = true;
    }

    /**
     * Actualiza el estado del cronómetro.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void update(double deltaTime) {
        if (!running) return;

        elapsed += deltaTime;
        if (elapsed >= duration) {
            running = false;
            // Clamp para evitar que elapsed exceda duration significativamente
            elapsed = duration;
        }
    }

    /** @return true si el cronómetro todavía está corriendo. */
    public boolean isRunning() {
        return running;
    }

    /**
     * Segundos transcurridos desde que se inició.
     *
     * @return tiempo acumulado en segundos
     */
    public double getElapsed() {
        return elapsed;
    }

    /**
     * Progreso de 0.0 (inicio) a 1.0 (completado).
     *
     * @return progreso normalizado [0.0, 1.0]
     */
    public double getProgress() {
        return duration > 0.0 ? Math.min(1.0, elapsed / duration) : 0.0;
    }

    /** Detiene el cronómetro manualmente. */
    public void stop() {
        running = false;
    }

    /** Reinicia el cronómetro con la misma duración. */
    public void restart() {
        elapsed = 0.0;
        running = true;
    }
}
