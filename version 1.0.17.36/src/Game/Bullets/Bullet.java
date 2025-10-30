package Game.Bullets;

import Game.EnimyNormal;
import Game.Fisics.BulletPhysics;
import Game.GameObjects;
import Game.Player;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import math.Vector2D;

public class Bullet extends GameObjects {

    private boolean dir;
    private boolean alive;
    private double bulletX;
    private double bulletY;
    private Player p;
    private bulletType type;
    private ArrayList<EnimyNormal> enemies;
    private BulletPhysics bPhysics;

    public Bullet(Vector2D position, BufferedImage texture, boolean isDer, double x, double y, bulletType tipo, Player p, ArrayList<EnimyNormal> enemies) {
        super(position, texture);
        this.type=tipo;
        this.p=p;
        this.alive=true;
        this.enemies=enemies;
        this.bulletX=x;
        this.bulletY=y;
        this.dir=isDer;

        double gravity= type.tieneGravedad() ? 0.385 : 0;
        bPhysics = new BulletPhysics(gravity, new Vector2D(bulletX,bulletY), type.tieneGravedad(),dir);
    }

    public bulletType getTipo(){
        return type;
    }
    public int getNumber(){
        return (int) Math.ceil(Math.random()*250);
    }
    public boolean isAlive() {
        return alive;
    }
    public void setDead(){
        alive=false;
    }
    public void setAlive(){
        alive=true;
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

    @Override
    public void update() {
        bPhysics.update(position);
    }
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
    @Override
    public void draw(Graphics g) {
        g.drawImage(texture, (int) position.getX(), (int) position.getY(), null);
        drawHitbox(g);
    }

    @Override
    public Rectangle getBounds() {
        int width = 20;
        int height = 10;
        return new Rectangle((int) position.getX()+5, (int) position.getY()+9, width, height);
    }
}
