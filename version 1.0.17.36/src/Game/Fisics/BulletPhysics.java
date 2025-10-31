package Game.Fisics;

import math.Vector2D;

public class BulletPhysics extends Physics {
    private int lifeTime = 150; // Duración de la bala en frames
    private double gravity;
    private boolean tieneG;
    private boolean der;
    private boolean boost;
    private Vector2D direction;

    public BulletPhysics(double gravity, Vector2D direction, boolean tieneGravedad, boolean der) {
        //cosas bala
        this.gravity = gravity;
        this.direction=direction;
        this.tieneG= tieneGravedad;
        this.der=der;

        //cosas physics
        this.slide = 0.1;
        this.speedMaxAir = 999;
        this.speedMaxPiso= 999;
        this.aAir=0.1;
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
        moveX(Math.abs(direction.getX()),false,der,boost);
        moveY(direction.getY()/2.75);
        updateMoves(position);
    }
    public void showInfoB(boolean yes){
        if(yes){
            System.out.println(" | velocity: " + velocity + " | velocityX: " + velocity.getX()+" | velocityY: " + velocity.getY());
        }
    }
}