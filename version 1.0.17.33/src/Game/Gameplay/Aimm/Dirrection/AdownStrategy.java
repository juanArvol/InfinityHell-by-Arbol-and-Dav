package Game.Gameplay.Aimm.Dirrection;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;

public class AdownStrategy extends AimStrategy {
    @Override
    protected AimDirection calculateDirection(Player player) {
        return new AimDirection(0, 35); // Dispara hacia abajo
    }
}
