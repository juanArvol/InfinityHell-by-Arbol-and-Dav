package Game.Bullets.BulletType;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Bullets.BulletType.BulletClass.BulletCollidable;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public abstract class BulletComport implements BulletCollidable{
    protected double bulletSpeed;
    protected boolean hasGravity;
    protected int damage;

    public double getBspeed(){
        return bulletSpeed;
    };
    public boolean hasGravity(){
        return hasGravity;
    };

    public int getDamage(){
        return damage;
    };
    public void onUpdate(Bullet bullet, GameObjects algo){
        //por defecto non hhace nada
    };
    @Override
    public void acceptCollision(Collidable other){
        switch (other) {
            case Player p -> onCollisionWith(p);
            case EnimyNormal e -> onCollisionWith(e);
            case Ambiente a -> onCollisionWith(a);
            default -> onCollisionWith((GameObjects) other);
        }
    }
    @Override
    public void bulletAcceptCollisionWith(Bullet b, Collidable other){
        switch (other) {
            case Player p -> bulletOnCollisionWith(b,p);
            case EnimyNormal e -> bulletOnCollisionWith(b,e);
            case Ambiente a -> bulletOnCollisionWith(b,a);
            default -> bulletOnCollisionWith((Bullet) b,((GameObjects) other));
        }
    }
    public void onCollision(Bullet bullet, GameObjects algo){
        //arroz
    };
}
/* public void acceptCollision(Collidable other) {
        switch (other) {
            case Player p -> onCollisionWith(p);
            case EnimyNormal e -> onCollisionWith(e);
            case Bullet b -> onCollisionWith(b);
            case Ambiente a -> onCollisionWith(a);
            default -> onCollisionWith((GameObjects) other);
        }
    }
    // Por defecto, no hacer nada si no se sobreescriben en las subclases
    @Override public void onCollisionWith(GameObjects other) {}
    @Override public void onCollisionWith(Player player) {}
    @Override public void onCollisionWith(EnimyNormal enemy) {}
    @Override public void onCollisionWith(Bullet bullet) {}
    @Override public void onCollisionWith(Ambiente ambiente) {} */