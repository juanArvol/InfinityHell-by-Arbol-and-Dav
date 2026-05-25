package Game.Fisics;

import Game.World.Surface.SurfaceMaterial;
import GameMath.Vector2D;

/**
 * Clase base de física.
 *
 * ── Fricción dinámica por superficie ────────────────────────────────────
 *
 * currentSurface se asigna por CollisionsSystem (Fase 0) DESPUÉS de que
 * cada objeto llama su update(). El surface se mantiene frame a frame:
 * si estás en el suelo, el surface del frame anterior es el correcto.
 * Solo cambia cuando CollisionsSystem detecta cambio de superficie.
 *
 * ── drag vs friction ────────────────────────────────────────────────────
 *
 *   friction — escala la aceleración ACTIVA (cuando hay input).
 *              Hielo: friction=0.05 → casi no podés acelerar.
 *
 *   drag     — amortiguación PASIVA de vx (cuando NO hay input).
 *              Hielo: drag=0.99 → se desliza largo.
 *              Barro: drag=0.60 → para casi instantáneo.
 *
 * IMPORTANTE: drag NO reemplaza al 'slide' que tenías en PlayerPhysics.
 * El slide original se asignaba dentro de moveX() y se aplicaba como
 * multiplicador de vx al final. Aquí hace lo mismo pero el valor
 * viene del material en contacto en lugar de estar hardcodeado.
 */
public class Physics {

    protected Vector2D velocity = new Vector2D(0, 0);
    protected double gravity;

    protected double mass        = 1.0;
    protected double aAir;
    protected double speedMaxAir;
    protected double speedMaxPiso;
    protected double aGround;
    protected double slide;       // mantenido por compatibilidad con subclases existentes
    protected double speedMax;
    protected double bonus;       // mantenido por compatibilidad con subclases existentes

    protected double inputX;
    protected double vx;
    protected double accel;

    protected boolean onGround;
    protected boolean salto;
    protected boolean running;

    protected double maxFallSpeed = 20.0;

    // Surface actual. DEFAULT garantiza que el primer frame tenga valores sensatos.
    protected SurfaceMaterial currentSurface = SurfaceMaterial.DEFAULT;

    public Physics(double gravity) {
        this.gravity = gravity;
    }

    // ── Gravedad externa ──────────────────────────────────────────────────
    // Sobreescribir en BulletPhysics para evitar doble gravedad.
    public boolean isGravityManagedExternally() { return false; }

    // ── Surface ───────────────────────────────────────────────────────────

    public void setCurrentSurface(SurfaceMaterial surface) {
        this.currentSurface = (surface != null) ? surface : SurfaceMaterial.DEFAULT;
    }

    public void clearSurface() {
        this.currentSurface = SurfaceMaterial.AIR;
    }

    public SurfaceMaterial getCurrentSurface() { return currentSurface; }

    // ── Movimiento horizontal ─────────────────────────────────────────────

    /**
     * Calcula la velocidad horizontal para este frame.
     *
     * Combina el sistema de aceleración/slide original (que funcionaba)
     * con la fricción de superficie (lo nuevo que querías).
     *
     * La friction ESCALA el slide y la aceleración — no los reemplaza.
     * Así el movimiento base sigue igual en superficie normal (friction=1.0)
     * y solo cambia en superficies especiales.
     *
     * @param inputX   -1 izq, +1 der, 0 quieto.
     * @param onGround Si el objeto está en el suelo.
     * @param running  Si está corriendo.
     */
    public void moveX(double inputX, boolean onGround, boolean running) {
        this.onGround = onGround;
        this.inputX   = inputX;
        this.running  = running;

        setMaxSpeed(onGround);

        accel = onGround ? aGround : aAir;
        bonus = onGround ? 1.0 : 0.8;

        // friction escala cuánto agarra la superficie (1.0 = normal, <1 = resbala)
        double friction = currentSurface.getFriction();
        // drag escala el slide — cuánto tarda en parar al soltar input (1.0 = sin freno extra)
        double drag     = currentSurface.getDrag();

        double mAccel = (accel / mass) * friction;

        vx = velocity.getX() + (inputX * mAccel) * bonus;

        // Clamp de velocidad máxima
        if (inputX != 0 && Math.abs(vx) >= speedMax) {
            vx = Math.copySign(speedMax, vx);
        }

        // Frenado pasivo al soltar input: slide del material
        // slide base de la subclase (0.9 suelo, 0.74 aire) modulado por drag del material
        if (inputX == 0) {
            double effectiveDrag = slide * drag;
            vx = velocity.getX() * effectiveDrag;
            if (Math.abs(vx) < 0.05) vx = 0;
        }

        vSetX(vx);
    }

    public void setMaxSpeed(boolean onGround) {
        speedMax = onGround ? speedMaxPiso : speedMaxAir;
        if (Math.abs(vx) > speedMax - accel) {
            speedMax = Math.max(0, speedMax - accel);
        }
    }

    // ── Gravedad ──────────────────────────────────────────────────────────

    public void applyGravity(boolean onGround) {
        if (!onGround) {
            double newVy = velocity.getY() + (gravity * mass);
            velocity.setY(Math.min(newVy, maxFallSpeed));
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────

    public void jump(double force) {
        velocity.setY(-force / mass);
        salto = true;
    }

    public void updateMoves(Vector2D position) {
        position.setX(position.getX() + velocity.getX());
        position.setY(position.getY() + velocity.getY());
    }

    public void stopY()        { velocity.setY(0); }
    public void stopX()        { velocity.setX(0); vx = 0; }
    public void stopVelocity() { stopX(); stopY(); }

    public void vSetX(double vX) { velocity.setX(vX); }
    public void vSetY(double vY) { velocity.setY(vY); }

    public void setMass(double m)           { this.mass = m; }
    public double getMass()                 { return mass; }
    public void setJumping(boolean jumping) { this.salto = jumping; }
    public Vector2D getVelocity()           { return velocity; }
    public void setMaxFallSpeed(double s)   { this.maxFallSpeed = s; }
    public double getGravity()              { return gravity; }
    public void setGravity(double g)        { this.gravity = g; }
    public double getOposite(double x)      { return -x; }

    public void addForce(double fx, double fy) {
        velocity.setX(velocity.getX() + (fx / mass));
        velocity.setY(velocity.getY() + (fy / mass));
    }

    public boolean getOnGround()       { return onGround; }
    public void setOnGround(boolean v) { this.onGround = v; }

    public void showInfo(boolean yes) {
        if (yes) {
            System.out.printf(
                "inputX:%.2f accel:%.2f velX:%.4f velY:%.4f onGround:%b friction:%.2f drag:%.2f%n",
                inputX, accel, velocity.getX(), velocity.getY(),
                onGround, currentSurface.getFriction(), currentSurface.getDrag()
            );
        }
    }
}
