package Game.Fisics;

import math.Vector2D;

public class BulletPhysics extends Physics {
    private int lifeTime = 90; // Duración de la bala en frames
    private double gravity;
    private double speed;
    private boolean gravedad;
    private boolean der;
    private boolean boost;
    
    public BulletPhysics(double gravity, double speedX, boolean tieneGravedad, boolean der) {
        //cosas bala
        this.gravity = gravity;
        this.speed = speedX;
        this.gravedad= tieneGravedad;
        this.der=der;

        //cosas physics
        this.slide = 1;
        this.speedMaxAir = 2;
        this.speedMaxPiso= 999;
        this.aAir=1;
        this.aGround=0.001;
    }
    @Override
    public void moveX(double inputX, boolean onGround, boolean direction, boolean running){
        setMass(1);
        setMaxSpeed(onGround);
        super.moveX(inputX, onGround, direction, running);
        super.showInfo(false);
    }
    public void update(Vector2D position) {
        if (lifeTime-- <= 0) {
            stopX();
            showInfoB(false);
            return; // La bala ha expirado
        }else{
            showInfoB(false);
        }

        moveX(speed,false,der,boost);

        position.setX(position.getX() + velocity.getX());
        position.setY(position.getY() + velocity.getY());
        
    }
    public void showInfoB(boolean yes){
        if(yes){
            System.out.println(" | velocity: " + velocity + " | velocityX: " + velocity.getX()+" | velocityY: " + velocity.getY());
        }
    }
}