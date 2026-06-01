package Game.Player;

import Game.Engine.GameMath.Physics.Implementation.PlayerPhysics;
import Inputs.KeyBoard;

/**
 * Controlador de input del jugador.
 *
 * ── REFACTOR: DESACOPLAR DE Player ───────────────────────────────────────
 *
 * PROBLEMA ORIGINAL:
 *   PlayerController recibía el Player completo en su constructor:
 *
 *     public PlayerController(Player player, PlayerState state) {
 *         this.player  = player;
 *         this.physics = (PlayerPhysics) player.getPhysics();
 *         ...
 *     }
 *
 *   Solo usaba player.getPhysics() — el resto del Player era ignorado.
 *   Esto creaba una dependencia innecesaria: PlayerController conoce
 *   Player, y Player conoce PlayerController → dependencia circular.
 *   Además, un lector del código asume que PlayerController usa el Player
 *   completo cuando en realidad solo usa la física.
 *
 * SOLUCIÓN:
 *   Inyectar directamente las dependencias reales:
 *   - PlayerPhysics: para mover y saltar.
 *   - PlayerState:   para leer/escribir el estado del jugador.
 *
 *   Player extrae physics antes de pasarla:
 *     PlayerPhysics physics = (PlayerPhysics) getPhysics();
 *     controller = new PlayerController(physics, state);
 *
 * BENEFICIO:
 *   - PlayerController no depende de Player. La firma del constructor
 *     documenta exactamente qué necesita.
 *   - Sin referencia circular entre Player y PlayerController.
 *   - Reutilizable: si otro tipo de entidad controlable (NPC con IA manual,
 *     personaje de replay) necesita el mismo controlador, funciona sin Player.
 */
public class PlayerController {

    private final PlayerPhysics physics;
    private final PlayerState   state;

    public PlayerController(PlayerPhysics physics, PlayerState state) {
        this.physics = physics;
        this.state   = state;
    }

    public void update() {
        // FIX A-03: respetar el flag de congelado. Si está activo (trampa,
        // cutscene, efecto de estado), el jugador no procesa ningún input.
        if (state.isCongelado()) return;

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
