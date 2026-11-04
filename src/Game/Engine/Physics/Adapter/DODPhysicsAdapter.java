package Game.Engine.Physics.Adapter;

import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.PrimitiveStorage;

/**
 * Adapter para acceso a PrimitiveStorage (proyectiles DOD).
 * 
 * ── HRFC — Movement Unification ──────────────────────────────────────────
 * 
 * Implementación del adapter para proyectiles que almacenan datos de física
 * en arrays densos (PrimitiveStorage).
 * 
 * ── ACCESO DIRECTO A ARRAYS ──────────────────────────────────────────────
 * 
 * Lee/escribe directamente en arrays sin indirecciones.
 * Mantiene la performance del modelo DOD.
 * 
 * ── SIMPLIFICACIONES PARA PROYECTILES ────────────────────────────────────
 * 
 * Los proyectiles tienen física simplificada:
 *   - Sin contactState (onGround/onWall/onCeiling siempre false)
 *   - Sin empuje (canPush siempre false)
 *   - Gravedad gestionada por AccelerationSystem (isGravityManagedExternally = true)
 * 
 * Esto evita almacenar datos innecesarios en los arrays.
 * 
 * ── LIFECYCLE ────────────────────────────────────────────────────────────
 * 
 * Instancia creada una sola vez por bullet y cacheada.
 * Zero allocations después de la inicialización.
 */
public final class DODPhysicsAdapter implements PhysicsDataAdapter {
    
    private final PrimitiveStorage storage;
    private final SimulationHandle handle;
    
    public DODPhysicsAdapter(PrimitiveStorage storage, SimulationHandle handle) {
        this.storage = storage;
        this.handle = handle;
    }
    
    // ── Velocity Access ───────────────────────────────────────────────────
    
    @Override
    public double getVelocityX() {
        return storage.velocitiesX()[handle.index()];
    }
    
    @Override
    public double getVelocityY() {
        return storage.velocitiesY()[handle.index()];
    }
    
    @Override
    public void setVelocityX(double vx) {
        storage.velocitiesX()[handle.index()] = (float) vx;
    }
    
    @Override
    public void setVelocityY(double vy) {
        storage.velocitiesY()[handle.index()] = (float) vy;
    }
    
    // ── Contact State Flags ───────────────────────────────────────────────
    // Proyectiles NO tienen contactState — siempre false
    
    @Override
    public boolean isOnGround() {
        return false;
    }
    
    @Override
    public void setOnGround(boolean onGround) {
        // No-op: proyectiles no tienen contactState
    }
    
    @Override
    public boolean isOnWall() {
        return false;
    }
    
    @Override
    public void setOnWall(boolean onWall) {
        // No-op: proyectiles no tienen contactState
    }
    
    @Override
    public boolean isOnCeiling() {
        return false;
    }
    
    @Override
    public void setOnCeiling(boolean onCeiling) {
        // No-op: proyectiles no tienen contactState
    }
    
    // ── Physical Properties ───────────────────────────────────────────────
    
    @Override
    public double getMass() {
        return storage.mass()[handle.index()];
    }
    
    @Override
    public boolean canPush() {
        // Proyectiles NO empujan objetos
        return false;
    }
    
    // ── Gravity Management ────────────────────────────────────────────────
    
    @Override
    public boolean isGravityManagedExternally() {
        // AccelerationSystem gestiona gravedad de proyectiles
        return true;
    }
    
    @Override
    public void applyGravity(boolean onGround, double deltaTime) {
        // No-op: AccelerationSystem aplica gravedad vía gravityScale
    }
    
    // ── Surface Material ──────────────────────────────────────────────────
    
    @Override
    public void clearSurface() {
        // No-op: proyectiles no almacenan surface material
    }
}
