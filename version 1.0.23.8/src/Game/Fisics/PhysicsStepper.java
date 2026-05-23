package Game.Fisics;

import Game.Engine.GameObjects;

public class PhysicsStepper {


    public static void moveX(GameObjects obj, double moveX) {
        var pos = obj.getTransform().getPosition();
            pos.setX(pos.getX() + moveX);
        
    }

    public static void moveY(GameObjects obj, double moveY) {
        var pos = obj.getTransform().getPosition();
            pos.setY(pos.getY() + moveY);
    }

    // Movimiento combinado X+Y con sub-steps diagonales
    public static void moveWith(GameObjects obj, double moveX, double moveY) {
        var pos = obj.getTransform().getPosition();
            pos.setX(pos.getX() + moveX);
            pos.setY(pos.getY() + moveY);
        
    }
}