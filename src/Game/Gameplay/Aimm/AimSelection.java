package Game.Gameplay.Aimm;

import Game.Gameplay.Aimm.Dirrection.AidleStrategy;
import Game.Player.Player;
import Inputs.KeyBoard;

/**
 * Selecciona la estrategia de apuntado según el input actual.
 *
 * ─── REFACTOR (Entradas v2) ───────────────────────────────────────────────────
 *
 *  · Reemplaza KeyBoard.up/down/left/right (campos estáticos eliminados)
 *    por KeyBoard.getState("stateKey") — API del nuevo módulo de input.
 *
 *  · Eliminado el setAorA(true) dentro de la estrategia anónima: esa llamada
 *    era un efecto secundario sin consumidor conocido y quedaba sobrescrita
 *    en el mismo frame por Mechanics. Eliminada como lógica inútil.
 *
 *  · La estrategia anónima ahora usa setAimingUpOrDown (nombre corregido).
 */
public class AimSelection {

    public static AimStrategy getStrategy() {

        boolean up    = KeyBoard.getState("up");
        boolean down  = KeyBoard.getState("down");
        boolean left  = KeyBoard.getState("left");
        boolean right = KeyBoard.getState("right");

        double dx = 0;
        double dy = 0;

        if (up)    dy -= 1;
        if (down)  dy += 1;
        if (left)  dx -= 1;
        if (right) dx += 1;

        if (dx == 0 && dy == 0) return new AidleStrategy();

        final double fdx = dx;
        final double fdy = dy;

        return new AimStrategy() {
            @Override
            protected AimDirection calculateDirection(Player player) {
                if (fdx != 0) setDir(fdx > 0);
                if (fdy != 0) setAimingUpOrDown(true);
                return new AimDirection(fdx, fdy);
            }
        };
    }
}
