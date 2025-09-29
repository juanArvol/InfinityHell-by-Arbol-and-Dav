package Game;

import Game.Gameplay.Mechanics;
import graficos.Assets;
import imput.KeyBoard;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import math.Vector2D;

public class Player extends MovingObjects {
    private boolean congelado = false;
    private boolean mirandoDerecha = true;
    private int frame = 0;
    private int animTick = 0;

    private double velocidadY = 0;
    private final double gravedad = 0.78;
    private boolean enElSuelo = false;

    private WeaponSelected weaponS;
    
@Override
public Vector2D getPosition() {
    return position;
}

public void setEnElSuelo(boolean enElSuelo) {
    this.enElSuelo = enElSuelo;
}

public void setMirandoDerecha(boolean mirandoDerecha) {
    this.mirandoDerecha = mirandoDerecha;
}

public void setCongelado(boolean congelado) {
    this.congelado = congelado;
}
/*public void setVelocidadY(double velocidadY) {
    this.velocidadY = velocidadY;
}*/
public void setBulletAngle(double bulletAngle){
    weaponS.setBulletA(bulletAngle);
}
public void setBulletDirY(double bulletDirectionY) {
    weaponS.setBulletDirY(bulletDirectionY);
}

public void setBulletDirX(double bulletDirectionX) {
    weaponS.setBulletDirX(bulletDirectionX);
}

@Override
public void update() {
    // gravedad
    velocidadY += gravedad;
    position.setY(position.getY() + velocidadY);

    Mechanics.updateMechanics(this);

    // movimiento horizontal
    if(!congelado){
        if (KeyBoard.right)      {
            position.setX(position.getX() + 4);
            mirandoDerecha = true;
        }
        if (KeyBoard.left) {
            position.setX(position.getX() - 4);
            mirandoDerecha = false;
        }
        
    // salto
        if ((KeyBoard.space || KeyBoard.up) && enElSuelo && KeyBoard.c==false) {
            velocidadY = -12.5;
            enElSuelo = true;
        }
    }else{
        if(KeyBoard.right){
            mirandoDerecha = true;
        }
        if(KeyBoard.left){
            mirandoDerecha = false;
        }
    }
    // balas
        if (KeyBoard.ei) {
            weaponS.tryShoot(
                position.getX() + texture.getWidth(),
                position.getY() + texture.getHeight() / 10,
                mirandoDerecha
            );
        } else {
            // si suelta el botón, resetea la ráfaga
            weaponS.resetBurst();
        }

    // actualizar balas
    weaponS.update();

}
    @Override
public void onCollision(GameObjects other) {
    if (other instanceof Ambiente) {
        Rectangle suelo = other.getBounds();

        // solo corregimos si viene cayendo, no si está subiendo
        if (velocidadY >= 0) {
            position.setY(suelo.getY() - getBounds().height);
            velocidadY = 0;
            enElSuelo = true;
        }
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

    for (Bullet b : weaponS.getBullets()) {
            b.draw(g);
        }

    drawHitbox(g);
}

@Override
public Rectangle getBounds() {
    return createBounds(5, 5, 27, 38);
}
    public Player(Vector2D position, BufferedImage texture) {
        super(position, texture);
        this.weaponS = new WeaponSelected(2,1);
    }
}