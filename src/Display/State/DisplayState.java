package Display.State;

import Display.ViewportInfo;

/**
 * Snapshot inmutable y completo del estado del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * EVOLUCIÓN RESPECTO A LA VERSIÓN ANTERIOR
 *
 * DisplayState anterior contenía: mode, realWidth, realHeight, surfaceReady.
 * Era suficiente para el GameLoop pero insuficiente para:
 *
 *   - Conocer la resolución virtual (necesaria para escalar inputs).
 *   - Saber qué transición está en curso (diagnóstico y coordinación).
 *   - Conocer el estado formal de la superficie (READY/LOST/RECREATING/FAILED).
 *   - Acceder al viewport sin consultar ViewportManager por separado.
 *   - Identificar el monitor activo.
 *
 * La versión expandida incorpora todos estos campos y los publica
 * atómicamente como un snapshot coherente.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * COMPATIBILIDAD
 *
 * Los campos originales se mantienen con los mismos nombres y semántica.
 * El constructor de 4 parámetros original sigue siendo válido (marcado
 * @Deprecated para guiar la migración al constructor completo).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * THREAD SAFETY
 *
 * Inmutable. Seguro leer desde cualquier thread sin sincronización.
 * Se publica mediante una sola escritura volatile en DisplayManager.
 */
public final class DisplayState {

    // ── Campos originales (compatibilidad) ───────────────────────────────────

    /** Modo de presentación activo. */
    public final DisplayMode mode;

    /** Ancho real del canvas en el momento de este snapshot. */
    public final int realWidth;

    /** Alto real del canvas en el momento de este snapshot. */
    public final int realHeight;

    /**
     * True si el BufferStrategy está disponible para render.
     * @deprecated Usar {@link #surfaceState} para información más precisa.
     */
    @Deprecated(since = "display-refactor-v3")
    public final boolean surfaceReady;

    // ── Campos nuevos ────────────────────────────────────────────────────────

    /** Resolución virtual del juego (constante durante la sesión normalmente). */
    public final Resolution virtualResolution;

    /** Resolución real actual (tamaño del canvas). */
    public final Resolution realResolution;

    /**
     * Viewport calculado para este estado.
     * Null si la superficie no está inicializada aún.
     */
    public final ViewportInfo viewport;

    /** Estado formal del ciclo de vida de la superficie de render. */
    public final SurfaceState surfaceState;

    /** Estado de transición activo en el momento de este snapshot. */
    public final Display.Transition.DisplayTransitionState transitionState;

    /** Índice del monitor activo. */
    public final int activeMonitorIndex;

    // ── Constructor completo ─────────────────────────────────────────────────

    public DisplayState(DisplayMode mode,
                        int realWidth, int realHeight,
                        Resolution virtualResolution,
                        ViewportInfo viewport,
                        SurfaceState surfaceState,
                        Display.Transition.DisplayTransitionState transitionState,
                        int activeMonitorIndex) {
        this.mode              = mode;
        this.realWidth         = realWidth;
        this.realHeight        = realHeight;
        this.surfaceReady      = surfaceState == SurfaceState.READY;
        this.virtualResolution = virtualResolution != null
                                   ? virtualResolution
                                   : new Resolution(Math.max(1, realWidth), Math.max(1, realHeight));
        this.realResolution    = new Resolution(Math.max(1, realWidth), Math.max(1, realHeight));
        this.viewport          = viewport;
        this.surfaceState      = surfaceState != null ? surfaceState : SurfaceState.LOST;
        this.transitionState   = transitionState != null
                                   ? transitionState
                                   : Display.Transition.DisplayTransitionState.IDLE;
        this.activeMonitorIndex = activeMonitorIndex;
    }

    // ── Constructor de compatibilidad (4 parámetros originales) ─────────────

    /**
     * Constructor de compatibilidad con la versión anterior de DisplayState.
     * Rellena los campos nuevos con valores por defecto seguros.
     *
     * @deprecated Usar el constructor completo para garantizar consistencia total.
     */
    @Deprecated(since = "display-refactor-v3")
    public DisplayState(DisplayMode mode, int realWidth, int realHeight, boolean surfaceReady) {
        this(mode,
             realWidth, realHeight,
             new Resolution(Math.max(1, realWidth), Math.max(1, realHeight)),
             null,
             surfaceReady ? SurfaceState.READY : SurfaceState.LOST,
             Display.Transition.DisplayTransitionState.IDLE,
             0);
    }

    // ── Conveniencias ────────────────────────────────────────────────────────

    /** True si el modo actual es cualquier forma de fullscreen. */
    public boolean isFullscreen() {
        return mode.isFullscreen();
    }

    /** True si la superficie está lista para renderizar. */
    public boolean isSurfaceReady() {
        return surfaceState == SurfaceState.READY;
    }

    /** True si hay alguna transición activa. */
    public boolean isTransitionActive() {
        return transitionState.isActive();
    }

    /** True si el viewport está disponible (no null). */
    public boolean hasViewport() {
        return viewport != null;
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    /**
     * Crea un Builder inicializado con los valores de este snapshot.
     * Facilita la publicación de estados derivados del anterior.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private DisplayMode                                    mode;
        private int                                            realWidth;
        private int                                            realHeight;
        private Resolution                                     virtualResolution;
        private ViewportInfo                                   viewport;
        private SurfaceState                                   surfaceState;
        private Display.Transition.DisplayTransitionState      transitionState;
        private int                                            activeMonitorIndex;

        private Builder(DisplayState s) {
            this.mode               = s.mode;
            this.realWidth          = s.realWidth;
            this.realHeight         = s.realHeight;
            this.virtualResolution  = s.virtualResolution;
            this.viewport           = s.viewport;
            this.surfaceState       = s.surfaceState;
            this.transitionState    = s.transitionState;
            this.activeMonitorIndex = s.activeMonitorIndex;
        }

        public Builder mode(DisplayMode m)              { this.mode = m;               return this; }
        public Builder realSize(int w, int h)           { this.realWidth = w; this.realHeight = h; return this; }
        public Builder virtualResolution(Resolution r)  { this.virtualResolution = r;  return this; }
        public Builder viewport(ViewportInfo vp)        { this.viewport = vp;          return this; }
        public Builder surfaceState(SurfaceState ss)    { this.surfaceState = ss;       return this; }
        public Builder transitionState(
            Display.Transition.DisplayTransitionState t) { this.transitionState = t;   return this; }
        public Builder activeMonitorIndex(int i)        { this.activeMonitorIndex = i; return this; }

        public DisplayState build() {
            return new DisplayState(mode, realWidth, realHeight,
                                    virtualResolution, viewport, surfaceState,
                                    transitionState, activeMonitorIndex);
        }
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "DisplayState[mode=%s real=%s virtual=%s surface=%s transition=%s monitor=%d vp=%s]",
            mode, realResolution, virtualResolution, surfaceState,
            transitionState, activeMonitorIndex,
            viewport != null ? viewport : "null"
        );
    }
}
