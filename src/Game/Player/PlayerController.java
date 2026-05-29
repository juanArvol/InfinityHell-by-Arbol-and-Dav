package Game.Player;

import Entradas.KeyBoard;
import Game.Fisics.PlayerPhysics;

/**
 * Controlador de input del jugador.
 *
 * Traduce input de teclado en llamadas a la física.
 * No contiene lógica de física; solo lee KeyBoard y delega.
 *
 * ─── REFACTOR (Entradas v2) ───────────────────────────────────────────────────
 *
 *  · Reemplaza todos los accesos a campos estáticos eliminados
 *    (KeyBoard.left, .right, .shift, .up, .r, .c) por KeyBoard.getState().
 *
 *  · handleActionsInput() eliminado: contenía solo cuerpos vacíos (código muerto).
 *    La lógica de recarga (r) y modo apuntado (c) se gestiona en Mechanics
 *    y a través del sistema de listeners de edge — no aquí.
 *
 *  · Eliminado el import no usado (java.security.Key).
 */
public class PlayerController {

    private final Player       player;
    private final PlayerPhysics physics;
    private final PlayerState   state;

    public PlayerController(Player player, PlayerState state) {
        this.player  = player;
        this.physics = (PlayerPhysics) player.getPhysics();
        this.state   = state;
    }

    public void update() {
        handleMovementInput();
        handleJumpInput();
    }

    private void handleMovementInput() {
        double inputX = 0;

        if (KeyBoard.getState("left")) {
            inputX = -1;
            state.setDer(false);
        }
        if (KeyBoard.getState("right")) {
            inputX = 1;
            state.setDer(true);
        }

        boolean running = KeyBoard.getState("shift");
        state.setRunning(running);

        physics.moveX(inputX, state.isEnElSuelo(), running);
    }

    private void handleJumpInput() {
        if (KeyBoard.getState("up") && state.isEnElSuelo()) {
            physics.jump(10);
            physics.setOnGround(false);
            physics.clearSurface();
            state.setEnElSuelo(false);
        }
    }

    public PlayerPhysics getPhysics() { return physics; }
}
