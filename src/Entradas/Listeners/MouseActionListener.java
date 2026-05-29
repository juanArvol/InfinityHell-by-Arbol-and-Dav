package Entradas.Listeners;

/**
 * Listener de acciones de ratón del juego.
 *
 * ─── DISEÑO ───────────────────────────────────────────────────────────────────
 *
 *  · onMouseAction(String action, float vx, float vy) cubre press y release de
 *    cualquier botón. El action string es el declarado en MouseButton.pressAction
 *    o MouseButton.releaseAction. Añadir un botón nuevo no toca esta interfaz.
 *
 *  · onScroll y onMouseMoved mantienen firmas propias porque su semántica es
 *    estructuralmente distinta (delta numérico, coordenadas sin acción asociada).
 *
 *  · Las coordenadas se entregan ya en espacio virtual (via ViewportInfo),
 *    para que los suscriptores no necesiten conocer la resolución del monitor.
 *
 * ─── CUÁNDO USAR LISTENER vs CONSULTA DIRECTA ────────────────────────────────
 *
 *  Listener   → clicks puntuales: disparar (edge), interactuar, menú.
 *  Consulta   → estado continuo: aim direction, isPressed cada frame.
 */
public interface MouseActionListener {

    /**
     * Se invoca en el press o release de un botón que tiene action declarada.
     *
     * @param action   identificador semántico, p.ej. "leftClick", "leftRelease",
     *                 "rightClick". Definido en MouseButton.pressAction / releaseAction.
     * @param virtualX coordenada X en espacio virtual del juego.
     * @param virtualY coordenada Y en espacio virtual del juego.
     */
    default void onMouseAction(String action, float virtualX, float virtualY) {}

    /**
     * Rueda del ratón girada.
     *
     * @param delta positivo = scroll abajo / zoom out,
     *              negativo = scroll arriba / zoom in
     *              (convención estándar AWT MouseWheelEvent).
     */
    default void onScroll(int delta) {}

    /**
     * El ratón se movió (incluye drag).
     *
     * @param virtualX nueva X en espacio virtual.
     * @param virtualY nueva Y en espacio virtual.
     */
    default void onMouseMoved(float virtualX, float virtualY) {}
}
