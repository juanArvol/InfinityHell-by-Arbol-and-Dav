package Game.Gameplay.Aimm;

import Game.Gameplay.Aimm.Dirrection.AidleStrategy;
import Game.Player;
import entradas.KeyBoard;

public class AimSelection {
    public static AimStrategy getStrategy() {
        // detector de la direccion
        boolean up = KeyBoard.up;
        boolean down = KeyBoard.down;
        boolean left = KeyBoard.left;
        boolean right = KeyBoard.right;
        
        //direccion resultante
        double dx=0;
        double dy=0;

        // suma los ejes
        if (up)     dy -= 1;
        if (down)   dy += 1;
        if (left)   dx -= 1;
        if (right)  dx += 1;

        final double fdx = dx;
        final double fdy = dy;

        if (dx == 0 && dy == 0) return new AidleStrategy();
        
        return new AimStrategy() {
        @Override
            protected AimDirection calculateDirection(Player player){
                if(fdx !=0){
                    setDir(fdx>0);
                }
                if(fdy !=0){
                    setAorA(true);
                }
                return new AimDirection(fdx, fdy);
            }
        };
    }
}
