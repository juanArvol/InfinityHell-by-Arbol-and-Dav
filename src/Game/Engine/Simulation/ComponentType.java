package Game.Engine.Simulation;

/**
 * Tipos de componentes para ComponentMask.
 *
 * ── HRFC — Projectile DOD Migration ──────────────────────────────────────
 *
 * Define los IDs de bits para cada tipo de componente en PrimitiveStorage.
 * Usado para construir ComponentMask que indica qué datos tiene una entidad.
 *
 * ── CONVENCIÓN ───────────────────────────────────────────────────────────
 *
 * Los IDs deben estar en el rango [0, 63] para caber en un long bitmask.
 * Organizados por dominio para claridad.
 *
 * ── LIMPIEZA DE CAMPOS ───────────────────────────────────────────────────
 *
 * Se eliminaron los IDs de campos que NO existen en PrimitiveStorage:
 *   - HEALTH (health/maxHealth ya no están en DOD, solo en HealthComponent OOP)
 *   - COLLISION_BOUNDS/MASKS (manejado por ColliderComponent OOP)
 *   - SPATIAL_HASH/REGION_ID (no hay sistema espacial DOD)
 *   - AGE (no se usa)
 *   - TYPE_ID/BEHAVIOR_ID (metadata no crítica)
 *
 * Se mantienen solo los campos que realmente existen y se usan.
 *
 * ── EXTENSIÓN ────────────────────────────────────────────────────────────
 *
 * Al agregar un nuevo componente:
 * 1. Agregar campo en PrimitiveStorage
 * 2. Agregar constante aquí con ID único
 * 3. Actualizar sistemas que lo requieran
 */
public final class ComponentType {

    // ── Kinematic (0-7) ───────────────────────────────────────────────────
    public static final int POSITION         = 0;  // positionsX/Y
    public static final int VELOCITY         = 1;  // velocitiesX/Y
    public static final int ACCELERATION     = 2;  // accelerationsX/Y
    public static final int ROTATION         = 3;  // rotations (visual rotation)
    public static final int ANGULAR_VELOCITY = 4;  // angularVelocities (rad/s)

    // ── Physics (8-15) ────────────────────────────────────────────────────
    public static final int MASS         = 8;  // mass
    public static final int DRAG         = 9;  // drag
    public static final int GRAVITY_SCALE = 10; // gravityScale

    // ── Lifetime (16-23) ──────────────────────────────────────────────────
    public static final int LIFETIME     = 16; // lifetimes

    // ── Metadata (24-31) ──────────────────────────────────────────────────
    public static final int OWNER        = 24; // ownerEntityIds (quien disparó)

    // ── State (32-39) ─────────────────────────────────────────────────────
    public static final int FLAGS        = 32; // flags (bitfield de estado)
    public static final int DAMAGE       = 33; // damage (daño que inflige)

    // ── Máscaras predefinidas ─────────────────────────────────────────────

    /**
     * Máscara para proyectiles (Bullet).
     * Incluye los datos hot que se procesan en batch:
     * - Position, Velocity, Acceleration (integración cinemática)
     * - Rotation, AngularVelocity (rotación visual)
     * - Lifetime (expiración temporal)
     * - Damage (impacto)
     * - Flags (estado)
     * - GravityScale, Mass, Drag (física)
     * - Owner (tracking de quien disparó)
     */
    public static final ComponentMask PROJECTILE_MASK = ComponentMask.EMPTY
        .with(POSITION)
        .with(VELOCITY)
        .with(ACCELERATION)
        .with(ROTATION)
        .with(ANGULAR_VELOCITY)
        .with(LIFETIME)
        .with(DAMAGE)
        .with(FLAGS)
        .with(GRAVITY_SCALE)
        .with(MASS)
        .with(DRAG)
        .with(OWNER);

    /**
     * Máscara para entidades con física completa.
     */
    public static final ComponentMask PHYSICS_MASK = ComponentMask.EMPTY
        .with(POSITION)
        .with(VELOCITY)
        .with(ACCELERATION)
        .with(MASS)
        .with(DRAG)
        .with(GRAVITY_SCALE);

    private ComponentType() {} // no instanciable
}
