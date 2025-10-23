package Game.Gameplay.Aimm;

import Game.Gameplay.Aimm.Dirrection.AdownStrategy;
import Game.Gameplay.Aimm.Dirrection.AidleStrategy;
import Game.Gameplay.Aimm.Dirrection.AleftStrategy;
import Game.Gameplay.Aimm.Dirrection.ArightStrategy;
import Game.Gameplay.Aimm.Dirrection.AupStrategy;
import entradas.KeyBoard;

public class AimSelection {
    public static AimStrategy getStrategy() {
        // acá agregamos las direcciones
        if (KeyBoard.up) return new AupStrategy();
        if (KeyBoard.down) return new AdownStrategy();
        if (KeyBoard.left) return new AleftStrategy();
        if (KeyBoard.right) return new ArightStrategy();
        // default
        return new AidleStrategy();
    }
}
