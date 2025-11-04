package Game.Bullets.BulletCharger.BulletClass;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Bullets.BulletCharger.BulletComport;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public class ThunderBullet extends BulletComport {
    @Override
    public double getBspeed() { return 1; }

    @Override
    public boolean hasGravity() { return false; }

    @Override
    public int getDamage() { return 15; }

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
        b.getPosition().setY(ambiente.getPosition().getY()-50);
        //b.getBphysics().stopX();
    }
}
