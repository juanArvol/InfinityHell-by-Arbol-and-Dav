package Game.Engine.Physics.Adapter;

import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

/**
 * Adapter para acceso a Physics2D (entidades OOP).
 * 
 * ── HRFC — Movement Unification ──────────────────────────────────────────
 * 
 * Implementación del adapter para entidades convencionales (Player, Enemy)
 * que almacenan datos de física en objetos heap (Physics2D).
 * 
 * ── DELEGACIÓN DIRECTA ───────────────────────────────────────────────────
 * 
 * Todos los métodos delegan a Physics2D sin transformaciones.
 * El compilador puede inlinear estas llamadas completamente.
 * 
 * ── LIFECYCLE ────────────────────────────────────────────────────────────
 * 
 * Instancia creada una sola vez por objeto y cacheada.
 * Zero allocations después de la inicialización.
 */
public final class OOPPhysicsAdapter implements PhysicsDataAdapter {
    
    private final Physics2D physics;
    
    public OOPPhysicsAdapter(Physics2D physics) {
        this.physics = physics;
    }
    
    // ── Velocity Access ───────────────────────────────────────────────────
    
    @Override
    public double getVelocityX() {
        return physics.getVelocity().getX();
    }
    
    @Override
    public double getVelocityY() {
        return physics.getVelocity().getY();
    }
    
    @Override
    public void setVelocityX(double vx) {
        physics.vSetX(vx);
    }
    
    @Override
    public void setVelocityY(double vy) {
        physics.vSetY(vy);
    }
    
    // ── Contact State Flags ───────────────────────────────────────────────
    
    @Override
    public boolean isOnGround() {
        return physics.getOnGround();
    }
    
    @Override
    public void setOnGround(boolean onGround) {
        physics.setOnGround(onGround);
    }
    
    @Override
    public boolean isOnWall() {
        return physics.getOnWall();
    }
    
    @Override
    public void setOnWall(boolean onWall) {
        physics.setOnWall(onWall);
    }
    
    @Override
    public boolean isOnCeiling() {
        return physics.getOnCeiling();
    }
    
    @Override
    public void setOnCeiling(boolean onCeiling) {
        physics.setOnCeiling(onCeiling);
    }
    
    // ── Physical Properties ───────────────────────────────────────────────
    
    @Override
    public double getMass() {
        return physics.getMass();
    }
    
    @Override
    public boolean canPush() {
        return physics.canPush();
    }
    
    // ── Gravity Management ────────────────────────────────────────────────
    
    @Override
    public boolean isGravityManagedExternally() {
        return physics.isGravityManagedExternally();
    }
    
    @Override
    public void applyGravity(boolean onGround, double deltaTime) {
        physics.applyGravity(onGround, deltaTime);
    }
    
    // ── Surface Material ──────────────────────────────────────────────────
    
    @Override
    public void clearSurface() {
        physics.clearSurface();
    }
}
