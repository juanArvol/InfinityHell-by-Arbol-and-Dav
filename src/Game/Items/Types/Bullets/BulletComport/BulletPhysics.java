package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Física de proyectiles.
 *
 * Almacena y expone la velocidad del proyectil. La integración de posición
 * la hace CollisionsSystem en FASE 1B con Swept AABB, no Bullet.update().
 *
 * ── HRFC — Consolidación Final de Kinetic Physics ────────────────────────
 *
 * BulletPhysics ahora configura propiedades aerodinámicas por defecto
 * para proyectiles con GravityMovement. Los proyectiles son típicamente
 * pequeños y aerodinámicos, resultando en velocidades terminales altas.
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
     * @param xSpeed velocidad inicial en X (unidades/frame del sistema legacy @ 30 FPS)
     * @param ySpeed velocidad inicial en Y (unidades/frame del sistema legacy @ 30 FPS)
     * 
     * ── HRFC-DT-007 — Temporal Correctness ────────────────────────────────
     * 
     * CORRECCIÓN CRÍTICA: Conversión de unidades legacy a temporales.
     * 
     * PROBLEMA:
     *   Los valores de velocidad provienen del sistema legacy calibrado @ 30 FPS,
     *   expresados en units/frame. Sin embargo, Physics2D.velocity debe estar
     *   en units/s para integración temporal correcta.
     * 
     * CONVERSIÓN:
     *   velocity [units/s] = speed [units/frame @ 30 FPS] × 30
     * 
     * EJEMPLO:
     *   WeaponPistola.bulletSpeedBase = 10 units/frame
     *   → velocity = 10 × 30 = 300 units/s
     *   → A 30 FPS: Δx = 300 × (1/30) = 10 units/frame ✓
     * 
     * INVARIANTE PRESERVADO:
     *   El comportamiento observable a 30 FPS es idéntico al sistema legacy,
     *   pero ahora funciona correctamente a cualquier framerate.
     */
    public BulletPhysics(double xSpeed, double ySpeed) {
        super(0.0); // gravedad base cero — la gestiona ProjectileMovement
        setMass(1);
        
        // HRFC-DT-007: Conversión temporal de legacy units/frame a units/s
        // Sistema legacy calibrado @ 30 FPS → multiplicar por 30
        velocity.setX(xSpeed * 30.0);
        velocity.setY(ySpeed * 30.0);

        // ── Propiedades aerodinámicas (HRFC — Consolidación) ─────────────
        // HRFC FASE 2: Coeficientes escalados para px/frame.
        // Los proyectiles son típicamente pequeños y aerodinámicos.
        // Estos valores producen velocidades terminales altas (~40-50 px/frame).
        // Behaviors específicos (MetheorBullet) pueden ajustarlos.
        effectiveArea = 0.3;        // área pequeña
        dragCoefficient = 0.0001;   // forma muy aerodinámica (escalado para px/frame)
        // mediumDensity obsoleto — no se usa después de corrección de unidades
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
