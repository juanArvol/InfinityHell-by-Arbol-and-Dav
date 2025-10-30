package Game.Gameplay.Aimm.Dirrection;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;

public class AleftStrategy extends AimStrategy {

    
    @Override
    protected AimDirection calculateDirection(Player player) {
        setDir(false);
        return new AimDirection(-1, 0); // Dispara hacia la izquierda
    }
}
