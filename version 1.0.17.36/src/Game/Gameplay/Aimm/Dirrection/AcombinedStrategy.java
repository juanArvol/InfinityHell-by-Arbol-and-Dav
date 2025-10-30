package Game.Gameplay.Aimm.Dirrection;

import Game.Player;
import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimStrategy;

public class AcombinedStrategy {
    public class AdownStrategy extends AimStrategy {
    @Override
    protected AimDirection calculateDirection(Player player) {
        setDir(player.isDer());
        return new AimDirection(0, 1); // Dispara hacia abajo
    }
}
}
