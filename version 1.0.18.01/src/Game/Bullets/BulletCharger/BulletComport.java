package Game.Bullets.BulletCharger;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public abstract class BulletComport implements BulletCollidable{
    protected double bulletSpeed;
    protected double bulletMaxSpeedAir;
    protected double accelerationAir;
    protected double bulletMaxSpeedPiso;
    protected double accelerationPiso;
    protected boolean hasGravity;
    protected int damage;

    public double getMaxSpeedAir(){
        return bulletMaxSpeedAir;
    }
    public double getMaxSpeedPiso(){
        return bulletMaxSpeedPiso;
    }
    public double getBspeed(){
        return bulletSpeed;
    };
    public double getAcceleration(boolean option){
        if(option){
            return accelerationAir;
        }else{
            return accelerationPiso;
        }
    }
    public boolean hasGravity(){
        return hasGravity;
    };

    public int getDamage(){
        return damage;
    };
    public void onUpdate(Bullet bullet, GameObjects algo){
        //por defecto non hhace nada
    };
    public void setGameObject(GameObjects algo){
        // nothing
    }
    @Override
    public void acceptCollision(Collidable other){
        switch (other) {
            case Player p -> onCollisionWith(p);
            case EnimyNormal e -> onCollisionWith(e);
            case Ambiente a -> onCollisionWith(a);
            default -> onCollisionWith((Bullet) other);
        }
    }
    @Override
    public void bulletAcceptCollisionWith(Bullet b, Collidable other){
        switch (other) {
            case Player p -> bulletOnCollisionWith(b,p);
            case EnimyNormal e -> bulletOnCollisionWith(b,e);
            case Ambiente a -> bulletOnCollisionWith(b,a);
            default -> onCollisionWith((Bullet) other);
        }
    }
    public void onCollision(Bullet bullet, GameObjects algo){
        //arroz
    };
}