package Game.Fisics;

import GameMath.Vector2D;

/**
 * Clase base de física.
 *
 * FIX BUG-005: agregado isGravityManagedExternally() para que CollisionsSystem
 * no aplique gravedad a objetos (como Bullet) que ya gestionan su propia gravedad.
 *
 * REFACTOR: limpieza de nombres de variables crípticos.
 * Los campos 'salto', 'count', 'dir', 'bonus', 'vx', 'vy' internos
 * se mantienen por compatibilidad pero se documenta su propósito.
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

    protected double inputX;        // Input horizontal del frame actual
    protected double vx;            // Velocidad X de trabajo (cálculo interno)
    protected double vy;            // Velocidad Y de trabajo (cálculo interno)
    protected double dir;           // Dirección: +1 derecha, -1 izquierda
    protected double accel;         // Aceleración aplicada en este frame

    protected boolean onGround;
    protected boolean direction;    // true = derecha
    protected boolean salto;        // true = está en salto
    protected boolean running;
    protected byte count = 0;       // uso: no usado actualmente (deuda técnica)

    public Physics(double gravity) {
        this.gravity = gravity;
    }

    /**
     * FIX BUG-005: indica si este objeto gestiona su propia gravedad externamente.
     * Si retorna true, CollisionsSystem NO aplicará gravedad a este objeto,
     * evitando doble aplicación.
     *
     * Sobreescribir en BulletPhysics para retornar true.
     */
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

    public void moveX(double inputX, boolean onGround, boolean direction, boolean running) {
        this.onGround  = onGround;
        this.direction = direction;
        this.inputX    = inputX;
        this.running   = running;

        setMaxSpeed(onGround);

        dir   = direction ? 1 : -1;
        accel = onGround ? aGround : aAir;
        double mAccel = accel / mass;
        bonus = onGround ? 1 : 0.8;

        vx = velocity.getX() + ((inputX * dir) * mAccel) * bonus;

        if (inputX != 0) {
            if (Math.abs(vx) >= speedMax) {
                vx = dir * speedMax;
            }
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
        if (Math.abs(vx) > speedMax - accel) {
            speedMax = speedMax - accel;
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
            System.out.printf("inputX:%.2f dir:%.0f accel:%.2f velX:%.2f velY:%.2f%n",
                inputX, dir, accel, velocity.getX(), velocity.getY());
        }
    }
}
