package Game.Bullets.BulletType.BulletClass;

import Game.Ambiente;
import Game.Bullets.Bullet;
import Game.Bullets.BulletType.BulletComport;
import Game.Colisions.SystemColisions.Collidable;
import Game.EnimyNormal;
import Game.GameObjects;
import Game.Player;

public class BulletNormal extends BulletComport {
    @Override
    public double getBspeed() { return 10; }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 10; }

    @Override
    public void onUpdate(Bullet bullet, GameObjects algo) { // esto por ahora no hara nada en lo q descubro como pasarle una instancia de
        /* switch (algo) {
            case Player p -> System.out.println();
            case EnimyNormal e -> System.out.println("e");
            case Bullet b -> System.out.println("i");
            case Ambiente a -> System.out.println();
            default -> System.out.println("u");
        }*/
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
    @Override
    public void onCollisionWith(GameObjects other) {
    }
    
    //COLISION DOBLE
    @Override
    public void bulletOnCollisionWith(Bullet b, Player player) {
        //System.out.println("david e puto");
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, EnimyNormal enemy) {
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, Ambiente ambiente) {
        //System.out.println("ptm esto no debe pasar");
    }
    @Override
    public void bulletOnCollisionWith(Bullet b, GameObjects other) {
    }
}
/* private void explode() {

        double baseDamage = 35; // daño base
        double explosionPower = Math.abs(1)*2.3 ; // mientras más caiga, más rompe
        double maxRadius = 250 + (explosionPower*1.5); // el rango depende de la velocidad
        double centerX = position.getX();
        double centerY = position.getY();

        for (EnimyNormal e : enemies) {
            double dx = e.getPosition().getX() - centerX;
            double dy = e.getPosition().getY() - centerY;
            double distance = Math.sqrt(dx*dx + dy*dy);

        if (distance <= maxRadius) {
            double force = (maxRadius - distance) * 0.1; // más cerca = más empuje

            // Evitamos dividir por cero si el enemigo está exactamente en el centro
        if (distance == 0) distance = 1;

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

            e.position.setX(e.position.getX() + pushX);
            e.position.setY(e.position.getY() + pushY);

            p.position.setX(p.position.getX() + pushX);
            p.position.setY(p.position.getY() + pushY);
            double damage = baseDamage + (explosionPower * (1 - distance / maxRadius));
            System.out.println("Enemy recibió " + (int)damage + " daño por explosión");
            System.out.println("Enemy empujado con fuerza: " + force);
        }
        }
    } */

    /* @Override
    public void onCollision(GameObjects other) {
        Player p= this.p;
        if(other instanceof Player && tipo==3) {
                p.position.setY(position.getY() - p.getBounds().height);
                p.setEnElSuelo(true);
            if(KeyBoard.space||(KeyBoard.up && KeyBoard.c==false)) {
                p.setEnElSuelo(false);
            }
            if(KeyBoard.down && KeyBoard.c && other instanceof Player) {
                p.position.setY(position.getY() + p.getBounds().height);
                p.setEnElSuelo(false);
            }else{
                p.setEnElSuelo(false);
            }
        }
        if(other instanceof EnimyNormal) {
            EnimyNormal e = (EnimyNormal) other;
            e.position.setX(e.position.getX()+(number));
            e.position.setY(e.position.getY()+(number));
        }
        if(other instanceof Ambiente) {
            Ambiente a = (Ambiente) other;
            if(tipo ==4){
                explode();
            }
            p.removeBullet(this);
        }
    } */