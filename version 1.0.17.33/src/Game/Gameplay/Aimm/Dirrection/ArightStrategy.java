package Game.Gameplay.Aimm.Dirrection;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;

public class ArightStrategy extends AimStrategy {
    @Override
    protected AimDirection calculateDirection(Player player) {
        return new AimDirection(25, 0); // Dispara hacia la derecha
    }
}
