package Game;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import graficos.Assets;
import math.Vector2D;

public class EnimyNormal extends MovingObjects {
    private ArrayList<Vector2D> path;
    private Vector2D node;
    private int index;
    private boolean following;
    private Player player;

    public EnimyNormal(Vector2D position, BufferedImage texture, ArrayList<Vector2D> path, int startIndex, boolean following, Player player) {
        super(position, texture);
        this.path = path;
        this.index = startIndex;
        this.following = following =false; 
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

        double distanceNode = node.subtract(getCenter()).magnitude();

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

        steering = steering.scale(1.0 / 60.0); // ajustar fuerza por frame rate
        velocity = velocity.add(steering);
        velocity = velocity.limit(2); // velocidad máxima
        position = position.add(velocity);
    }

    @Override
    public void draw(Graphics g) {
        g.drawImage(Assets.enimy1[0], (int) position.getX(), (int) position.getY(), null);
        drawHitbox(g);
    }

    @Override
    public void onCollision(GameObjects other) {
        if (other instanceof Player) {
            position.add(new Vector2D(-1, 0)); // retrocede un poco
            System.out.println("Colisión con el jugador");
            // Aquí metes lógica extra si quieres (restar vida, empujar, etc.)
        }

        if (other instanceof Ambiente) {
            Rectangle suelo = other.getBounds();
            position.setY(suelo.getY() - getBounds().height); 
            velocity.setY(-0); // lo "apega" al suelo
        }
    }
}
