package Game.Gameplay;

import Entradas.KeyBoard;
import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimSelection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player.Player;

public class Mechanics {

    public static void updateMechanics(Player player){
        boolean c = KeyBoard.c;
        AimStrategy strategy = AimSelection.getStrategy();
        AimDirection direction = strategy.aim(player);
        double dirX = direction.getDirection().getX();
        
        player.getState().setCongelado(c);
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