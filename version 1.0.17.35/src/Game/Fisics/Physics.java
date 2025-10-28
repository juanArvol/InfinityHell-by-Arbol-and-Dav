package Game.Fisics;

import math.Vector2D;

public class Physics {
    protected Vector2D velocity = new Vector2D(0, 0);
    protected double gravity = 0.78;

    protected double mass;             //masa del objeto
    protected double aAir;             //Aceleracion en el aire
    protected double speedMaxAir;      //Velocidad maxima aire
    protected double speedMaxPiso;     //Velocidad maxima suelo
    protected double aGround;          //Aceleracion en el suelo
    protected double slide;            //Deslizamiento al frenar
    protected double speedMax;         //velocidad maxima
    protected double bonus;            //bono en la velocidad segun entorno

    protected double inputX;
    protected double vx;
    protected double dir;
    protected double accel;

    protected boolean onGround;
    protected boolean direction;
    protected boolean salto;
    protected boolean running;
    protected  byte count=0;

    public void setMass(double m){
        this.mass=m;
    }
    public double getMass(){
        return mass;
    }
    public void vSetX(double vX){
        velocity.setX(vX);
    }

    public void setJumping(boolean jumping){
        this.salto=jumping;
    }

    public void moveX(double inputX,boolean onGround, boolean direction, boolean running) {
        /* if (Double.isNaN(velocity.getX())) {
            System.out.println("ADVERTENCIA: velocity.getX() es NaN al iniciar moveX. Reiniciando a 0.");
            velocity.setX(0);
        } */
        this.onGround=onGround;
        this.direction=direction;
        this.inputX=inputX;
        this.running=running;

        dir = direction ? 1 : -1;
        accel = onGround ? aGround : aAir;
        double mAccel= (accel/mass);

        // factor reductor de velocidad en x
        bonus = onGround ? 1 : 0.8;
        vx=(velocity.getX()+ ((inputX*dir)*mAccel)*bonus);

            // Límite de velocidad
        if(inputX !=0){
            if(Math.abs(vx)>=speedMax){
                vx=dir* speedMax;
            }
        }
        vSetX(vx);
    }
    public void setMaxSpeed(boolean onGround){
        speedMax= onGround ? speedMaxPiso : speedMaxAir;
        if(Math.abs(vx)>speedMax-accel){
            speedMax=speedMax-accel;
        }
    }
    public void applyGravity(boolean onGround) {
        // F = m * g → a = g (independiente de masa, pero dejamos abierto a modificaciones)
        if (!onGround) {
            velocity.setY(velocity.getY() + (gravity*mass));
        }
    }
    public void showInfo(boolean yes){
        if(yes){
            System.out.println("inputX: " + inputX + " | dir: " + dir + " | accel: " + accel + " | bonus: " + bonus + " | velX: " + velocity.getX()+" | velY: " + velocity.getY()+" | vx: " + vx+" | masa: " + mass);
        }
    }
    public void jump(double force) {
        velocity.setY(-force/mass);
        salto = true;
    }
    public void addForce(double fx, double fy) {
        // F = m * a → a = F / m
        velocity.setX(velocity.getX() + (fx/mass));
        velocity.setY(velocity.getY() + (fy/mass));
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public void stopY() {
        velocity.setY(0);
    }

    public void stopX() {
        velocity.setX(0);
        vx=0;
    }
    public double getOposite(double x){
        return -x;
    }
}
