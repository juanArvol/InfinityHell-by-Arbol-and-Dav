package Game.Gameplay.Aimm.Dirrection;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;

public class AidleStrategy extends AimStrategy {
    @Override
    protected AimDirection calculateDirection(Player player) {
        setDir(player.isDer());
        if(player.isDer()){
            return new AimDirection(1, 0); // Dispara hacia la derecha
        }else{
            return new AimDirection(-1, 0); // Dispara hacia la izquierda
        }
    }
}
