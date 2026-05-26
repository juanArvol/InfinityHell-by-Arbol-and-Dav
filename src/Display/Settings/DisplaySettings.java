package Display.Settings;

/**
 * Configuración centralizada del sistema de display.
 *
 * Contiene TODOS los parámetros que afectan resolución, fullscreen,
 * escalado y render. Nunca dispersar estas constantes entre clases.
 *
 * Uso: pasar instancia por inyección de dependencias.
 * NO usar como singleton — el DisplayManager lo gestiona.
 */
public class DisplaySettings {

    // ─── Resolución Virtual ────────────────────────────────────────────────────
    /** Ancho lógico fijo. TODA la lógica del juego usa este valor. */
    public final int virtualWidth;

    /** Alto lógico fijo. TODA la lógica del juego usa este valor. */
    public final int virtualHeight;

    // ─── Ventana ───────────────────────────────────────────────────────────────
    /** Título de la ventana. */
    public final String windowTitle;

    /** Ancho inicial de ventana en modo windowed. */
    public final int windowedWidth;

    /** Alto inicial de ventana en modo windowed. */
    public final int windowedHeight;

    // ─── Fullscreen ────────────────────────────────────────────────────────────
    /** Si arranca en fullscreen real (GraphicsDevice.setFullScreenWindow). */
    public final boolean startFullscreen;

    /** Índice del monitor a usar (0 = monitor principal). */
    public final int monitorIndex;

    // ─── Escalado ──────────────────────────────────────────────────────────────
    /** Modo de escalado del framebuffer virtual a pantalla física. */
    public final ScalingMode scalingMode;

    /** Factor de escala de la UI (independiente del escalado del juego). */
    public final float uiScale;

    // ─── Render ────────────────────────────────────────────────────────────────
    /** Frames por segundo objetivo. */
    public final int targetFps;

    /** Color de las barras de letterbox/pillarbox (negro por defecto). */
    public final java.awt.Color letterboxColor;

    // ─── Hints de calidad ──────────────────────────────────────────────────────
    /**
     * Si activar interpolación bilineal al escalar (suaviza el juego).
     * Para pixel art: false. Para HD: true.
     */
    public final boolean useInterpolation;

    // ───────────────────────────────────────────────────────────────────────────

    private DisplaySettings(Builder b) {
        this.virtualWidth     = b.virtualWidth;
        this.virtualHeight    = b.virtualHeight;
        this.windowTitle      = b.windowTitle;
        this.windowedWidth    = b.windowedWidth;
        this.windowedHeight   = b.windowedHeight;
        this.startFullscreen  = b.startFullscreen;
        this.monitorIndex     = b.monitorIndex;
        this.scalingMode      = b.scalingMode;
        this.uiScale          = b.uiScale;
        this.targetFps        = b.targetFps;
        this.letterboxColor   = b.letterboxColor;
        this.useInterpolation = b.useInterpolation;
    }

    /** Aspect ratio virtual calculado (ej: 1.777 para 16:9). */
    public float getVirtualAspectRatio() {
        return (float) virtualWidth / virtualHeight;
    }

    // ─── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int         virtualWidth     = 1280;
        private int         virtualHeight    = 720;
        private String      windowTitle      = "Game";
        private int         windowedWidth    = 1280;
        private int         windowedHeight   = 720;
        private boolean     startFullscreen  = false;
        private int         monitorIndex     = 0;
        private ScalingMode scalingMode      = ScalingMode.FIT;
        private float       uiScale          = 1.0f;
        private int         targetFps        = 60;
        private java.awt.Color letterboxColor = java.awt.Color.BLACK;
        private boolean     useInterpolation = false; // false = pixel-perfect por defecto

        public Builder virtualResolution(int w, int h) { virtualWidth = w; virtualHeight = h; return this; }
        public Builder windowTitle(String t)            { windowTitle = t; return this; }
        public Builder windowedSize(int w, int h)       { windowedWidth = w; windowedHeight = h; return this; }
        public Builder startFullscreen(boolean v)       { startFullscreen = v; return this; }
        public Builder monitorIndex(int i)              { monitorIndex = i; return this; }
        public Builder scalingMode(ScalingMode m)       { scalingMode = m; return this; }
        public Builder uiScale(float s)                 { uiScale = s; return this; }
        public Builder targetFps(int fps)               { targetFps = fps; return this; }
        public Builder letterboxColor(java.awt.Color c) { letterboxColor = c; return this; }
        public Builder useInterpolation(boolean v)      { useInterpolation = v; return this; }

        public DisplaySettings build() {
            if (virtualWidth <= 0 || virtualHeight <= 0)
                throw new IllegalStateException("virtualResolution must be > 0");
            if (targetFps <= 0)
                throw new IllegalStateException("targetFps must be > 0");
            return new DisplaySettings(this);
        }
    }
}
