package Game;

import Game.Fisics.PlayerPhysics;
import States.GameState;
import entradas.KeyBoard;
import graficos.Assets;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import math.Vector2D;
import Game.Bullets.Bullet;

public class Player extends MovingObjects {
    private PlayerPhysics pPhysics;
    private boolean isShoot;
    private boolean congelado = false;
    private boolean mirandoDerecha = true;
    private boolean agachado= false;
    private boolean running= false;
    private int frame = 0;
    private int animTick = 0;

    private boolean enElSuelo = false;

    private double WaitingTime;
    private GameState gameState;
    private WeaponSelected weaponS;
    private ArrayList<EnimyNormal> enemies;
    private ArrayList<Bullet> bullets = new ArrayList<>();
    
@Override
public Vector2D getPosition() {
    return position;
}
public void setEnElSuelo(boolean enElSuelo) {
    this.enElSuelo = enElSuelo;
}
public void setWait(boolean isShoot){
    if(isShoot){
        WaitingTime++;
    }else{
        WaitingTime= WaitingTime-1;
    }
    if (WaitingTime < 0) WaitingTime = 0;
}
public void setEnemies(ArrayList<EnimyNormal> enemies){
    this.enemies = enemies;
}
public ArrayList<EnimyNormal> getEnemies() {
    return enemies;
}
public void setCongelado(boolean congelado) {
    this.congelado = congelado;
}
public boolean isCongelado() {
    return congelado;
}
public void setAgachado(boolean agachado){
    this.agachado=agachado;
}
public boolean isAgachado(){
    return agachado;
}
public boolean isDer(){
    return mirandoDerecha;
}
public boolean isSuelo(){
    return enElSuelo;
}
private boolean checkCollision() {
    for (GameObjects obj : GameState.instance.getObjects()) {
        if (obj == this) continue;
        if (!(obj instanceof Ambiente)) continue; // <- bloquea solo con paredes y suelo
        if (getBounds().intersects(obj.getBounds())) {
            onCollision(obj);
            return true;
        }
    }
    return false;
}
public void drawBullets(Graphics g) {
    for (Bullet b : bullets) {
        b.draw(g);
        b.drawHitbox(g);
    }
}
public ArrayList<Bullet> getBullets() {
    return bullets;
}
public void addBullet(Bullet b) {
    bullets.add(b);
    gameState.addGameObject(b);
}
public void removeBullet(Bullet b) {
    bullets.remove(b);
    gameState.removeGameObject(b);
}
public void setAlive(boolean alive){
    for (Bullet b : bullets) {
        if(alive){
            b.setAlive();
        }else{
            b.setDead();
        }
    }
}
public void updateBullets(){
    /*bullets.removeIf(b -> !b.isAlive() ||
                b.getPosition().getX() < 0 ||
                b.getPosition().getX() > 1900);*/
    for (Bullet b : bullets) b.update();
}
public void setWeapon(WeaponSelected weaponS){
    this.weaponS = weaponS;
}

@Override
public void update() {
    running= false;
    double inputX = 0;

    if(KeyBoard.shift){
        running=true;
        if (KeyBoard.right) {
            inputX = 25;
            mirandoDerecha = true;
        }
        if (KeyBoard.left) {
            inputX = 25;
        mirandoDerecha = false;
        }
    }else{
        if (KeyBoard.right) {
            inputX = 1;
            mirandoDerecha = true;
        }
        if (KeyBoard.left) {
            inputX = 1;
        mirandoDerecha = false;
        }
    }
    if (congelado){
        inputX = 0;
    }

    // Saltar
    if ((KeyBoard.space || (KeyBoard.up && !KeyBoard.ei)) && enElSuelo && !congelado) {
        pPhysics.jump(14.5);  // salto ajustable
        enElSuelo = false;
    } else{    
        pPhysics.setJumping(false);
    }

    pPhysics.moveX(inputX, enElSuelo, mirandoDerecha, running);
    pPhysics.applyGravity(enElSuelo);

    // --- SUBSTEPS ---
    double moveX = pPhysics.getVelocity().getX();
    double moveY = pPhysics.getVelocity().getY();

    int steps = (int)Math.max(Math.abs(moveX), Math.abs(moveY));
    steps = Math.min(steps, 8); // máximo 8 substeps
    if (steps < 1) steps = 1;

    double stepX = moveX / steps;
    double stepY = moveY / steps;

    for (int i = 0; i < steps; i++) {
        position.setX(position.getX() + stepX);
        if (checkCollision()) {
            position.setX(position.getX() - stepX); // rollback
        break;
        }

        position.setY(position.getY() + stepY);
        if (checkCollision()) {
            position.setY(position.getY() - stepY); // rollback
            enElSuelo = true;
        break;
        }
    }
    // balas
        if (KeyBoard.ei) {
            weaponS.tryShoot(
                position.getX() + texture.getWidth(),
                position.getY() + texture.getHeight() / 10,
                mirandoDerecha
            );
            WaitingTime++;
        } else {
            setWait(isShoot = false);
        }
        if(WaitingTime==0){
            weaponS.resetBurst();
        }

    // actualizar balas
    weaponS.update();
    updateBullets();
    if (enemies != null) {
        for (EnimyNormal e : enemies) {
            e.setMirandoDer(mirandoDerecha);
            e.setEnElsuelo(enElSuelo);
        }
    }
}
    @Override
public void onCollision(GameObjects other) {
    if (other instanceof Ambiente) {
    }
}

@Override

public void draw(Graphics g) {
    BufferedImage currentFrame;

    if (KeyBoard.right || KeyBoard.left) {
        animTick++;
        if (animTick >= 10) {
            frame++;
            animTick = 0;
        }

        // reiniciar frame si se pasa del límite
if (mirandoDerecha) {
    frame = frame % Assets.walkDere.length;
    currentFrame = Assets.walkDere[frame];
} else {
    frame = frame % Assets.walkHiz.length;
    currentFrame = Assets.walkHiz[frame];
}
    }
    else { currentFrame = Assets.cubo;
        frame = 0;
        animTick = 0;
    }
    g.drawImage(currentFrame, (int) position.getX(), (int) position.getY(), 37, 37, null);
    drawHitbox(g);
    drawBullets(g);
}

@Override
public Rectangle getBounds() {
    return createBounds(5, 5, 27, 38);
}
    public Player(Vector2D position, BufferedImage texture, GameState gameState) {
        super(position, texture);
        this.gameState = gameState;
        this.pPhysics = new PlayerPhysics();
    }
}