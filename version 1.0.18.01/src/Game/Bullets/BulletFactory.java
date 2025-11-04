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
    private static final Random rand = new Random(); // instancia clase nativa de java q randomiza valores de los tipos de datos

    public static Bullet createBullet(
            int bulletPershot,
            double x, double y, //spawn en ejes X/Y
            bulletType type,
            Player owner,
            ArrayList<EnimyNormal> enemies) {

        // Obtener direccion de apuntado
        AimStrategy strategy = AimSelection.getStrategy();
        AimDirection aimDir = strategy.aim(owner);

        // Escala esa dirección a la velocidad de la bala
        Vector2D direction = aimDir.getScaledDirection(type.getSpeed());

        // Vector de spawn de las balas
        Vector2D spawnPos = new Vector2D(x, y);

        // Vector modificador de la direccion
        Vector2D dir = direction.normalize();

        // Vector perpendicular (para dispersión lateral)
        Vector2D perp = new Vector2D(dir.getY(), dir.getX());

        // Factor de dispersion apartir de la cantidad de balas por disparo
        double spreadFactor =  Math.sqrt(bulletPershot);
        double random1=rand.nextDouble();
        double random2=rand.nextDouble();
        byte sign = rand.nextBoolean() ? (byte)1 : -1;
        
        // Factor de dispersion relativo

        double frenadoY=((random2*sign) / spreadFactor)*0  ;
        double spreadYonX =(((random1/spreadFactor)*(bulletPershot/spreadFactor))*sign)*0;

        //System.out.println("spread: "+spreadFactor);
        //System.out.println(random1 + " " +random2);
        //System.out.println("forward: "+frenadoY+" lateral: "+spreadYonX);

        Vector2D finalDir; //vector final de la direccion (wtf un comentario influyendo XD)
        if (bulletPershot>1) {
            finalDir=(dir.add(perp.scale(spreadYonX)).add(dir.scale(frenadoY)).scale(type.getSpeed()));
        }else{
            finalDir=direction;
        }
        //System.out.println("X: "+finalDir.getX()+" y: "+finalDir.getY());
        
        // Crea y devuelve la bala con sus físicas
        return new Bullet(
            spawnPos,
            Assets.bala,
            strategy.getDir(),
            (direction.getX() + finalDir.getX()), //movimiento inicial X
            (direction.getY() + finalDir.getY()), //movimiento inicial Y
            type,
            owner,
            enemies
        );
    }
}

