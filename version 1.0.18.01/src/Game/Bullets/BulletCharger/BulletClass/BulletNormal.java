package Game.Bullets.BulletCharger.BulletClass;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Bullets.BulletCharger.BulletComport;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public class BulletNormal extends BulletComport {
    @Override
    public double getBspeed() { return 10; }

    @Override
    public double getMaxSpeedAir(){
        return 1;
    }

    @Override
    public double getMaxSpeedPiso(){
        return 999;
    }

    @Override
    public double getAcceleration(boolean option){
        if(option){
            return 0.1;
        }else{
            return 0.0001;
        }
    }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 10; }

    @Override
    public void onUpdate(Bullet bullet, GameObjects algo) {
        switch (algo) {
            case Player p -> onUpdateWith(bullet, p);
            case EnimyNormal e -> onUpdateWith(bullet,e);
            case Bullet b -> onUpdateWith(bullet,b);
            case Ambiente a -> onUpdateWith(bullet,a);
            default -> onUpdateWith((Bullet)bullet, (Bullet) bullet);
        }
    }
    public void onUpdateWith(Bullet bullet, GameObjects algo){
        //BulletOnUpdate.bulletOnUpdate(bullet, algo);
    }
    //Recibe el tipo de colision
    @Override
    public void onCollision(Bullet bullet, GameObjects algo) {
        acceptCollision(algo);                      //colision unitaria
        bulletAcceptCollisionWith(bullet, algo);    //colision doble
    }
    
    //aceptacion de la colision
    @Override
    public void acceptCollision(Collidable other) {
        super.acceptCollision(other);               //envia y define la colision con el other
    }
    @Override
    public void bulletAcceptCollisionWith(Bullet b, Collidable other) {
        super.bulletAcceptCollisionWith(b, other);  //envia y define la colision doble entre bala y other (que pasa con ambos)
    }

    //COLISION UNITARIA
    @Override
    public void onCollisionWith(Player player) {
        //System.out.println("bala colisiono con jugador");
    }
    @Override
    public void onCollisionWith(EnimyNormal enemy) {
    }

    @Override
    public void onCollisionWith(Bullet bullet) {
    }
    @Override
    public void onCollisionWith(Ambiente ambiente) {
        //System.out.println("aAaaaAAaaaa");
    }
    
    //COLISION DOBLE
    @Override
    public void bulletOnCollisionWith(Bullet b, Player player) {

    }
    @Override
    public void bulletOnCollisionWith(Bullet b, EnimyNormal enemy) {

    }
    @Override
    public void bulletOnCollisionWith(Bullet b, Ambiente ambiente) {

        //System.out.println("ptm esto no debe pasar");
    }
}
