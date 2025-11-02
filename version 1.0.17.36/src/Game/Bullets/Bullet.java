package Game.Bullets;

import Game.Ambiente;
import Game.EnimyNormal;
import Game.Fisics.BulletPhysics;
import Game.Fisics.PhysicsStepper;
import Game.GameObjects;
import Game.Player;
import States.GameState;
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
    private GameState gs;
    private ArrayList<EnimyNormal> enemies;
    private BulletPhysics bPhysics;

    public Bullet(Vector2D position, BufferedImage texture, boolean isDer, double xSpeed, double ySpeed, bulletType tipo, Player p, ArrayList<EnimyNormal> enemies) {
        super(position, texture);
        this.type=tipo;
        this.p=p;
        this.alive=true;
        this.enemies=enemies;
        this.bulletX=xSpeed;
        this.bulletY=ySpeed;
        this.dir=isDer;
        this.gs= new GameState();
        double bulletGravity= type.tieneGravedad() ? 0.385 : 0;
        bPhysics = new BulletPhysics(bulletGravity, new Vector2D(bulletX,bulletY), type.tieneGravedad(),dir);
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
    

    @Override
    public void update() {
        bPhysics.update(position);
        type.getBulletClass().onUpdate(this,p);
        
        double moveX = bPhysics.getVelocity().normalize().getX();
        double moveY = bPhysics.getVelocity().normalize().getY();

        PhysicsStepper.moveWith(this, moveX, moveY, gs.getObjects());
    }
    @Override
    public void onCollisionWith(Ambiente ambiente) {
        bPhysics.stopVelocity();
        System.out.println(bPhysics.getVelocity());
        //System.out.println("su pta madreeee");
    }
    @Override
    public void onCollisionWith(Player Player) {
        type.getBulletClass().onCollision(this, p);
    }
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
