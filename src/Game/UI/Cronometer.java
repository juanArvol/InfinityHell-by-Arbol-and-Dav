package Game.UI;

/**
 * Cronómetro simple para temporización de eventos en UI/gameplay.
 *
 * FIX DESIGN-008: en el original, lastTime se actualizaba cada frame
 * aunque el cronómetro no estuviera corriendo, causando que el primer
 * delta al iniciar fuera el tiempo desde el último frame, no desde el
 * momento en que se llamó run().
 */
public class Cronometer {

    private long startTime;
    private long duration;
    private boolean running;

    public Cronometer() {
        running = false;
    }

    /**
     * Inicia el cronómetro por 'millis' milisegundos.
     */
    public void run(long millis) {
        this.duration  = millis;
        this.startTime = System.currentTimeMillis(); // FIX: captura el tiempo AHORA
        this.running   = true;
    }

    /**
     * Actualiza el estado del cronómetro.
     * Llama cada frame.
     */
    public void update() {
        if (!running) return;

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= duration) {
            running = false;
        }
    }

    /** @return true si el cronómetro todavía está corriendo. */
    public boolean isRunning() {
        return running;
    }

    /** @return milisegundos transcurridos desde que se inició. */
    public long getElapsed() {
        if (!running) return 0;
        return System.currentTimeMillis() - startTime;
    }

    /** Detiene el cronómetro manualmente. */
    public void stop() {
        running = false;
    }
}
