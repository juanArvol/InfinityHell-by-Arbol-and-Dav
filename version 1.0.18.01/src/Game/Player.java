package Game;

import Game.Bullets.Bullet;
import Game.Colisions.SystemColisions.CollisionVisitor;
import Game.Fisics.PhysicsStepper;
import Game.Fisics.PlayerPhysics;
import Game.Gameplay.Mechanics;
import States.GameState;
import entradas.KeyBoard;
import graficos.Assets;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import math.Vector2D;

public class Player extends MovingObjects {
    private PlayerPhysics pPhysics;
    private boolean congelado;
    private boolean mirandoDerecha = true;
    private boolean mArriba = false;
    private boolean mAbajo = false;
    private boolean agachado = false;
    private boolean running = KeyBoard.shift;
    private int frame = 0;
    private int animTick = 0;
    private int life = 100;
    private int lifeMax = 200;

    private boolean enElSuelo = false;

    private double waitingTime;
    private GameState gameState;
    private WeaponSelected weaponS;
    private ArrayList<EnimyNormal> enemies;
    private ArrayList<Bullet> bullets = new ArrayList<>();

    @Override
    public void acceptVisitor(CollisionVisitor visitor){
        visitor.visit(this);
    }
    @Override
    public Vector2D getPosition() {
        return position;
    }
    public PlayerPhysics getPlayerFisics(){
        return pPhysics;
    }
    public void setEnElSuelo(boolean enElSuelo) {
        this.enElSuelo = enElSuelo;
    }

    public void setWait(boolean isShoot) {
        waitingTime = isShoot ? ++waitingTime : --waitingTime;
        if (waitingTime <= 0) waitingTime = 0;
    }

    public void setEnemies(ArrayList<EnimyNormal> enemies) {
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

    public void setAgachado(boolean agachado) {
        this.agachado = agachado;
    }

    public boolean isAgachado() {
        return agachado;
    }

    public boolean isDer() {
        return mirandoDerecha;
    }

    public boolean isSuelo() {
        return enElSuelo;
    }

    public boolean isMarriva() {
        return mArriba;
    }

    public boolean isMabajo() {
        return mAbajo;
    }

    public boolean isMirandoAorA() {
        return mArriba || mAbajo;
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

    public void setAlive(boolean alive) {
        for (Bullet b : bullets) {
            if (alive) {
                b.setAlive();
            } else {
                b.setDead();
            }
        }
    }

    public void updateBullets() {
        /*bullets.removeIf(b -> !b.isAlive() ||
                b.getPosition().getX() < 0 ||
                b.getPosition().getX() > 1900);*/
        for (Bullet b : bullets) b.update();
    }

    public void setWeapon(WeaponSelected weaponS) {
        this.weaponS = weaponS;
    }

    public int getLife() {
        return life;
    }

    public int getLifeMax() {
        return lifeMax;
    }

    public void recibedDaimage(int value) {
        this.life -= value * 0;
        if (this.life < 0) {
            this.life = 0;
        }
    }

    @Override
    public void update() {
        running = false;
        double inputX = 0;

        //System.out.println("arriba: "+mArriba+" abajo: "+mAbajo);

        Mechanics.updateMechanics(this);

        if (!congelado) {
            if (KeyBoard.shift) {
                running = true;
                if (KeyBoard.right) {
                    inputX = 25;
                    mirandoDerecha = true;
                }
                if (KeyBoard.left) {
                    inputX = 25;
                    mirandoDerecha = false;
                }
            } else {
                if (KeyBoard.right) {
                    inputX = 1;
                    mirandoDerecha = true;
                }
                if (KeyBoard.left) {
                    inputX = 1;
                    mirandoDerecha = false;
                }
            }
        } else {
            if (KeyBoard.right) {
                mirandoDerecha = true;
            }
            if (KeyBoard.left) {
                mirandoDerecha = false;
            }
            if (KeyBoard.up) {
                mArriba = true;
                mAbajo = false;
            } else {
                mArriba = false;
            }
            if (KeyBoard.down) {
                mAbajo = true;
                mArriba = false;
            } else {
                mAbajo = false;
            }
        }

        // Saltar
        if ((KeyBoard.space || (KeyBoard.up && !KeyBoard.ei)) && enElSuelo && !congelado) {
            pPhysics.jump(17.5);  // salto ajustable
            enElSuelo = false;
        } else {
            pPhysics.setJumping(false);
        }

        pPhysics.moveX(inputX, enElSuelo, mirandoDerecha, running);
        pPhysics.applyGravity(enElSuelo);

        // --- SUBSTEPS ---
        double moveX = pPhysics.getVelocity().getX();
        double moveY = pPhysics.getVelocity().getY();

        PhysicsStepper.moveWith(this, moveX, moveY, gameState.getObjects());

        // balas
        if (KeyBoard.ei) {
            weaponS.tryShoot(
                position.getX() + texture.getWidth(),
                position.getY() + texture.getHeight() / 10,
                mirandoDerecha
            );
        } else {
            setWait(false);
        }

        if (waitingTime == 0) {
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
    public void onCollisionWith(Ambiente ambiente) {
        enElSuelo = true;
        pPhysics.getVelocity().setY(0);
    }

    @Override
    public void onCollisionWith(Bullet bullet) {
    }

    @Override
    public void onCollisionWith(EnimyNormal enemy) {
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
        } else {
            currentFrame = Assets.cubo;
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

    public Player(Vector2D spawn, BufferedImage texture, GameState gameState) {
        super(spawn, texture); //esto carga el spawn al Game Objects
        this.gameState = gameState;
        this.pPhysics = new PlayerPhysics();
    }
}
