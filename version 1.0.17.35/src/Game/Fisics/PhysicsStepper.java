package Game.Fisics;

import Game.Colisions.CollisionManager;
import Game.GameObjects;
import java.util.List;

public class PhysicsStepper {

    private static final int maxSubSteps = 8;

    public static void moveWith(GameObjects obj, double moveX, double moveY, List<GameObjects> allObjects) {
        int steps = (int) Math.max(Math.abs(moveX), Math.abs(moveY));
        steps = Math.min(Math.max(steps, 1), maxSubSteps);

        double stepX = moveX / steps;
        double stepY = moveY / steps;

        for (int i = 0; i < steps; i++) {
            obj.getPosition().setX(obj.getPosition().getX() + stepX);
            obj.getPosition().setY(obj.getPosition().getY() + stepY);

            // Verifica colisiones luego de cada paso
            GameObjects collided = CollisionManager.findCollision(obj, allObjects);

            if (collided != null) {
                // Notifica la colisión
                obj.acceptCollision(collided);
                collided.acceptCollision(obj);

                // Rompe el bucle o ajusta posición según lógica del objeto
                obj.onCollision(collided);
                break;
            }
        }
    }
}
