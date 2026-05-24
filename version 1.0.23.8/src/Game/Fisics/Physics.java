package Game.Fisics;

import GameMath.Vector2D;

/**
 * Clase base de física.
 */
public class Physics {

    protected Vector2D velocity = new Vector2D(0, 0);
    protected double gravity;

    protected double mass;
    protected double aAir;          // Aceleración en el aire
    protected double speedMaxAir;   // Velocidad máxima en el aire
    protected double speedMaxPiso;  // Velocidad máxima en el suelo
    protected double aGround;       // Aceleración en el suelo
    protected double slide;         // Factor de deslizamiento al frenar
    protected double speedMax;      // Velocidad máxima actual (calculada)
    protected double bonus;         // Multiplicador de aceleración según entorno

    protected double inputX;        // Input horizontal del frame actual (-1, 0, +1)
    protected double vx;            // Velocidad X de trabajo (cálculo interno)
    protected double vy;            // Velocidad Y de trabajo (cálculo interno)
    protected double accel;         // Aceleración aplicada en este frame

    protected boolean onGround;
    protected boolean salto;        // true = está en salto
    protected boolean running;
    protected byte count = 0;

    public Physics(double gravity) {
        this.gravity = gravity;
    }

    public boolean isGravityManagedExternally() {
        return false;
    }

    public void setMass(double m) { this.mass = m; }
    public double getMass()       { return mass; }

    public void vSetX(double vX) { velocity.setX(vX); }
    public void vSetY(double vY) { velocity.setY(vY); }

    public void setJumping(boolean jumping) { this.salto = jumping; }

    public void stopY() { velocity.setY(0); }
    public void stopX() { velocity.setX(0); vx = 0; }
    public void stopVelocity() { stopX(); stopY(); }

    public void jump(double force) {
        velocity.setY(-force / mass);
        salto = true;
    }

    /**
     * Calcula la velocidad horizontal para este frame.
     *
     * @param inputX  Dirección con signo: -1 izquierda, +1 derecha, 0 quieto.
     * @param onGround Si el objeto está en el suelo.
     * @param running  Si está corriendo.
     *
     * FIX oscilación: la versión anterior tenía un campo 'dir' que debía
     * calcularse como Math.signum(inputX), pero nunca se asignaba aquí.
     * Cuando inputX != 0 y vx >= speedMax, se hacía vx = dir * speedMax
     * con dir = 0 → vx = 0. Al frame siguiente volvía a acelerar → oscilación.
     *
     * Solución: inputX ya viene con signo propio. El clamp de velocidad
     * usa Math.copySign para preservar la dirección correctamente.
     */
    public void moveX(double inputX, boolean onGround, boolean running) {
        this.onGround = onGround;
        this.inputX   = inputX;
        this.running  = running;

        setMaxSpeed(onGround);

        accel         = onGround ? aGround : aAir;
        double mAccel = accel / mass;
        bonus         = onGround ? 1.0 : 0.8;

        vx = velocity.getX() + (inputX * mAccel) * bonus;

        // Clamp: si se supera la velocidad máxima, recortar preservando la dirección.
        // FIX: usar Math.copySign(speedMax, vx) en lugar del 'dir' que nunca se asignaba.
        if (inputX != 0 && Math.abs(vx) >= speedMax) {
            vx = Math.copySign(speedMax, vx);
        }

        vSetX(vx);
    }

    public void moveY(double inputY, boolean hasGravity) {
        if (hasGravity) applyGravity(onGround);
        vSetY(inputY * aAir);
    }

    public void updateMoves(Vector2D position) {
        position.setX(position.getX() + velocity.getX());
        position.setY(position.getY() + velocity.getY());
    }

    public Vector2D getVelocity() { return velocity; }

    public void setMaxSpeed(boolean onGround) {
        speedMax = onGround ? speedMaxPiso : speedMaxAir;
        // Reducción suave al acercarse al máximo (evita clipping brusco)
        if (Math.abs(vx) > speedMax - accel) {
            speedMax = Math.max(0, speedMax - accel);
        }
    }

    public void applyGravity(boolean onGround) {
        if (!onGround) {
            velocity.setY(velocity.getY() + (gravity * mass));
        }
    }

    public double getGravity()         { return gravity; }
    public void   setGravity(double g) { gravity = g; }

    public void addForce(double fx, double fy) {
        velocity.setX(velocity.getX() + (fx / mass));
        velocity.setY(velocity.getY() + (fy / mass));
    }

    public double getOposite(double x) { return -x; }
    public boolean getOnGround()       { return onGround; }
    public void setOnGround(boolean v) { this.onGround = v; }

    public void showInfo(boolean yes) {
        if (yes) {
            System.out.printf("inputX:%.2f accel:%.2f velX:%.2f velY:%.2f%n",
                inputX, accel, velocity.getX(), velocity.getY());
        }
    }
}
