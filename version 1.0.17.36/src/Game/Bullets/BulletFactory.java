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
    private static Random rand = new Random();

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

        // Vector de spawn de las balas
        Vector2D spawnPos = new Vector2D(x, y);

        // Vector de la direccion de las balas
        Vector2D dir = direction.normalize();

        // Vector perpendicular (para dispersión lateral)
        Vector2D perp = new Vector2D(dir.getY(), -dir.getX());

        // Factor de dispersion apartir de las balas disparadas por disparo
        double spreadFactor =  Math.sqrt(bulletPershot); // suaviza crecimiento
        double random1=rand.nextDouble();
        double random2=rand.nextDouble();

        // Factor de dispersion relativo
        double lateral = ((random1 - 0.5) * 0.025 * spreadFactor)*0.45;  // hacia los lados
        double forward = ((random2 - 0.5) * 0.5 * spreadFactor)*0.25;  // ligeramente hacia adelante/atrás

        //System.out.println("forward: "+forward+"lateral: "+lateral);
        
        Vector2D finalDir;
        if (bulletPershot>1) {
            finalDir= dir.add(perp.scale(lateral)).add(dir.scale(forward)).normalize().scale(type.getSpeed());
        }else{
            finalDir=direction;
        }
        //System.out.println(finalDir);
        // Crea y devuelve la bala con sus físicas
        return new Bullet(
            spawnPos,
            Assets.bala,
            strategy.getDir(),
            direction.getX() + finalDir.getX(),
            direction.getY() + finalDir.getY(),
            type,
            owner,
            enemies
        );
    }
}

