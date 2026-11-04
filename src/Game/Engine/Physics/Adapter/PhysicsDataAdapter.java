package Game.Engine.Physics.Adapter;

/**
 * Adapter unificado para acceso a datos de física.
 * 
 * ── HRFC — Movement Unification ──────────────────────────────────────────
 * 
 * PROBLEMA: CollisionsSystem necesita mover TODOS los objetos dinámicos,
 * pero los datos están en dos lugares:
 *   - Entidades OOP: Physics2D.velocity (heap objects)
 *   - Proyectiles DOD: PrimitiveStorage arrays
 * 
 * SOLUCIÓN: Interfaz unificada que abstrae la fuente de datos.
 * CollisionsSystem usa el adapter sin conocer el modelo de almacenamiento.
 * 
 * ── RESPONSABILIDAD ──────────────────────────────────────────────────────
 * 
 * Proporciona acceso uniforme a:
 *   - Velocity (read/write)
 *   - Flags de física (onGround, onWall, onCeiling)
 *   - Propiedades físicas (mass, canPush)
 *   - Control de gravedad externa
 * 
 * ── IMPLEMENTACIONES ─────────────────────────────────────────────────────
 * 
 * OOPPhysicsAdapter: lee/escribe Physics2D (entidades convencionales)
 * DODPhysicsAdapter: lee/escribe PrimitiveStorage (proyectiles)
 * 
 * ── GARANTÍA DE PERFORMANCE ──────────────────────────────────────────────
 * 
 * Sin allocations: las implementaciones se crean una sola vez por objeto
 * y se reutilizan en cada frame. Las llamadas a métodos son inlineables.
 */
public interface PhysicsDataAdapter {
    
    // ── Velocity Access ───────────────────────────────────────────────────
    
    /** Velocity X en units/s */
    double getVelocityX();
    
    /** Velocity Y en units/s */
    double getVelocityY();
    
    /** Configura velocity X */
    void setVelocityX(double vx);
    
    /** Configura velocity Y */
    void setVelocityY(double vy);
    
    // ── Contact State Flags ───────────────────────────────────────────────
    
    /** True si el objeto está tocando el suelo */
    boolean isOnGround();
    
    /** Configura flag de contacto con suelo */
    void setOnGround(boolean onGround);
    
    /** True si el objeto está tocando una pared */
    boolean isOnWall();
    
    /** Configura flag de contacto con pared */
    void setOnWall(boolean onWall);
    
    /** True si el objeto está tocando el techo */
    boolean isOnCeiling();
    
    /** Configura flag de contacto con techo */
    void setOnCeiling(boolean onCeiling);
    
    // ── Physical Properties ───────────────────────────────────────────────
    
    /** Masa del objeto (para cálculos de empuje) */
    double getMass();
    
    /** True si el objeto puede empujar objetos pushables */
    boolean canPush();
    
    // ── Gravity Management ────────────────────────────────────────────────
    
    /** 
     * True si la gravedad es gestionada externamente.
     * Si true, CollisionsSystem NO aplica gravedad automática.
     */
    boolean isGravityManagedExternally();
    
    /** Aplica gravedad según estado de contacto */
    void applyGravity(boolean onGround, double deltaTime);
    
    // ── Surface Material ──────────────────────────────────────────────────
    
    /** Limpia el material de superficie almacenado */
    void clearSurface();
}
