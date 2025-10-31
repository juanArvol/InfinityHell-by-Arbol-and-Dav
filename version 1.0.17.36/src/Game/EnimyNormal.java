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
     private int lifeEnemyNormalNormal = 1000000 ;
     private int lifeMaxEnimyNormal = 1000000;
    // se me ocurrio que un enemigo le pueda robar a uno aun potenciador, jajaj seria epico, se imagina a ese hp teniendo mas vida o creciendo, o obteniendo un abazooca

    public EnimyNormal(Vector2D position, BufferedImage texture, ArrayList<Vector2D> path, int startIndex, boolean following, Player player) {
        super(position, texture);
        this.path = path;
        this.index = startIndex;
        this.following = following;
        this.velocity = new Vector2D(0.1, 0); // velocidad inicial
        this.player = player;
    }
    public int getLifeEnemyNormal(){
    return lifeEnemyNormalNormal;
}
public int getLifeMaxEnemyNormal(){
    return lifeMaxEnimyNormal;
}

public void daemonEnemyBullet(int value){
    this.lifeEnemyNormalNormal -= value;
    if (this.lifeEnemyNormalNormal < 0 ) {
        this.lifeEnemyNormalNormal = 0;
            System.out.println("li enemifo: " + this.lifeEnemyNormalNormal + "/" + this.lifeMaxEnimyNormal);
}
        
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
    if (other instanceof Player) {
 
}
}
@Override
public void onCollisionWith(Ambiente ambiente) {
}
    @Override
        public void onCollisionWith(Bullet bullet) {
    
    System.out.println("muerto");
    System.out.println("vida enemigo: " + lifeEnemyNormalNormal + "/" + lifeMaxEnimyNormal);

    if (lifeEnemyNormalNormal <= 0) {
        System.out.println(" se supone que se tiene que morir");
    }
    //arvoloco si algun dia llega a leer esto, yo me encargo de los sprites y eso pero tambien quiero encargarme de la destruccion del enemynormal, yo lo cree yo lo destruire.
            
    }

        @Override
    public void onCollisionWith(Player p) {
        
            p.recibedDaimage(10);
            int offset=0;
            if(derecha){
                player.position.setX((position.getX()) + player.getBounds().width+offset);
            }else{
                player.position.setX((position.getX()) - player.getBounds().width-offset);
            }
            velocity.setX(dir*10);
            velocity.setY(dirY*10); // lo "apega" al suelo
    }
}
