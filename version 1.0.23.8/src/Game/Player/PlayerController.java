package Game.Player;

import Entradas.KeyBoard;
import Game.Fisics.PlayerPhysics;

public class PlayerController {

    private Player player;
    private PlayerPhysics physics;
    private PlayerState state;

    
    public PlayerController(Player player, PlayerState state) {
        this.player = player;
        this.physics = (PlayerPhysics) player.getPhysics();
        this.state = state;
    }

    public void update() {

        handleMovementInput();
        handleJumpInput();
    }

    private void handleMovementInput() {

        // FIX BUG-11: inputX era siempre 1 tanto para left como right.
        // Ahora se usa +1 para derecha y -1 para izquierda.
        // physics.moveX() usa state.isDer() para la direccion visual,
        // e inputX para la magnitud con signo.
        double inputX = 0;

        if (KeyBoard.left) {
            inputX = -1;
            state.setDer(false);
        }

        if (KeyBoard.right) {
            inputX = 1;
            state.setDer(true);
        }

        boolean running = KeyBoard.shift;
        state.setRunning(running);

        physics.moveX(
            inputX,
            state.isEnElSuelo(),
            running
        );
    }

    private void handleJumpInput() {
        if (KeyBoard.up && state.isEnElSuelo()) {
            physics.jump(10);
            state.setEnElSuelo(false);
        }
    }

    public PlayerPhysics getPhysics() {
        return physics;
    }
}
