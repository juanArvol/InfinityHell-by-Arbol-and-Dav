package Game.Bullets;

import Game.EnimyNormal;
import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimSelection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;
import graficos.Assets;
import java.util.ArrayList;
import java.util.Random;
import math.Vector2D;

public class BulletFactory {
    private static final Random rand = new Random();

    public static Bullet createBullet(
            int bulletPershot,
            double x, double y,
            bulletType type,
            Player owner,
            ArrayList<EnimyNormal> enemies) {

        // Dirección base del disparo
        AimStrategy strategy = AimSelection.getStrategy();
        AimDirection aimDir = strategy.aim(owner);
        Vector2D baseDir = aimDir.getScaledDirection(type.getSpeed()).normalize();

        // Posición inicial de la bala
        Vector2D spawnPos = new Vector2D(x, y);

        // Parámetros de dispersión
        double maxSpreadDegrees = 5.0 + bulletPershot; // puedes ajustar este valor o hacerlo depender del tipo
        double spreadRadians = Math.toRadians(maxSpreadDegrees);

        // Generar un ángulo aleatorio dentro del cono de dispersión
        double randomAngle = ((rand.nextDouble() * 2) - 1) * spreadRadians / 2.0;

        // Rotar la dirección base por ese ángulo
        Vector2D finalDir = rotateVector(baseDir, randomAngle).scale(type.getSpeed());

        // Crear y devolver la bala
        return new Bullet(
                spawnPos,
                Assets.bala,
                strategy.getDir(),
                finalDir.getX(),
                finalDir.getY(),
                type,
                owner,
                enemies
        );
    }

    /**
     * Rota un vector en un ángulo (en radianes)
     */
    private static Vector2D rotateVector(Vector2D v, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = v.getX() * cos - v.getY() * sin;
        double y = v.getX() * sin + v.getY() * cos;
        return new Vector2D(x, y);
    }
}
