package Game.Engine.Profiling;

/**
 * Timer de alta resolución para medir operaciones individuales.
 *
 * ── HRFC — Bottleneck Diagnosis Infrastructure ────────────────────────────
 *
 * SubsystemTimer usa System.nanoTime() para mediciones de alta precisión
 * de operaciones individuales (update, render, collision detection, etc.).
 *
 * DISEÑO:
 *   - Medición con nanoTime() (±1 microsegundo en hardware moderno)
 *   - API start/stop simple — sin overhead de allocations
 *   - Resultados en milisegundos (ms) para legibilidad
 *   - Reutilizable — llamar reset() entre mediciones
 *
 * USO:
 *   SubsystemTimer timer = new SubsystemTimer();
 *   timer.start();
 *   // operación a medir
 *   timer.stop();
 *   double ms = timer.getElapsedMs();
 *
 * NOTA: Para medir subsistemas críticos usar directamente en lugar de
 * crear abstracciones adicionales que añadan overhead.
 */
public class SubsystemTimer {
    private long startNanos;
    private long elapsedNanos;
    private boolean running;

    public SubsystemTimer() {
        reset();
    }

    /**
     * Inicia la medición del tiempo.
     * Si ya está running, reinicia desde cero.
     */
    public void start() {
        startNanos = System.nanoTime();
        elapsedNanos = 0;
        running = true;
    }

    /**
     * Detiene la medición y registra el tiempo transcurrido.
     * Si no está running, no hace nada.
     */
    public void stop() {
        if (running) {
            elapsedNanos = System.nanoTime() - startNanos;
            running = false;
        }
    }

    /**
     * Retorna el tiempo transcurrido en milisegundos.
     * Si está running, retorna el tiempo parcial desde start() hasta ahora.
     * Si ya se detuvo, retorna el tiempo total medido.
     */
    public double getElapsedMs() {
        if (running) {
            long currentNanos = System.nanoTime();
            return (currentNanos - startNanos) / 1_000_000.0;
        } else {
            return elapsedNanos / 1_000_000.0;
        }
    }

    /**
     * Retorna el tiempo transcurrido en nanosegundos.
     */
    public long getElapsedNanos() {
        if (running) {
            return System.nanoTime() - startNanos;
        } else {
            return elapsedNanos;
        }
    }

    /**
     * Resetea el timer a su estado inicial.
     */
    public void reset() {
        startNanos = 0;
        elapsedNanos = 0;
        running = false;
    }

    /**
     * Retorna true si el timer está actualmente corriendo.
     */
    public boolean isRunning() {
        return running;
    }
}
