package Game.Gameplay;

import Entradas.KeyBoard;
import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimSelection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player.Player;

/**
 * Mecánicas de juego por frame.
 *
 * ─── REFACTOR (Entradas v2) ───────────────────────────────────────────────────
 *
 *  · Reemplaza KeyBoard.c (campo estático eliminado) por
 *    KeyBoard.getState("c") — API del nuevo módulo de input.
 */
public class Mechanics {

    public static void updateMechanics(Player player) {
        boolean congelado = KeyBoard.getState("c");

        AimStrategy  strategy  = AimSelection.getStrategy();
        AimDirection direction = strategy.aim(player);
        double       dirX      = direction.getDirection().getX();

        player.getState().setCongelado(congelado);
        player.getState().setAimDirection(direction.getDirection());

        if (dirX > 0) {
            player.getState().setDer(true);
        } else if (dirX < 0) {
            player.getState().setDer(false);
        }

        player.getState().setMirandoArriba(direction.getDirection().getY() < 0);
        player.getState().setMirandoAbajo(direction.getDirection().getY() > 0);
    }
}
