package Inputs.Listeners;

import Inputs.MouseAction;

/**
 * Listener de acciones de ratón del juego.
 *
 * ─── DISEÑO ───────────────────────────────────────────────────────────────────
 *
 *  · onMouseAction(MouseAction action, float vx, float vy) cubre press y release de
 *    cualquier botón. El MouseAction enum es tipado y cerrado. Añadir un botón 
 *    nuevo requiere actualizar tanto MouseButton como MouseAction enum.
 *
 *  · onScroll y onMouseMoved mantienen firmas propias porque su semántica es
 *    estructuralmente distinta (delta numérico, coordenadas sin acción asociada).
 *
 *  · Las coordenadas se entregan ya en espacio virtual (via ViewportInfo),
 *    para que los suscriptores no necesiten conocer la resolución del monitor.
 *
 * ─── MINI-HRFC — TYPED MOUSE ACTIONS ──────────────────────────────────────────
 *
 *  Los String de acciones quedan encapsulados en MouseInput como configuración
 *  interna. Los consumidores reciben MouseAction tipado:
 *  
 *  MouseInput ("middleClick") → MouseAction.MIDDLE_CLICK → PlayerCombat
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
     * @param action   acción tipada (LEFT_CLICK, MIDDLE_CLICK, RIGHT_CLICK, etc.).
     * @param virtualX coordenada X en espacio virtual del juego.
     * @param virtualY coordenada Y en espacio virtual del juego.
     */
    default void onMouseAction(MouseAction action, float virtualX, float virtualY) {}

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
