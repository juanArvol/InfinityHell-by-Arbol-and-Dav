package Display.State;

/**
 * Estado del ciclo de vida de la superficie de render (BufferStrategy).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * MOTIVACIÓN
 *
 * El estado de la BufferStrategy era implícito: se deducía de bsRef != null
 * y de flags sueltos como needsRecreation. Esto generaba:
 *
 *   1. Recreaciones no controladas activadas por múltiples fuentes.
 *   2. Bucles infinitos potenciales si createBufferStrategy() falla.
 *   3. Ambigüedad entre "surface perdida" y "surface nunca creada".
 *
 * SurfaceState formaliza el ciclo de vida en cuatro estados distinguibles.
 * Toda decisión sobre la superficie se basa en el estado, no en null checks.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * TRANSICIONES VÁLIDAS
 *
 *   READY        → LOST         (contentsLost durante uso)
 *   READY        → RECREATING   (resize / transición explícita)
 *   LOST         → RECREATING   (GameLoop solicita recreación)
 *   RECREATING   → READY        (createBufferStrategy() exitoso)
 *   RECREATING   → FAILED       (createBufferStrategy() falla tras retries)
 *   FAILED       → RECREATING   (reintentar después de un tiempo)
 *   Cualquiera   → READY        (nueva creación exitosa)
 */
public enum SurfaceState {

    /**
     * BufferStrategy disponible y válida.
     * El sistema puede renderizar y presentar frames normalmente.
     */
    READY,

    /**
     * BufferStrategy invalidada (contentsLost o dispose externo).
     * No se puede renderizar. Se requiere recreación.
     */
    LOST,

    /**
     * Recreación de BufferStrategy en progreso en el EDT.
     * El GameLoop debe descartar frames hasta que el estado sea READY.
     */
    RECREATING,

    /**
     * La recreación ha fallado definitivamente (canvas no displayable,
     * error de peer AWT, o se ha alcanzado el límite de reintentos).
     * Requiere intervención: el sistema registra el error y no reintenta
     * automáticamente para evitar bucles infinitos.
     */
    FAILED;

    /** True si la superficie está disponible para renderizar. */
    public boolean isRenderable() {
        return this == READY;
    }

    /** True si se debe descartar el frame actual. */
    public boolean shouldDropFrame() {
        return this != READY;
    }
}
