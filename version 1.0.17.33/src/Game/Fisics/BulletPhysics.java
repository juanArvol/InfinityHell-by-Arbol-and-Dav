package Game.Fisics;

import math.Vector2D;

public class BulletPhysics extends Physics{
    public BulletPhysics(double gravity){
        this.gravity=gravity;
        this.aAir=1;
        this.speedMaxAir=1;
        this.slide=1;
    }

    public void update(Vector2D position){
        applyGravity(false);
        position.setX(position.getX() + velocity.getX());
        position.setY(position.getY() + velocity.getY());
    }

    public void setVelocityI(double velX, double velY) {
        velocity.setX(velX);
        velocity.setY(velY);
    }

    public Vector2D getVelocity(){
        return velocity;
    }
}