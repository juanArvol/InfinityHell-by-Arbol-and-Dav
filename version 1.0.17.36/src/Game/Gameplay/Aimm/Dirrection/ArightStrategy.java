package Game.Gameplay.Aimm.Dirrection;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;

public class ArightStrategy extends AimStrategy {
    @Override
    protected AimDirection calculateDirection(Player player) {
        setDir(true);
        return new AimDirection(1, 0); // Dispara hacia la derecha
    }
}
