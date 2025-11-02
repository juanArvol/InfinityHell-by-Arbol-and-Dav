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
        Vector2D perp = new Vector2D(dir.getY(), dir.getX());

        // Factor de dispersion apartir de las balas disparadas por disparo
        double spreadFactor =  Math.sqrt(bulletPershot);
        double random1=rand.nextDouble();
        double random2=rand.nextDouble();
        byte sign = rand.nextBoolean() ? (byte)1 : -1;
        
        // Factor de dispersion relativo
        double spreadYonX =(((random1/spreadFactor)*(bulletPershot/spreadFactor))*sign)*0.25;
        double frenadoY=((random2*sign) / spreadFactor)*0  ;

        //System.out.println("spread: "+spreadFactor);
        //System.out.println(random1 + " " +random2);
        //System.out.println("forward: "+frenadoY+" lateral: "+spreadYonX);
        //System.out.println("X: "+finalDir.getX()+" y: "+finalDir.getY());
        Vector2D finalDir;
        if (bulletPershot>1) {
            finalDir=(dir.add(perp.scale(spreadYonX)).add(dir.scale(frenadoY)).scale(type.getSpeed()));
        }else{
            finalDir=direction;
        }
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

