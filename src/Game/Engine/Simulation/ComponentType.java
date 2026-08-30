package Game.Engine.Simulation;

/**
 * Tipos de componentes de simulación soportados por la infraestructura DOD.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * ComponentType enumera todos los tipos de datos de simulación que pueden
 * almacenarse en la infraestructura DOD del Engine.
 *
 * Cada tipo tiene:
 *   - Un ID único (para ComponentMask)
 *   - Un nombre descriptivo
 *   - Clasificación hot/warm/cold
 *
 * ── CLASIFICACIÓN DE DATOS ───────────────────────────────────────────────
 *
 * HOT — datos accedidos constantemente por frame:
 *   Position, Velocity, Acceleration, Health, Lifetime, Flags
 *
 * WARM — datos accedidos frecuentemente pero no en cada operación:
 *   Rotation, AngularVelocity, CollisionBounds, Mass, TypeId, BehaviorId
 *
 * COLD — datos estáticos o de baja frecuencia:
 *   (Los datos cold normalmente NO van al DOD storage — permanecen en dominio)
 *
 * Esta clasificación es una guía para optimización futura. La implementación
 * actual almacena todos los componentes en el mismo nivel.
 *
 * ── EXTENSIBILIDAD ───────────────────────────────────────────────────────
 *
 * Para añadir un nuevo tipo de componente:
 *
 * 1. Añadir la entrada en este enum
 * 2. Crear el almacenamiento correspondiente en PrimitiveStorage
 * 3. Implementar los sistemas que lo procesen
 *
 * NO es necesario modificar EntityStore ni SimulationHandle.
 *
 * ── LÍMITE DE 64 COMPONENTES ─────────────────────────────────────────────
 *
 * ComponentMask usa un long (64 bits). Si se necesitan más de 64 tipos,
 * migrar a una representación extendida (long[] o BitSet).
 *
 * Para la arquitectura híbrida propuesta, 64 componentes son suficientes
 * porque solo los datos HOT de simulación van al DOD. Los datos de dominio
 * permanecen en las instancias OO.
 *
 * ── ORDEN DE DEFINICIÓN ──────────────────────────────────────────────────
 *
 * El orden en este enum determina el ID del componente.
 * NO reordenar componentes existentes — eso rompería compatibilidad
 * con savegames si se implementa serialización en el futuro.
 *
 * Nuevos componentes deben añadirse AL FINAL.
 */
public enum ComponentType {

    // ── COMPONENTES HOT (acceso constante por frame) ─────────────────────

    /** Posición 2D (x, y) en píxeles. */
    POSITION(DataTemperature.HOT),

    /** Velocidad 2D (vx, vy) en píxeles/segundo. */
    VELOCITY(DataTemperature.HOT),

    /** Aceleración 2D (ax, ay) en píxeles/segundo². */
    ACCELERATION(DataTemperature.HOT),

    /** Vida actual de la entidad. */
    HEALTH(DataTemperature.HOT),

    /** Tiempo de vida restante en segundos (para proyectiles, efectos). */
    LIFETIME(DataTemperature.HOT),

    /** Flags de estado (bitfield de 32 bits). */
    FLAGS(DataTemperature.HOT),

    // ── COMPONENTES WARM (acceso frecuente pero no constante) ────────────

    /** Rotación en radianes. */
    ROTATION(DataTemperature.WARM),

    /** Velocidad angular en radianes/segundo. */
    ANGULAR_VELOCITY(DataTemperature.WARM),

    /** Límites de colisión (bounds: minX, minY, maxX, maxY). */
    COLLISION_BOUNDS(DataTemperature.WARM),

    /** Máscara de colisión (bitfield que determina con qué puede colisionar). */
    COLLISION_MASK(DataTemperature.WARM),

    /** Masa física en kg (para física basada en fuerzas). */
    MASS(DataTemperature.WARM),

    /** Coeficiente de arrastre (air drag). */
    DRAG(DataTemperature.WARM),

    /** Multiplicador de gravedad (1.0 = gravedad normal, 0.0 = sin gravedad). */
    GRAVITY_SCALE(DataTemperature.WARM),

    /** ID de tipo de entidad (para dispatch de comportamiento). */
    TYPE_ID(DataTemperature.WARM),

    /** ID de comportamiento activo (para dispatch de lógica). */
    BEHAVIOR_ID(DataTemperature.WARM),

    /** ID del owner/creador de esta entidad (para atribución de daño). */
    OWNER_ID(DataTemperature.WARM),

    /** Hash espacial para optimización de queries espaciales. */
    SPATIAL_HASH(DataTemperature.WARM),

    /** ID de región activa (para ActiveRegion system). */
    REGION_ID(DataTemperature.WARM),

    // ── COMPONENTES ADICIONALES (reserva para futuros sistemas) ──────────

    /** Edad de la entidad en segundos desde su creación. */
    AGE(DataTemperature.WARM),

    /** Daño que inflige esta entidad al colisionar. */
    DAMAGE(DataTemperature.WARM),

    /** Vida máxima de la entidad. */
    MAX_HEALTH(DataTemperature.WARM);

    // ── Metadata ─────────────────────────────────────────────────────────

    private final DataTemperature temperature;

    ComponentType(DataTemperature temperature) {
        this.temperature = temperature;
    }

    /**
     * Retorna el ID único de este componente (0-63).
     * El ID es el ordinal del enum y es estable mientras no se reordene.
     */
    public int id() {
        return ordinal();
    }

    /**
     * Retorna la clasificación de temperatura de este componente.
     */
    public DataTemperature temperature() {
        return temperature;
    }

    /**
     * Retorna true si este es un componente HOT (acceso constante).
     */
    public boolean isHot() {
        return temperature == DataTemperature.HOT;
    }

    /**
     * Retorna true si este es un componente WARM (acceso frecuente).
     */
    public boolean isWarm() {
        return temperature == DataTemperature.WARM;
    }

    /**
     * Clasificación de temperatura de datos — determina estrategia de cache.
     */
    public enum DataTemperature {
        /** Acceso constante cada frame — prioridad máxima de cache locality. */
        HOT,

        /** Acceso frecuente pero no cada frame — segunda prioridad. */
        WARM,

        /** Acceso infrecuente — puede vivir fuera del DOD storage. */
        COLD
    }
}
