package Entradas.Listeners;

/**
 * Listener de acciones de teclado del juego.
 *
 * ─── DISEÑO ───────────────────────────────────────────────────────────────────
 *
 *  · Eventos semánticos, no teclas físicas. La lógica del juego no sabe qué
 *    tecla disparó una acción; eso es responsabilidad de los KeyBinding en KeyBoard.
 *
 *  · onKeyAction(String action) es el único punto de entrada para edges.
 *    El parámetro "action" es el mismo string declarado en KeyBinding.edgeAction.
 *    Esto elimina el acoplamiento fuerte entre la interfaz y los bindings:
 *    añadir una tecla nueva no requiere añadir un método nuevo aquí.
 *
 *  · onKeyPressed / onKeyReleased (estado continuo) siguen disponibles por
 *    consulta directa a KeyBoard.getState(stateKey) — más eficiente por frame.
 *    El listener es para eventos puntuales (edge-triggered).
 *
 * ─── CUÁNDO USAR LISTENER vs CONSULTA DIRECTA ────────────────────────────────
 *
 *  Listener   → acciones puntuales: recargar, saltar, abrir menú, toggle.
 *  Consulta   → estado continuo: moverse, correr, agacharse cada frame.
 *
 * ─── COMPATIBILIDAD ───────────────────────────────────────────────────────────
 *
 *  Los suscriptores implementan onKeyAction() con un switch/if sobre el action
 *  string que les interese. No necesitan implementar métodos por cada tecla.
 *  Añadir una nueva tecla/acción no requiere modificar esta interfaz.
 */
public interface KeyActionListener {

    /**
     * Se invoca una vez (edge rising) cuando una tecla con edgeAction declarado
     * pasa de no-pulsada a pulsada.
     *
     * @param action el identificador semántico de la acción, p.ej. "jump",
     *               "reload", "toggleFullscreen". Definido en KeyBinding.edgeAction.
     */
    void onKeyAction(String action);
}
