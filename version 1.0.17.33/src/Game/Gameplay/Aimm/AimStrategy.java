package Game.Gameplay.Aimm;

import Game.Player;
import entradas.KeyBoard;

public abstract class AimStrategy {

    protected boolean up;
    protected boolean down;
    protected boolean left;
    protected boolean right;
    protected boolean c;

    public AimDirection aim(Player player) {
        // Leer teclas aquí y guardarlas como estado solo durante este frame
        this.up = KeyBoard.up;
        this.down = KeyBoard.down;
        this.left = KeyBoard.left;
        this.right = KeyBoard.right;
        this.c = KeyBoard.c;

        // Delegamos la lógica a cada estrategia concreta
        return calculateDirection(player);
    }

    // Método que cada estrategia concreta debe implementar
    protected abstract AimDirection calculateDirection(Player player);
}
