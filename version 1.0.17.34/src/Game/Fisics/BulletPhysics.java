package Game.Fisics;

import math.Vector2D;

public class BulletPhysics extends Physics{
    private int lifeTime=90;
    public BulletPhysics(double gravity){
        this.gravity=gravity;
        this.aAir=0;
        this.speedMaxAir=1;
        this.slide=1;
    }

    public void update(Vector2D position){
        if(lifeTime-- <=0){
            velocity.setX(0);
            velocity.setY(0);
        }
        applyGravity(false);
        position.setX(position.getX() + velocity.getX());
        position.setY(position.getY() + velocity.getY());

        System.out.println(" | velocity: " + velocity + " | velocityX: " + velocity.getX());
    }

    public void setBulletVelocity(double velX, double velY) {
        velocity.setX(velX);
        velocity.setY(velY);
    }

    public Vector2D getVelocity(){
        return velocity;
    }
}