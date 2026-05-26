package Game.Gameplay.Aimm.Dirrection;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player.Player;

public class AidleStrategy extends AimStrategy {
    @Override
    protected AimDirection calculateDirection(Player player) {
        setDir(player.getState().isDer());
        if(player.getState().isDer()){
            return new AimDirection(1, 0); // Dispara hacia la derecha
        }else{
            return new AimDirection(-1, 0); // Dispara hacia la izquierda
        }
    }
}
