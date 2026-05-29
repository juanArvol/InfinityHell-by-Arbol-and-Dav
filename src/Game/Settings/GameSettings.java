package Game.Settings;

/**
 * Configuración global del juego en runtime.
 *
 * Singleton inmutable en construcción, con campos mutables controlados.
 *
 * ─── BUG CORREGIDO ────────────────────────────────────────────────────────────
 *
 * BUG-SETTINGS-VISIBILITY · debugEnabled y fpsEnabled no eran volatile
 *   CAUSA: GameSettings es leído desde el GameLoop thread (isFpsEnabled(),
 *          isDebugEnabled() en cada frame) y escrito desde el GameLoop thread
 *          también (toggleFps() vía KeyActionListener en keyboard.update()).
 *          En este caso concreto ambas operaciones ocurren en el mismo thread,
 *          por lo que no hay race condition de escritura simultánea.
 *
 *          SIN EMBARGO: sin volatile, el compilador JIT y la CPU pueden reordenar
 *          lecturas/escrituras y mantener valores en registros de CPU. Si en el
 *          futuro se añade un thread de UI o un hilo de red que modifique estas
 *          flags, la ausencia de volatile causaría que el GameLoop nunca viera
 *          el cambio (lectura de valor stale cacheado en registro).
 *
 *          Además, toggleFps() tiene un bug de atomicidad: la operación
 *          `this.fpsEnabled = !this.fpsEnabled` es un read-modify-write no atómico.
 *          Si dos threads la llaman simultáneamente pueden ambos leer el mismo valor
 *          inicial y escribir el mismo resultado (toggle se pierde).
 *
 *   SOLUCIÓN:
 *     · volatile en los campos boolean garantiza visibilidad entre threads.
 *     · toggleFps() usa synchronized(this) para atomicidad del read-modify-write.
 *       El bloque es de 1 instrucción → contención prácticamente nula.
 *     · setters mantienen volatile write → visibilidad garantizada.
 *
 *   RIESGO: ninguno. volatile y synchronized son mecanismos estándar del JDK.
 *           No cambian el comportamiento observable en el caso de uso actual
 *           (un solo thread), pero protegen contra futuros usos multi-thread.
 *
 *   COMPATIBILIDAD FUTURA 2D/3D: relevante. Un sistema 3D con hilo de render
 *           separado (ej: OpenGL en su propio thread) podría leer estas flags
 *           para decidir si mostrar debug overlays. volatile lo hace seguro.
 */
public class GameSettings {

    private static final GameSettings instance = new GameSettings();

    /**
     * BUG-SETTINGS-VISIBILITY FIX: volatile garantiza visibilidad entre threads.
     * GameLoop lee, posibles threads futuros (UI, red) escriben.
     */
    private volatile boolean debugEnabled = false;
    private volatile boolean fpsEnabled   = false;

    private GameSettings() {}

    public static GameSettings getInstance() {
        return instance;
    }

    // ── Debug ─────────────────────────────────────────────────────────────────

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean value) {
        this.debugEnabled = value;
    }

    // ── FPS counter ───────────────────────────────────────────────────────────

    /** Devuelve true si el contador de FPS debe mostrarse en pantalla. */
    public boolean isFpsEnabled() {
        return fpsEnabled;
    }

    /** Activa o desactiva el contador de FPS en pantalla. */
    public void setFpsEnabled(boolean value) {
        this.fpsEnabled = value;
    }

    /**
     * Toggle: alterna el estado del FPS counter.
     *
     * BUG-SETTINGS-VISIBILITY FIX: synchronized garantiza atomicidad del
     * read-modify-write. Sin esto, dos threads concurrentes podrían leer el
     * mismo valor y aplicar el toggle dos veces con el mismo resultado.
     */
    public synchronized void toggleFps() {
        this.fpsEnabled = !this.fpsEnabled;
    }

    /**
     * Toggle: alterna el estado de debug.
     * Mismo razonamiento que toggleFps().
     */
    public synchronized void toggleDebug() {
        this.debugEnabled = !this.debugEnabled;
    }
}
