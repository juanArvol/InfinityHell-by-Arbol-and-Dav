package Game.Bullets;

import Game.EnimyNormal;
import Game.Fisics.BulletPhysics;
import Game.Fisics.PhysicsStepper;
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
    

    @Override
    public void update() {
        bPhysics.update(position);
        type.getBulletClass().onUpdate(this,p);

        double moveX = bPhysics.getVelocity().getX();
        double moveY = bPhysics.getVelocity().getY();

        PhysicsStepper.moveWith(this, moveX, moveY, p.getGameState().getObjects());
    }
    
    @Override
    public void onCollisionWith(GameObjects Player) {
        type.getBulletClass().onCollision(this, Player, p);
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
