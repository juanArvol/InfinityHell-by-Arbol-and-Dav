package Inputs;

/**
 * Descriptor declarativo de un binding tecla → acción.
 *
 * Cada instancia representa UNA tecla registrada en KeyBoard.
 * Agregar una tecla nueva = añadir un entry al array de KeyBoard.BINDINGS.
 * No hay que tocar ningún otro sitio.
 *
 * ─── CAMPOS ──────────────────────────────────────────────────────────────────
 *
 *  keyCode   — java.awt.event.KeyEvent.VK_* de la tecla física.
 *
 *  stateKey  — nombre semántico del estado continuo que expone esta tecla,
 *              o null si la tecla solo produce edge (ej. F3, F11, ESCAPE).
 *              Se usa para rellenar el mapa de estados de la instancia KeyBoard
 *              y para el reset en focusLost.
 *
 *  edgeAction — acción semántica disparada en edge (rising edge), o null si
 *               la tecla solo alimenta un estado continuo sin edge propio.
 *
 * Inmutable por diseño: todos los campos son final.
 */
public final class KeyBinding {

    public final int        keyCode;
    public final String     stateKey;    // null = sin estado continuo
    public final String     edgeAction;  // null = sin edge semántico

    public KeyBinding(int keyCode, String stateKey, String edgeAction) {
        this.keyCode    = keyCode;
        this.stateKey   = stateKey;
        this.edgeAction = edgeAction;
    }

    // ─── Factory helpers para los dos patrones más comunes ────────────────────

    /**
     * Tecla de movimiento o estado continuo, sin acción de edge.
     * Ejemplo: W, A, S, D, SHIFT.
     */
    public static KeyBinding stateOnly(int keyCode, String stateKey) {
        return new KeyBinding(keyCode, stateKey, null);
    }

    /**
     * Tecla de acción (solo edge, sin estado continuo expuesto).
     * Ejemplo: F3, F11, ESCAPE.
     */
    public static KeyBinding edgeOnly(int keyCode, String edgeAction) {
        return new KeyBinding(keyCode, null, edgeAction);
    }

    /**
     * Tecla que alimenta tanto un estado continuo como un edge semántico.
     * Ejemplo: SPACE (estado "space" + edge "jump"), S (estado "down" + edge "crouch").
     */
    public static KeyBinding stateAndEdge(int keyCode, String stateKey, String edgeAction) {
        return new KeyBinding(keyCode, stateKey, edgeAction);
    }
}
