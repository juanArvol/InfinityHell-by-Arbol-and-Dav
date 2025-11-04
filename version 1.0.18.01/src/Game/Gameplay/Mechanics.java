package Game.Gameplay;

import Game.Player;
import entradas.KeyBoard;

public class Mechanics {
    
    public static void updateMechanics(Player player){
        boolean c = KeyBoard.c;
        // Obtener estrategia según teclas
        // Ejecutar dirección calculada
        
        if(c){
            player.setCongelado(true);
        }else{
            player.setCongelado(false);
        }
    }
}
