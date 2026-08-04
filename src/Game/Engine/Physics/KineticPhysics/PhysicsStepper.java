package Game.Engine.Physics.KineticPhysics;

import Game.Engine.GameObjects;

/**
 * Utilidad para mover objetos paso a paso.
 *
 * Separa la lógica de "mover la posición" de la lógica de física,
 * permitiendo que CollisionsSystem aplique sub-steps o swept AABB
 * sin acoplarse a Physics directamente.
 */
public class PhysicsStepper {

    public static void moveX(GameObjects obj, double moveX) {
        var pos = obj.getTransform().getPosition();
        pos.setX(pos.getX() + moveX);
    }

    public static void moveY(GameObjects obj, double moveY) {
        var pos = obj.getTransform().getPosition();
        pos.setY(pos.getY() + moveY);
    }

    /** Movimiento combinado X+Y (compatible con swept AABB diagonal). */
    public static void moveWith(GameObjects obj, double moveX, double moveY) {
        var pos = obj.getTransform().getPosition();
        pos.setX(pos.getX() + moveX);
        pos.setY(pos.getY() + moveY);
    }
}
