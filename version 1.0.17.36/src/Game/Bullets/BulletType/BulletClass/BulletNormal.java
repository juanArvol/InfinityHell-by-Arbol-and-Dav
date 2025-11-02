package Game.Bullets.BulletType.BulletClass;

import Game.Bullets.Bullet;
import Game.Bullets.BulletType.BulletComport;
import Game.Player;

public class BulletNormal implements BulletComport {
    @Override
    public double getSpeed() { return 10; }

    @Override
    public boolean hasGravity() { return true; }

    @Override
    public int getDamage() { return 10; }

    @Override
    public void onUpdate(Bullet bullet, Player owner) {
        /* Vector2D ownerp= owner.getPosition();
        System.out.println(ownerp); */
    }
    
    public void onCollision(Bullet bullet, Player owner) {
        
            System.out.println("bala colisiono con jugador");
        
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