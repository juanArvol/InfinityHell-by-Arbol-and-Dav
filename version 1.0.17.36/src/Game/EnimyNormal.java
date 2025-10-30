package Game;

import Game.Bullets.Bullet;
import graficos.Assets;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import math.Vector2D;

public class EnimyNormal extends MovingObjects {
    private ArrayList<Vector2D> path;
    private Vector2D node;
    private int index;
    private boolean following;
    private Player player;
    
    private boolean derecha;
    private double dir;
    private boolean piso;
    private double dirY;

    public EnimyNormal(Vector2D position, BufferedImage texture, ArrayList<Vector2D> path, int startIndex, boolean following, Player player) {
        super(position, texture);
        this.path = path;
        this.index = startIndex;
        this.following = following;
        this.velocity = new Vector2D(0.1, 0); // velocidad inicial
        this.player = player;
    }
    private Vector2D followingPlayer(){
        Vector2D playerPos = player.getCenter();
        return seekForce(playerPos);
    }

    // seguir el camino
    private Vector2D pathFollowing() {
        node = path.get(index);

        double distanceNode = node.subtract(getCenter()).length();

        if (distanceNode < 10) { // umbral de llegada, antes ponías 160 (demasiado grande)
            index++;
            if (index >= path.size()) {
                index = 0; // vuelve al inicio para ciclar
            }
        }

        return seekForce(node);
    }

    // buscar el nodo actual
    private Vector2D seekForce(Vector2D target) {
        Vector2D desired = target.subtract(getCenter());
        desired = desired.normalize().scale(2); // velocidad deseada
        return desired.subtract(velocity);
    }

    @Override
    public void update() {
        Vector2D steering;

        if (following) {
        // si quieres que solo siga al path
        steering = pathFollowing();
    } else {
        // si quieres que siga al player
        steering = followingPlayer();
    }

        steering = steering.scale(2.0/ 60); // ajustar fuerza por frame rate
        velocity = velocity.add(steering);
        velocity = velocity.limit(200); // velocidad máxima
        position = position.add(velocity);
    }
    public void setMirandoDer(boolean derecha){
        this.derecha= derecha;
        dir= derecha ? 1 : -1;
    }
    public void setEnElsuelo(boolean piso) {
        this.piso= piso;
        dirY= piso ? -1 : 1;
    }
    
    @Override
    public void draw(Graphics g) {
        g.drawImage(Assets.enimy1[0], (int) position.getX(), (int) position.getY(), null);
        drawHitbox(g);
    }
    @Override
public void onCollision(GameObjects other) {
    other.acceptCollision(this);
}
@Override
public void onCollisionWith(Ambiente ambiente) {
}
    @Override
        public void onCollisionWith(Bullet bullet) {
    }

        @Override
    public void onCollisionWith(Player player) {
    }
}