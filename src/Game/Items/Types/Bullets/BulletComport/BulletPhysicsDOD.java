package Game.Items.Types.Bullets.BulletComport;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Adapter de física de proyectiles sobre DOD storage.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * BulletPhysicsDOD conserva la API de BulletPhysics pero lee/escribe
 * directamente en PrimitiveStorage via SimulationHandle.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 * BulletPhysicsDOD     → API de dominio para modificar velocity
 * PrimitiveStorage     → arrays densos con velocity data
 * MovementSystem       → integra position += velocity * dt
 * AccelerationSystem   → integra velocity += acceleration * dt
 *
 * ── API CONSERVADA ───────────────────────────────────────────────────────
 *
 * Todos los métodos de BulletPhysics están disponibles:
 * - getXspeed(), getYspeed()
 * - setXspeed(), setYspeed()
 * - getVelocity(), setVelocity()
 * - setAcceleration(), setGravityScale()
 * - stopX(), stopY(), stopVelocity()
 *
 * Los behaviors NO necesitan cambios — acceden a physics igual que antes.
 *
 * ── DIFERENCIAS CON BulletPhysics ────────────────────────────────────────
 *
 * BulletPhysics hereda de Physics2D y tiene estado interno (Vector2D velocity).
 * BulletPhysicsDOD NO tiene estado — solo wrapper sobre storage arrays.
 *
 * Ventaja: sin object allocation, sin indirección, acceso directo a arrays.
 *
 * ── VALIDACIÓN DE HANDLE ─────────────────────────────────────────────────
 *
 * NO se valida el handle en cada acceso — asumimos que Bullet mantiene
 * un handle válido y lo revalida después de compact() si es necesario.
 *
 * Validar en cada get/set destruiría el beneficio de performance del DOD.
 */
public final class BulletPhysicsDOD {

    private final PrimitiveStorage storage;
    private final SimulationHandle handle;

    /**
     * Constructor desde EntityStore.
     *
     * @param storage PrimitiveStorage compartido
     * @param handle handle válido de la bullet
     */
    public BulletPhysicsDOD(PrimitiveStorage storage, SimulationHandle handle) {
        this.storage = storage;
        this.handle = handle;
    }

    // ── Accessors directos ────────────────────────────────────────────────

    public double getXspeed() {
        return storage.velocitiesX()[handle.index()];
    }

    public double getYspeed() {
        return storage.velocitiesY()[handle.index()];
    }

    public void setXspeed(double xs) {
        storage.velocitiesX()[handle.index()] = (float) xs;
    }

    public void setYspeed(double ys) {
        storage.velocitiesY()[handle.index()] = (float) ys;
    }

    // ── API Vector2D ──────────────────────────────────────────────────────

    /**
     * Retorna velocity como Vector2D.
     * ALLOCATION: crea nuevo Vector2D en cada llamada.
     * Evitar en hot paths — usar getXspeed()/getYspeed() directamente.
     */
    public Vector2D getVelocity() {
        int idx = handle.index();
        return new Vector2D(
            storage.velocitiesX()[idx],
            storage.velocitiesY()[idx]
        );
    }

    public void setVelocity(Vector2D vel) {
        setVelocity(vel.getX(), vel.getY());
    }

    public void setVelocity(double vx, double vy) {
        int idx = handle.index();
        storage.velocitiesX()[idx] = (float) vx;
        storage.velocitiesY()[idx] = (float) vy;
    }

    public void addVelocity(double dvx, double dvy) {
        int idx = handle.index();
        storage.velocitiesX()[idx] += (float) dvx;
        storage.velocitiesY()[idx] += (float) dvy;
    }

    // ── Acceleration API ──────────────────────────────────────────────────

    /**
     * Configura acceleration directamente.
     * AccelerationSystem integrará estos valores en velocity.
     *
     * IMPORTANTE: Esta es la API preferida para ProjectileMovement.
     * No modificar velocity directamente si hay acceleration — causa doble integración.
     */
    public void setAcceleration(double ax, double ay) {
        int idx = handle.index();
        storage.accelerationsX()[idx] = (float) ax;
        storage.accelerationsY()[idx] = (float) ay;
    }

    public void addAcceleration(double dax, double day) {
        int idx = handle.index();
        storage.accelerationsX()[idx] += (float) dax;
        storage.accelerationsY()[idx] += (float) day;
    }

    public double getAccelerationX() {
        return storage.accelerationsX()[handle.index()];
    }

    public double getAccelerationY() {
        return storage.accelerationsY()[handle.index()];
    }

    // ── Gravity Scale ─────────────────────────────────────────────────────

    /**
     * Configura el multiplicador de gravedad.
     * AccelerationSystem aplicará: velY += (accY + GRAVITY * gravityScale) * dt
     */
    public void setGravityScale(double scale) {
        storage.gravityScale()[handle.index()] = (float) scale;
    }

    public double getGravityScale() {
        return storage.gravityScale()[handle.index()];
    }

    // ── Physical Properties ───────────────────────────────────────────────

    public void setMass(double mass) {
        storage.mass()[handle.index()] = (float) mass;
    }

    public double getMass() {
        return storage.mass()[handle.index()];
    }

    public void setDrag(double drag) {
        storage.drag()[handle.index()] = (float) drag;
    }

    public double getDrag() {
        return storage.drag()[handle.index()];
    }

    // ── Stop helpers ──────────────────────────────────────────────────────

    public void stopX() {
        storage.velocitiesX()[handle.index()] = 0f;
    }

    public void stopY() {
        storage.velocitiesY()[handle.index()] = 0f;
    }

    public void stopVelocity() {
        int idx = handle.index();
        storage.velocitiesX()[idx] = 0f;
        storage.velocitiesY()[idx] = 0f;
    }

    // ── Collision Normal (legacy compatibility) ───────────────────────────
    // Estos datos NO migran a DOD en FASE 1 — se mantienen en Bullet como
    // campos locales. API expuesta para que BulletBehavior no note diferencia.

    private int lastContactNormalX = 0;
    private int lastContactNormalY = 0;

    public void setLastContactNormal(int nx, int ny) {
        this.lastContactNormalX = nx;
        this.lastContactNormalY = ny;
    }

    public void clearLastContactNormal() {
        this.lastContactNormalX = 0;
        this.lastContactNormalY = 0;
    }

    public int getLastContactNormalX() {
        return lastContactNormalX;
    }

    public int getLastContactNormalY() {
        return lastContactNormalY;
    }

    public boolean hasContactNormal() {
        return lastContactNormalX != 0 || lastContactNormalY != 0;
    }
}
