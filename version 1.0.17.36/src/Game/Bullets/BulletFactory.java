package Game.Bullets;

import Game.EnimyNormal;
import Game.Gameplay.Aimm.AimDirection;
import Game.Gameplay.Aimm.AimSelection;
import Game.Gameplay.Aimm.AimStrategy;
import Game.Player;
import graficos.Assets;
import java.util.ArrayList;
import math.Vector2D;

public class BulletFactory {
    private boolean dir;
    private static double spreadX;
    private static double spreadY;
    public static Bullet createBullet(
            int bulletPershot,
            double x, double y,
            bulletType type,
            Player owner,
            ArrayList<EnimyNormal> enemies) {

        // Obtener dirección actual del jugador
        AimStrategy strategy = AimSelection.getStrategy();
        AimDirection aimDir = strategy.aim(owner);

        // Escalar esa dirección a la velocidad de la bala
        Vector2D direction = aimDir.getScaledDirection(type.getSpeed());

        Vector2D spawnPos = new Vector2D(x, y);
        
        // Pequeña dispersión aleatoria (ideal para escopetas)
        if (bulletPershot>1) {
            spreadX = Math.random() * (0.23*(bulletPershot/2));
            spreadY = Math.random() * (0.23*(bulletPershot/2));
        }else{
            spreadX=0;
            spreadY=0;
        }
        System.out.println("dir=" + direction);
        
        // Crea y devuelve la bala con sus físicas
        return new Bullet(
            spawnPos,
            Assets.bala,
            strategy.getDir(),
            direction.getX() + spreadX,
            direction.getY() + spreadY,
            type,
            owner,
            enemies
        );
    }
}

