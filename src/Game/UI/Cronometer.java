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
    private long endTime;   // FIX B-01: guardamos el instante real de expiración
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
        this.startTime = System.currentTimeMillis();
        this.endTime   = 0;
        this.running   = true;
    }

    /**
     * Actualiza el estado del cronómetro.
     * Llama cada frame.
     */
    public void update() {
        if (!running) return;

        long now     = System.currentTimeMillis();
        long elapsed = now - startTime;
        if (elapsed >= duration) {
            endTime = now;   // capturamos el momento exacto de expiración
            running = false;
        }
    }

    /** @return true si el cronómetro todavía está corriendo. */
    public boolean isRunning() {
        return running;
    }

    /**
     * Milisegundos transcurridos desde que se inició.
     *
     * FIX B-01: la versión anterior retornaba 0 cuando el cronómetro había
     * expirado, confundiendo a los callers que usaban getElapsed() para
     * animaciones post-expiración (fade-outs, efectos de UI).
     *
     * Ahora:
     *  - Si está corriendo  → tiempo real desde startTime.
     *  - Si expiró          → tiempo total transcurrido hasta la expiración
     *                         (endTime - startTime ≈ duration).
     *  - Si nunca arrancó   → 0.
     */
    public long getElapsed() {
        if (running)    return System.currentTimeMillis() - startTime;
        if (endTime > 0) return endTime - startTime;
        return 0;
    }

    /** Detiene el cronómetro manualmente. */
    public void stop() {
        if (running) {
            endTime = System.currentTimeMillis();
            running = false;
        }
    }
}
