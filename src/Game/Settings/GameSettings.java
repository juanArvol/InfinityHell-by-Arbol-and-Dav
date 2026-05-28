package Game.Settings;

public class GameSettings {

    private static final GameSettings instance = new GameSettings();

    private boolean debugEnabled  = false;
    private boolean fpsEnabled    = false;   // FPS counter (toggle desde settings)

    private GameSettings(){}

    public static GameSettings getInstance() {
        return instance;
    }

    // ── Debug ─────────────────────────────────────────────────────────────────

    public boolean isDebugEnabled() { return debugEnabled; }

    public void setDebugEnabled(boolean value) { this.debugEnabled = value; }

    // ── FPS counter ───────────────────────────────────────────────────────────

    /** Devuelve true si el contador de FPS debe mostrarse en pantalla. */
    public boolean isFpsEnabled() { return fpsEnabled; }

    /** Activa o desactiva el contador de FPS en pantalla. */
    public void setFpsEnabled(boolean value) { this.fpsEnabled = value; }

    /** Toggle convenience: alterna el estado del FPS counter. */
    public void toggleFps() { this.fpsEnabled = !this.fpsEnabled; }
}
