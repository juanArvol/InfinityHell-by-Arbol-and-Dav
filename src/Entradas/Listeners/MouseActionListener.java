package Entradas.Listeners;

/**
 * Listener de acciones de ratón del juego.
 *
 * Misma filosofía que KeyActionListener: eventos semánticos, desacoplados
 * del AWT subyacente. Los suscriptores solo implementan lo que necesitan.
 *
 * DISEÑO:
 *  - Eventos de click son edge-triggered (se disparan una vez por press).
 *  - Los estados continuos (leftPressed, rightPressed, mouseX/Y) siguen
 *    disponibles como consulta directa en MouseInput para sistemas que los
 *    necesiten cada frame (p.ej. la dirección del aim del player).
 *  - Las coordenadas de mouse se entregan ya transformadas a espacio VIRTUAL
 *    mediante ViewportInfo, para que los suscriptores no necesiten conocer
 *    la resolución real del monitor.
 *
 * CUÁNDO USAR LISTENER vs CONSULTA DIRECTA:
 *  - Listener   → clicks puntuales: disparar (si es edge), interactuar, menú.
 *  - Consulta   → estado continuo: aim direction, rightPressed para zoom/apuntar.
 */
public interface MouseActionListener {

    // ─── Botón izquierdo ──────────────────────────────────────────────────────

    /**
     * Botón izquierdo pulsado (edge).
     *
     * @param virtualX coordenada X en espacio virtual del juego
     * @param virtualY coordenada Y en espacio virtual del juego
     */
    default void onLeftClick(float virtualX, float virtualY) {}

    /** Botón izquierdo liberado. */
    default void onLeftRelease(float virtualX, float virtualY) {}

    // ─── Botón derecho ────────────────────────────────────────────────────────

    /**
     * Botón derecho pulsado (edge) — generalmente "apuntar" / aim.
     *
     * @param virtualX coordenada X en espacio virtual del juego
     * @param virtualY coordenada Y en espacio virtual del juego
     */
    default void onRightClick(float virtualX, float virtualY) {}

    /** Botón derecho liberado. */
    default void onRightRelease(float virtualX, float virtualY) {}

    // ─── Rueda ────────────────────────────────────────────────────────────────

    /**
     * Rueda del ratón girada.
     *
     * @param delta positivo = scroll hacia abajo / zoom out,
     *              negativo = scroll hacia arriba / zoom in
     *              (convención estándar AWT MouseWheelEvent)
     */
    default void onScroll(int delta) {}

    // ─── Movimiento ───────────────────────────────────────────────────────────

    /**
     * El ratón se movió (incluye drag).
     * Llamado cada frame en que la posición cambió.
     *
     * @param virtualX nueva X en espacio virtual
     * @param virtualY nueva Y en espacio virtual
     */
    default void onMouseMoved(float virtualX, float virtualY) {}
}
