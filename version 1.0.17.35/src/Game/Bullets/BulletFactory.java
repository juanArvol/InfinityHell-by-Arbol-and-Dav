package Game.Bullets;

import Game.EnimyNormal;
import Game.Player;
import graficos.Assets;
import java.util.ArrayList;
import math.Vector2D;

public class BulletFactory {

    public static Bullet createBullet(
            double x, double y,
            boolean mirandoDerecha,
            bulletType type,
            Player owner,
            ArrayList<EnimyNormal> enemies) {

        double dir = mirandoDerecha ? 1 : -1;

        // Posición inicial ligeramente adelantada
        Vector2D spawnPos = new Vector2D(
            mirandoDerecha ? x + 8 : x - 8,
            y
        );

        // Pequeña dispersión aleatoria (ideal para escopetas)
        double spreadX = (Math.random() - 0.5) * 0.1;
        double spreadY = (Math.random() - 0.5) * 0.1;

        // Crea y devuelve la bala con sus físicas
        return new Bullet(
            spawnPos,
            Assets.bala,
            dir + spreadX,
            spreadY,
            type,
            owner,
            enemies
        );
    }
}

