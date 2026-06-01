package Display.Transition;

/**
 * Estado formal de transición del subsistema Display.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * TransitionLock (boolean atómico) garantiza exclusión mutua pero no sabe
 * QUÉ transición está ejecutándose. Esto impide:
 *   - Diagnóstico preciso durante fallos.
 *   - Bloqueo selectivo de operaciones incompatibles.
 *   - Razonamiento sobre la transición en curso desde cualquier subsistema.
 *
 * DisplayTransitionState convierte el boolean en un enum exhaustivo.
 * Cada transición tiene un tipo conocido en todo momento.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * INVARIANTE
 *
 * Solo puede haber un estado activo a la vez. IDLE es el único estado
 * compatible con el inicio de cualquier nueva transición.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * EXTENSIBILIDAD
 *
 * Agregar una nueva transición requiere únicamente añadir una constante aquí.
 * DisplayTransitionMachine.tryBegin() valida la compatibilidad centralizada.
 */
public enum DisplayTransitionState {

    /** Sin transición activa. El sistema está estable. */
    IDLE,

    /** Transición hacia FULLSCREEN_EXCLUSIVE en progreso. */
    ENTERING_FULLSCREEN,

    /** Transición desde FULLSCREEN_EXCLUSIVE hacia WINDOWED en progreso. */
    LEAVING_FULLSCREEN,

    /** Transición hacia BORDERLESS_FULLSCREEN en progreso. */
    ENTERING_BORDERLESS,

    /** Transición desde BORDERLESS_FULLSCREEN hacia WINDOWED en progreso. */
    LEAVING_BORDERLESS,

    /** Cambio de resolución virtual en progreso. */
    CHANGING_RESOLUTION,

    /** Cambio de monitor activo en progreso. */
    CHANGING_MONITOR,

    /**
     * Reconfiguración general del display en progreso.
     * Cubre operaciones compuestas (p. ej. cambio de monitor + resolución).
     */
    RECONFIGURING_DISPLAY;

    /** True si esta transición implica salir de cualquier modo fullscreen. */
    public boolean isLeavingFullscreen() {
        return this == LEAVING_FULLSCREEN || this == LEAVING_BORDERLESS;
    }

    /** True si esta transición implica entrar en cualquier modo fullscreen. */
    public boolean isEnteringFullscreen() {
        return this == ENTERING_FULLSCREEN || this == ENTERING_BORDERLESS;
    }

    /** True si hay alguna transición activa (no IDLE). */
    public boolean isActive() {
        return this != IDLE;
    }
}
