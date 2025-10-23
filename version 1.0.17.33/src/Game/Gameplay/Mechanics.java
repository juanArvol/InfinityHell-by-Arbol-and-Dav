package Game.Gameplay;

import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimSelection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;

public class Mechanics {

    public static void updateMechanics(Player player){

        // Obtener estrategia según teclas
        AimStrategy strategy = AimSelection.getStrategy();

        // Ejecutar dirección calculada
        AimDirection direction = strategy.aim(player);

        // Aplicar dirección a las balas del jugador
        //player.setCongelado(direction);
    }
}
