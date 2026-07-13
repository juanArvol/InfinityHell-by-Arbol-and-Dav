package Display.State;

/**
 * Estado formal del ciclo de vida de la superficie de render.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN ORIGINAL
 *
 * El estado de la BufferStrategy era implícito: se deducía de bsRef != null
 * y de flags sueltos como needsRecreation. SurfaceState formaliza el ciclo
 * de vida en estados distinguibles para que toda decisión sobre la superficie
 * se base en el estado, no en null checks.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HRFC-002: ADICIÓN DE SUSPENDED
 *
 * Problema anterior:
 *   Los eventos de pérdida de foco (Alt+Tab, windowDeactivated) y de
 *   minimización no tenían representación en el ciclo de vida de la surface.
 *   La surface permanecía en READY durante toda la deactivación, y el
 *   GameLoop seguía intentando renderizar sobre una BufferStrategy que el
 *   OS puede haber invalidado (especialmente en FULLSCREEN_EXCLUSIVE).
 *
 *   RecreateBufferStrategy se encolaba al deiconificar, pero en Alt+Tab sin
 *   iconificación (BORDERLESS_FULLSCREEN) no se encolaba nada, dejando al
 *   GameLoop en un estado indeterminado entre pérdida y recuperación de foco.
 *
 * Solución: estado SUSPENDED.
 *   SUSPENDED señala que la ventana perdió activación o foco pero la BS puede
 *   seguir siendo válida. El GameLoop descarta frames silenciosamente.
 *   La surface NO se destruye: si la BS sigue válida al recuperar foco, la
 *   transición a READY es inmediata sin coste de reconstrucción.
 *   Si la BS no es válida (contentsLost o peer destruido), la transición de
 *   ResumeRendering reconstruye antes de volver a READY.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * TRANSICIONES VÁLIDAS
 *
 *   READY        → RECREATING   (resize / transición explícita / build iniciado)
 *   READY        → SUSPENDED    (pérdida de foco / windowDeactivated)
 *   READY        → LOST         (contentsLost detectado por GameLoop)
 *   SUSPENDED    → RECREATING   (ResumeRendering con requiresRebuild=true)
 *   SUSPENDED    → READY        (ResumeRendering con BS todavía válida)
 *   SUSPENDED    → LOST         (contentsLost detectado mientras suspendido)
 *   LOST         → RECREATING   (RecreateBufferStrategy / ResumeRendering)
 *   RECREATING   → READY        (build exitoso)
 *   RECREATING   → FAILED       (build fallido tras intentos de recuperación)
 *   FAILED       → RECREATING   (reintento programado)
 *   Cualquiera   → READY        (nueva build exitosa)
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CONTRATO CON ReadinessGate
 *
 * La ReadinessGate en SurfacePublisher está ABIERTA solo cuando:
 *   surfaceState == READY
 *
 * En todos los demás estados (SUSPENDED, LOST, RECREATING, FAILED) la gate
 * está CERRADA y acquireFrame() retorna null → drop silencioso en GameLoop.
 * Esto garantiza que el GameLoop nunca accede a recursos parcialmente
 * reconstruidos o invalidados.
 */
public enum SurfaceState {

    /**
     * BufferStrategy disponible y válida.
     * El sistema puede renderizar y presentar frames normalmente.
     * La ReadinessGate está ABIERTA.
     */
    READY,

    /**
     * La ventana perdió activación o foco del sistema operativo.
     *
     * La BufferStrategy puede seguir siendo válida en memoria pero el OS
     * no garantiza la integridad de la presentación. El GameLoop descarta
     * frames silenciosamente. La BS no se destruye: si sigue siendo válida
     * al recuperar foco, la transición a READY es inmediata.
     *
     * Cubre: Alt+Tab, windowDeactivated, windowLostFocus, windowIconified.
     * La ReadinessGate está CERRADA.
     */
    SUSPENDED,

    /**
     * BufferStrategy invalidada (contentsLost, dispose externo, o build fallido).
     * No se puede renderizar. Se requiere recreación explícita.
     * La ReadinessGate está CERRADA.
     */
    LOST,

    /**
     * Recreación de BufferStrategy en progreso en el EDT.
     * El GameLoop descarta frames hasta que el estado sea READY.
     * La ReadinessGate está CERRADA.
     */
    RECREATING,

    /**
     * La recreación ha fallado definitivamente tras intentos de recuperación.
     * Requiere intervención: el sistema registra el error y programa un reintento
     * con backoff para evitar bucles infinitos.
     * La ReadinessGate está CERRADA.
     */
    FAILED;

    /**
     * True si la superficie está disponible para renderizar.
     * Solo READY devuelve true.
     */
    public boolean isRenderable() {
        return this == READY;
    }

    /**
     * True si se debe descartar el frame actual (no renderizar).
     * Todo estado excepto READY requiere descarte.
     */
    public boolean shouldDropFrame() {
        return this != READY;
    }

    /**
     * True si la ventana está en un estado de baja actividad.
     * Cubre SUSPENDED: la BS puede estar viva pero la presentación no está garantizada.
     */
    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    /**
     * True si hay un proceso de reconstrucción activo o pendiente.
     */
    public boolean isRebuilding() {
        return this == RECREATING;
    }

    /**
     * True si la surface necesita una intervención activa para volver a READY.
     * Incluye LOST, RECREATING y FAILED — todos requieren una acción del pipeline.
     * SUSPENDED NO está incluido: puede volver a READY sin reconstrucción.
     */
    public boolean needsIntervention() {
        return this == LOST || this == RECREATING || this == FAILED;
    }
}
