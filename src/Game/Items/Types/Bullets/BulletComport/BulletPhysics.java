package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Física de proyectiles.
 *
 * Almacena y expone la velocidad del proyectil. La integración de posición
 * la hace CollisionsSystem en FASE 1B con Swept AABB, no Bullet.update().
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * La gravedad ya no se gestiona en BulletPhysics. Antes el constructor
 * recibía hasGravity y gravityValue, y Bullet.update() llamaba:
 *
 *   if (behavior.hasGravity()) getPhysics().applyGravity(false);
 *
 * Esa responsabilidad ahora pertenece a {@link Movement.GravityMovement},
 * una implementación de ProjectileMovement. BulletPhysics es solo un
 * almacén de velocidad con acceso conveniente para los behaviors.
 *
 * ── CollisionsSystem ─────────────────────────────────────────────────────
 *
 * isGravityManagedExternally() retorna true, indicando a CollisionsSystem
 * que NO aplique gravedad externa sobre este objeto (la gestiona el propio
 * sistema de movimiento del proyectil).
 *
 * ── lastContactNormal ─────────────────────────────────────────────────────
 *
 * CollisionsSystem escribe la normal del último impacto detectado via
 * Swept AABB antes de llamar a CollisionDispatcher.dispatch(). Los
 * BulletBehavior (especialmente BulletJump) leen estos valores para
 * determinar desde qué cara llegó el impacto, sin depender de los
 * flags onGround/onWall/onCeiling (que solo se establecen para SÓLIDOS).
 *
 * Convención de normales (igual que SweptAABB):
 *   normalX = -1 → impacto desde la derecha → cara izquierda del obstáculo
 *   normalX = +1 → impacto desde la izquierda → cara derecha del obstáculo
 *   normalY = -1 → impacto desde abajo → cara superior del obstáculo (suelo)
 *   normalY = +1 → impacto desde arriba → cara inferior del obstáculo (techo)
 *
 * El valor se resetea a (0, 0) al inicio de cada frame por CollisionsSystem.
 * Si el proyectil no tuvo contacto en este frame, ambos son 0.
 */
public class BulletPhysics extends Physics2D {

    /**
     * Normal horizontal del último contacto detectado este frame.
     * Escrito por CollisionsSystem ANTES del dispatch. Leído por BulletBehavior.
     * -1 = impacto desde derecha, +1 = impacto desde izquierda, 0 = sin contacto lateral.
     */
    private int lastContactNormalX = 0;

    /**
     * Normal vertical del último contacto detectado este frame.
     * Escrito por CollisionsSystem ANTES del dispatch. Leído por BulletBehavior.
     * -1 = impacto desde abajo (suelo), +1 = impacto desde arriba (techo), 0 = sin contacto vertical.
     */
    private int lastContactNormalY = 0;

    /**
     * @param xSpeed velocidad inicial en X (unidades/frame)
     * @param ySpeed velocidad inicial en Y (unidades/frame)
     */
    public BulletPhysics(double xSpeed, double ySpeed) {
        super(0.0); // gravedad base cero — la gestiona ProjectileMovement
        setMass(1);
        velocity.setX(xSpeed);
        velocity.setY(ySpeed);
    }

    /** CollisionsSystem NO aplica gravedad externa a este objeto. */
    @Override
    public boolean isGravityManagedExternally() { return true; }

    // ── API conveniente para behaviors y movimientos ──────────────────────

    public double getXspeed()          { return velocity.getX(); }
    public double getYspeed()          { return velocity.getY(); }
    public void   setXspeed(double xs) { velocity.setX(xs); }
    public void   setYspeed(double ys) { velocity.setY(ys); }

    public void accelerate(double fx, double fy) { addForce(fx, fy); }

    @Override public void stopX()        { velocity.setX(0); }
    @Override public void stopY()        { velocity.setY(0); }
    @Override public void stopVelocity() { stopX(); stopY(); }

    // ── Normal de último contacto ─────────────────────────────────────────

    /**
     * Escribe la normal del impacto detectado por CollisionsSystem.
     * Llamado por CollisionsSystem en FASE 1B inmediatamente antes del dispatch.
     *
     * @param nx normal horizontal (-1, 0, +1)
     * @param ny normal vertical   (-1, 0, +1)
     */
    public void setLastContactNormal(int nx, int ny) {
        this.lastContactNormalX = nx;
        this.lastContactNormalY = ny;
    }

    /**
     * Resetea la normal al estado "sin contacto".
     * Llamado por CollisionsSystem al inicio de cada frame (FASE 0.5 de triggers).
     */
    public void clearLastContactNormal() {
        this.lastContactNormalX = 0;
        this.lastContactNormalY = 0;
    }

    /**
     * Normal horizontal del impacto más reciente detectado por CollisionsSystem.
     * -1 = impacto desde derecha, +1 = desde izquierda, 0 = sin contacto lateral.
     * Válido durante onCollision() — CollisionsSystem lo escribe antes del dispatch.
     */
    public int getLastContactNormalX() { return lastContactNormalX; }

    /**
     * Normal vertical del impacto más reciente detectado por CollisionsSystem.
     * -1 = impacto desde abajo (suelo), +1 = desde arriba (techo), 0 = sin contacto vertical.
     * Válido durante onCollision() — CollisionsSystem lo escribe antes del dispatch.
     */
    public int getLastContactNormalY() { return lastContactNormalY; }

    /**
     * True si hay una normal de contacto válida para este frame.
     * Útil para distinguir entre "tuve un impacto" y "no tuve impacto".
     */
    public boolean hasContactNormal() {
        return lastContactNormalX != 0 || lastContactNormalY != 0;
    }
}
