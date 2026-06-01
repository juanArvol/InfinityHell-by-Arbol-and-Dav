package Main.Debug;

/**
 * Configuración global del juego en runtime.
 *
 * Singleton inmutable en construcción, con campos mutables controlados.
**/

public class DebugGameSettings {

    private static final DebugGameSettings instance = new DebugGameSettings();

    /**
     * GameLoop lee, posibles threads futuros (UI, red) escriben.
     */
    private volatile boolean debugEnabled = false;
    private volatile boolean fpsEnabled   = false;

    private DebugGameSettings() {}

    public static DebugGameSettings getInstance() {
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

    public synchronized void toggleFps() {
        this.fpsEnabled = !this.fpsEnabled;
    }

    public synchronized void toggleDebug() {
        this.debugEnabled = !this.debugEnabled;
    }
}
