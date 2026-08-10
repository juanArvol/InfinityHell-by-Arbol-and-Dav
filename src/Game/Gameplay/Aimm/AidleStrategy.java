package Game.Gameplay.Aimm;

import Game.Player.PlayerState;

/**
 * Estrategia de apuntado idle: mantiene la última dirección horizontal del jugador.
 *
 * ── HRFC — Player Reengineering ───────────────────────────────────────────
 *
 * CAMBIOS:
 *   - Ya no recibe Player completo. Recibe PlayerState (lo mínimo necesario).
 *   - No almacena estado propio entre frames — lee y escribe directamente
 *     en PlayerState, que es la única fuente de verdad.
 *   - Eliminados setDir() / setAimingUpOrDown(): el estado vive en PlayerState.
 *
 * COMPORTAMIENTO:
 *   Cuando no hay input de movimiento o apuntado, el jugador apunta
 *   horizontalmente en la última dirección que estaba mirando.
 *   Si miraba a la derecha → AimDirection(1, 0).
 *   Si miraba a la izquierda → AimDirection(-1, 0).
 *   verticalAim se resetea a NONE (sin apuntado vertical en idle).
 */
public class AidleStrategy implements AimStrategy {

    @Override
    public AimDirection calculateDirection(PlayerState state) {
        // No hay apuntado vertical en idle
        state.setVerticalAim(PlayerState.VerticalAim.NONE);

        boolean facingRight = state.isDer();
        return facingRight
            ? new AimDirection(1, 0)
            : new AimDirection(-1, 0);
    }
}
