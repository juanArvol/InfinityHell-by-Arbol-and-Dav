package Game.Gameplay.Aimm.Dirrection;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player.Player;

/**
 * Estrategia de apuntado idle: mantiene la última dirección horizontal del jugador.
 *
 * ─── REFACTOR (Entradas v2) ───────────────────────────────────────────────────
 *
 *  · Sin cambios funcionales. Actualizada la llamada setAorA → setAimingUpOrDown
 *    (renombrado en AimStrategy para claridad).
 */
public class AidleStrategy extends AimStrategy {

    @Override
    protected AimDirection calculateDirection(Player player) {
        boolean facingRight = player.getState().isDer();
        setDir(facingRight);
        setAimingUpOrDown(false);
        return facingRight
            ? new AimDirection(1, 0)
            : new AimDirection(-1, 0);
    }
}
