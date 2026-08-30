package Game.Engine.Simulation;

/**
 * Máscara de bits compacta para representar qué componentes tiene una entidad.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * ComponentMask permite representar la presencia/ausencia de hasta 64
 * componentes diferentes usando un único long.
 *
 * Cada bit representa un tipo de componente:
 *   bit 0 → Position
 *   bit 1 → Velocity
 *   bit 2 → Acceleration
 *   bit 3 → Health
 *   bit 4 → Lifetime
 *   ...
 *
 * ── POR QUÉ BITMASK ──────────────────────────────────────────────────────
 *
 * Alternativas descartadas:
 *
 *   Set<ComponentType>     → allocations, indirección, cache misses
 *   boolean[] hasComponent → 64 bytes por entidad
 *   BitSet                 → overhead de objeto, no cache-friendly
 *
 * long bitmask:
 *   - 8 bytes por entidad
 *   - operaciones de bits extremadamente rápidas
 *   - puede copiarse/compararse trivialmente
 *   - perfecto para cache locality
 *
 * ── LÍMITE DE 64 COMPONENTES ─────────────────────────────────────────────
 *
 * Un long tiene 64 bits. Si algún día se necesitan más de 64 tipos de
 * componentes, migrar a long[] o BitSet.
 *
 * Para la arquitectura híbrida propuesta, 64 componentes son más que
 * suficientes:
 *   - Position, Velocity, Acceleration
 *   - Rotation, AngularVelocity
 *   - Health, MaxHealth
 *   - Lifetime, Age
 *   - CollisionBounds, CollisionMask
 *   - SpatialHash, RegionId
 *   - Mass, Drag, Gravity
 *   - TypeId, BehaviorId, OwnerId
 *   - Flags (invulnerable, frozen, etc.)
 *   ...
 *
 * ── IMMUTABILITY ─────────────────────────────────────────────────────────
 *
 * ComponentMask es inmutable. Modificar una máscara crea una nueva instancia.
 * Esto previene bugs sutiles donde una máscara compartida se modifica
 * accidentalmente.
 *
 * ── OPERACIONES ──────────────────────────────────────────────────────────
 *
 *   has(bit)       → ¿tiene este componente?
 *   with(bit)      → crea nueva máscara con componente añadido
 *   without(bit)   → crea nueva máscara con componente eliminado
 *   matches(mask)  → ¿tiene TODOS los componentes de mask?
 *   matchesAny(mask) → ¿tiene AL MENOS UNO de los componentes de mask?
 *
 * ── EJEMPLO ──────────────────────────────────────────────────────────────
 *
 *   // Definir IDs de componentes
 *   static final int POSITION = 0;
 *   static final int VELOCITY = 1;
 *   static final int HEALTH = 2;
 *
 *   // Crear máscara para una entidad con Position + Velocity
 *   ComponentMask mask = ComponentMask.EMPTY
 *       .with(POSITION)
 *       .with(VELOCITY);
 *
 *   // Validar componentes
 *   if (mask.has(POSITION)) {
 *       // acceder a positionsX[index], positionsY[index]
 *   }
 *
 *   // Sistema que requiere Position + Velocity
 *   ComponentMask movementRequirements = ComponentMask.EMPTY
 *       .with(POSITION)
 *       .with(VELOCITY);
 *
 *   if (entityMask.matches(movementRequirements)) {
 *       // Esta entidad puede procesarse por MovementSystem
 *   }
 */
public final class ComponentMask {

    /** Máscara vacía — ningún componente presente. */
    public static final ComponentMask EMPTY = new ComponentMask(0L);

    private final long bits;

    private ComponentMask(long bits) {
        this.bits = bits;
    }

    /**
     * Retorna true si el componente especificado está presente.
     *
     * @param componentId ID del componente (0-63)
     * @return true si el bit está activado
     * @throws IllegalArgumentException si componentId está fuera de rango
     */
    public boolean has(int componentId) {
        validateComponentId(componentId);
        return (bits & (1L << componentId)) != 0;
    }

    /**
     * Retorna una nueva máscara con el componente añadido.
     *
     * @param componentId ID del componente (0-63)
     * @return nueva ComponentMask con el bit activado
     * @throws IllegalArgumentException si componentId está fuera de rango
     */
    public ComponentMask with(int componentId) {
        validateComponentId(componentId);
        return new ComponentMask(bits | (1L << componentId));
    }

    /**
     * Retorna una nueva máscara con el componente eliminado.
     *
     * @param componentId ID del componente (0-63)
     * @return nueva ComponentMask con el bit desactivado
     * @throws IllegalArgumentException si componentId está fuera de rango
     */
    public ComponentMask without(int componentId) {
        validateComponentId(componentId);
        return new ComponentMask(bits & ~(1L << componentId));
    }

    /**
     * Retorna true si esta máscara contiene TODOS los componentes de other.
     *
     * Usado para validar que una entidad cumple los requisitos de un sistema:
     *
     *   ComponentMask requirements = EMPTY.with(POSITION).with(VELOCITY);
     *   if (entityMask.matches(requirements)) {
     *       // Entidad puede procesarse por MovementSystem
     *   }
     *
     * @param other máscara de componentes requeridos
     * @return true si todos los bits de other están presentes en this
     */
    public boolean matches(ComponentMask other) {
        return (bits & other.bits) == other.bits;
    }

    /**
     * Retorna true si esta máscara contiene AL MENOS UNO de los componentes de other.
     *
     * @param other máscara de componentes candidatos
     * @return true si al menos un bit de other está presente en this
     */
    public boolean matchesAny(ComponentMask other) {
        return (bits & other.bits) != 0;
    }

    /**
     * Retorna el número de componentes presentes.
     *
     * @return cantidad de bits activados
     */
    public int count() {
        return Long.bitCount(bits);
    }

    /**
     * Retorna true si no hay componentes presentes.
     */
    public boolean isEmpty() {
        return bits == 0;
    }

    /**
     * Retorna la representación raw de la máscara.
     * Expuesto para serialización o debugging — no usar para lógica de negocio.
     */
    public long raw() {
        return bits;
    }

    /**
     * Crea una ComponentMask desde una representación raw.
     * Usado para deserialización.
     */
    public static ComponentMask fromRaw(long bits) {
        return new ComponentMask(bits);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ComponentMask)) return false;
        ComponentMask other = (ComponentMask) obj;
        return bits == other.bits;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bits);
    }

    @Override
    public String toString() {
        if (bits == 0) return "ComponentMask[EMPTY]";
        return "ComponentMask[0b" + Long.toBinaryString(bits) + "]";
    }

    private static void validateComponentId(int componentId) {
        if (componentId < 0 || componentId >= 64) {
            throw new IllegalArgumentException(
                "componentId must be in range [0, 63], got: " + componentId
            );
        }
    }
}
