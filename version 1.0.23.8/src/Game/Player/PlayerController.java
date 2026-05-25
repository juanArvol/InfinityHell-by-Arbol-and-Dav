package Game.Player;

import Entradas.KeyBoard;
import Game.Fisics.PlayerPhysics;

public class PlayerController {

    private final Player player;
    private final PlayerPhysics physics;
    private final PlayerState state;

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

        physics.moveX(inputX, state.isEnElSuelo(), running);
    }

    private void handleJumpInput() {
        if (KeyBoard.up && state.isEnElSuelo()) {
            physics.jump(10);
            physics.setOnGround(false);
            physics.clearSurface();
            state.setEnElSuelo(false);
        }
    }

    public PlayerPhysics getPhysics() { return physics; }
}
