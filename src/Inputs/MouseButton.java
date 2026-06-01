package Inputs;

/**
 * Descriptor declarativo de un botón de ratón registrado en MouseInput.
 *
 * Agrega un botón nuevo = añadir un entry al array de MouseInput.BUTTONS.
 * No hay que tocar ningún otro sitio.
 *
 * ─── CAMPOS ──────────────────────────────────────────────────────────────────
 *
 *  awtButton   — constante java.awt.event.MouseEvent.BUTTON* (1=izq, 2=medio, 3=der).
 *
 *  stateKey    — nombre semántico del estado continuo ("leftPressed", "rightPressed"),
 *                o null si el botón no expone estado continuo.
 *
 *  pressAction — acción semántica disparada en el press (edge), o null.
 *
 *  releaseAction — acción semántica disparada en el release, o null.
 *
 * Inmutable por diseño.
 */
public final class MouseButton {

    public final int    awtButton;
    public final String stateKey;
    public final String pressAction;
    public final String releaseAction;

    public MouseButton(int awtButton, String stateKey,
                       String pressAction, String releaseAction) {
        this.awtButton     = awtButton;
        this.stateKey      = stateKey;
        this.pressAction   = pressAction;
        this.releaseAction = releaseAction;
    }
}
