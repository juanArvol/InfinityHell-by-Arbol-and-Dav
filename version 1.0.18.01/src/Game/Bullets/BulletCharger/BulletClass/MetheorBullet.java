package Game.Bullets.BulletCharger.BulletClass;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Bullets.BulletCharger.BulletClass.BulletClassUpdater.BulletOnUpdate;
import Game.Bullets.BulletCharger.BulletComport;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public class MetheorBullet extends BulletComport {
    private EnimyNormal enemy;
    private Player player;
    private Ambiente ambiente;
    @Override
    public double getBspeed() { return 100; }

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
        BulletOnUpdate.bulletOnUpdate(bullet, algo);
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
        //System.out.println("david e puto");
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, EnimyNormal enemy) {
        explode(b);
        //System.out.println("posicion del enemy: "+ enemy.getEnemyPosition());
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, Ambiente ambiente) {
        explode(b);
    }
    @Override
    public void setGameObject(GameObjects algo){
        if(algo instanceof EnimyNormal e){
            this.enemy=e;
        }
        if(algo instanceof Player p){
            this.player=p;
        }
        if(algo instanceof Ambiente a){
            this.ambiente=a;
        }
    }
    private void explode(Bullet b){
        double baseDamage = 35; // daño base
        double explosionPower = Math.abs(b.getBphysics().getVelocity().getY())*2.3 ; // mientras más caiga, más rompe
        double maxRadius = 250 + (explosionPower*1.5); // el rango depende de la velocidad
        double centerX = b.getPosition().getX();
        double centerY = b.getPosition().getY();
        double force=1;

        for (EnimyNormal e : player.getEnemies()) {
            double dx = e.getPosition().getX() - centerX;
            double dy = e.getPosition().getY() - centerY;
            double distance = Math.sqrt(dx*dx + dy*dy);
        
        // Evitamos operar entre 0 por si el enemigo está exactamente en el centro
        if (distance == 0) distance = 1;

        if (distance <= maxRadius) {
            force = (maxRadius - distance) * 0.1; // más cerca = más empuje
        }

        // Calculamos fuerza SOLO en los ejes necesarios
            double pushX = 0;
            double pushY = 0;
        
            // Si hay distancia real en X, empujamos en X
            if (Math.abs(dx) > 1) {
                pushX = (dx / distance) * force;
            }

            // Si hay distancia real en Y, empujamos en Y
            if (Math.abs(dy) > 1) {
                pushY = (dy / distance) * force;
            }

            e.getEnemyPosition().setX(e.getEnemyPosition().getX() + pushX);
            e.getEnemyPosition().setY(e.getEnemyPosition().getY() + pushY);

            player.getPosition().setX(player.getPosition().getX() + pushX);
            player.getPosition().setY(player.getPosition().getY() + pushY);

            double damage = baseDamage + (explosionPower * (1 - distance / maxRadius));
            System.out.println("Enemy recibió " + (int)damage + " daño por explosión");
            System.out.println("Enemy empujado con fuerza: " + force);
        }
    }
}
