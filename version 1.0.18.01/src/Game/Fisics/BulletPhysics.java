package Game.Fisics;

import math.Vector2D;

public class BulletPhysics extends Physics {
    private int lifeTime=60; // Duración de la bala en frames
    private double gravity;
    private boolean tieneG;
    private boolean onGround=false;
    private boolean der;
    private boolean boost;
    private Vector2D moveBullet;

    public BulletPhysics(double gravity, double speedMaxAir, double speedMaxPiso, double airAcceleration, double pisoAcceleration,  Vector2D bulletVelocity, boolean tieneGravedad, boolean derecha) {
        //cosas bala
        this.gravity = gravity;
        this.moveBullet=bulletVelocity;
        this.tieneG= tieneGravedad;
        this.der=derecha;
        //cosas physics
        this.slide = 1;
        this.speedMaxAir = speedMaxAir;
        this.speedMaxPiso= speedMaxPiso;
        this.aAir=airAcceleration;
        this.aGround=pisoAcceleration;
    }
    public void setOnground(boolean yes){
        this.onGround=yes;
        //System.out.println("la bala choca contra el piso papi");
    }
    @Override
    public void moveX(double inputX, boolean onGround, boolean direction, boolean running){
        setMass(1);
        setMaxSpeed(onGround);
        super.moveX(inputX, onGround, direction, running);
        super.showInfo(true);
    }
    public void update(Vector2D position) {
        if (lifeTime-- <= 0) {
            stopX();
            stopY();
            showInfoB(false);
            return; // La bala ha expirado
        }else{
            showInfoB(false);
        }
        moveX(Math.abs(moveBullet.getX()),onGround,der,boost);
        moveY(moveBullet.getY(),tieneG);
        updateMoves(position);
    }
    public void showInfoB(boolean yes){
        if(yes){
            System.out.println(" | velocity: " + velocity + " | velocityX: " + velocity.getX()+" | velocityY: " + velocity.getY());
        }
    }
    public Vector2D getBphysicsVector2d(){
        return moveBullet;
    }
}