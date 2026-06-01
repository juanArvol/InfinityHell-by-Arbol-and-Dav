package Display.Settings;

import Display.Background.DisplayBackground;
import Display.Background.SolidColorBackground;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;

/**
 * Configuración inmutable del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAMBIOS RESPECTO A LA VERSIÓN ANTERIOR
 *
 * 1. letterboxColor → fillColor + background (separación de conceptos):
 *
 *    Antes: un solo campo Color letterboxColor controlaba tanto el color
 *    de las barras de presentación como implícitamente el fondo del frame.
 *    Eran dos cosas distintas representadas por una sola variable.
 *
 *    Ahora:
 *      - fillColor       : Color de las barras de letterbox/pillarbox.
 *                          Va al ViewportManager → ViewportCalculator → FillArea.
 *      - background      : DisplayBackground que limpia el framebuffer virtual
 *                          antes de cada frame. Va al RenderSurfaceManager.
 *
 *    Valor por defecto de ambos: negro (comportamiento idéntico al original).
 *
 * 2. Compatibilidad:
 *    El builder ofrece letterboxColor() como alias de fillColor() para
 *    no romper configuraciones existentes. Está marcado @Deprecated para
 *    guiar la migración hacia fillColor() y background().
 *
 * ──────────────────────────────────────────────────────────────────────────
 * USO
 *
 *   DisplaySettings settings = DisplaySettings.builder()
 *       .virtualResolution(1280, 720)
 *       .windowedSize(1280, 720)
 *       .scalingMode(ScalingMode.FIT)
 *       .fillColor(Color.BLACK)                          // barras de relleno
 *       .background(new SolidColorBackground(Color.BLACK)) // fondo del frame
 *       .build();
 */
public final class DisplaySettings {

    // Resolución virtual
    public final int virtualWidth;
    public final int virtualHeight;

    // Ventana
    public final String    windowTitle;
    public final int       windowedWidth;
    public final int       windowedHeight;
    public final boolean   windowResizable;
    public final boolean   windowDecorated;
    public final Cursor    cursor;
    public final Dimension minimumWindowSize;
    public final Dimension maximumWindowSize;

    // Fullscreen
    public final boolean startFullscreen;
    public final int     monitorIndex;

    // Escalado y presentación
    public final ScalingMode       scalingMode;

    /**
     * Color de las barras de relleno (letterbox / pillarbox).
     * Configura el color de los FillArea en ViewportInfo.
     */
    public final Color fillColor;

    /**
     * Fondo del framebuffer virtual.
     * Se aplica al inicio de cada frame antes de renderizar la escena.
     * Puede ser SolidColorBackground, GradientBackground, etc.
     */
    public final DisplayBackground background;

    public final boolean useInterpolation;

    // Render
    public final int targetFps;
    public final int bufferCount;

    private DisplaySettings(Builder b) {
        this.virtualWidth      = b.virtualWidth;
        this.virtualHeight     = b.virtualHeight;
        this.windowTitle       = b.windowTitle;
        this.windowedWidth     = b.windowedWidth;
        this.windowedHeight    = b.windowedHeight;
        this.windowResizable   = b.windowResizable;
        this.windowDecorated   = b.windowDecorated;
        this.cursor            = b.cursor;
        this.minimumWindowSize = b.minimumWindowSize;
        this.maximumWindowSize = b.maximumWindowSize;
        this.startFullscreen   = b.startFullscreen;
        this.monitorIndex      = b.monitorIndex;
        this.scalingMode       = b.scalingMode;
        this.fillColor         = b.fillColor;
        this.background        = b.background;
        this.useInterpolation  = b.useInterpolation;
        this.targetFps         = b.targetFps;
        this.bufferCount       = b.bufferCount;
    }

    public float getVirtualAspectRatio() {
        return (float) virtualWidth / virtualHeight;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private int    virtualWidth  = 1280;
        private int    virtualHeight = 720;

        private String    windowTitle       = "Game";
        private int       windowedWidth     = 1280;
        private int       windowedHeight    = 720;
        private boolean   windowResizable   = true;
        private boolean   windowDecorated   = true;
        private Cursor    cursor            = null;
        private Dimension minimumWindowSize = new Dimension(320, 180);
        private Dimension maximumWindowSize = null;

        private boolean startFullscreen = false;
        private int     monitorIndex    = 0;

        private ScalingMode       scalingMode      = ScalingMode.FIT;
        private Color             fillColor        = Color.BLACK;
        private DisplayBackground background       = SolidColorBackground.BLACK;
        private boolean           useInterpolation = false;

        private int targetFps   = 60;
        private int bufferCount = 3;

        public Builder virtualResolution(int w, int h)  { this.virtualWidth = w; this.virtualHeight = h; return this; }
        public Builder windowTitle(String t)             { this.windowTitle = t;         return this; }
        public Builder windowedSize(int w, int h)        { this.windowedWidth = w; this.windowedHeight = h; return this; }
        public Builder windowResizable(boolean v)        { this.windowResizable = v;     return this; }
        public Builder windowDecorated(boolean v)        { this.windowDecorated = v;     return this; }
        public Builder cursor(Cursor c)                  { this.cursor = c;               return this; }
        public Builder minimumWindowSize(int w, int h)   { this.minimumWindowSize = new Dimension(w, h); return this; }
        public Builder maximumWindowSize(int w, int h)   { this.maximumWindowSize = new Dimension(w, h); return this; }
        public Builder startFullscreen(boolean v)        { this.startFullscreen = v;     return this; }
        public Builder monitorIndex(int i)               { this.monitorIndex = i;         return this; }
        public Builder scalingMode(ScalingMode m)        { this.scalingMode = m;          return this; }
        public Builder useInterpolation(boolean v)       { this.useInterpolation = v;     return this; }
        public Builder targetFps(int fps)                { this.targetFps = fps;          return this; }
        public Builder bufferCount(int n)                { this.bufferCount = n;          return this; }

        /**
         * Color de las barras de relleno (letterbox / pillarbox).
         * Independiente del color de fondo del framebuffer.
         */
        public Builder fillColor(Color c) {
            this.fillColor = c != null ? c : Color.BLACK;
            return this;
        }

        /**
         * Fondo del framebuffer virtual antes de cada frame.
         * Implementación por defecto: SolidColorBackground.BLACK.
         * Se puede cambiar a gradiente, textura, etc.
         */
        public Builder background(DisplayBackground bg) {
            this.background = bg != null ? bg : SolidColorBackground.BLACK;
            return this;
        }

        /**
         * Atajo: fija el fondo como un SolidColorBackground del color dado.
         * Equivalente a {@code background(new SolidColorBackground(c))}.
         */
        public Builder backgroundColor(Color c) {
            return background(new SolidColorBackground(c != null ? c : Color.BLACK));
        }

        /**
         * @deprecated Usar {@link #fillColor(Color)} para las barras de relleno
         *             y {@link #backgroundColor(Color)} para el fondo del frame.
         *             Mantenido por compatibilidad; aplica ambos al mismo color.
         */
        @Deprecated(since = "refactor-display-v2", forRemoval = true)
        public Builder letterboxColor(Color c) {
            fillColor(c);
            backgroundColor(c);
            return this;
        }

        public DisplaySettings build() {
            if (virtualWidth  <= 0) throw new IllegalStateException("virtualWidth must be > 0");
            if (virtualHeight <= 0) throw new IllegalStateException("virtualHeight must be > 0");
            if (targetFps     <= 0) throw new IllegalStateException("targetFps must be > 0");
            if (bufferCount < 2 || bufferCount > 4)
                throw new IllegalStateException("bufferCount must be 2, 3, or 4");
            return new DisplaySettings(this);
        }
    }
}
