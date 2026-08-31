package Game.Engine.Simulation.Storage;

import java.util.Arrays;

/**
 * Almacenamiento denso basado en Structure of Arrays (SoA) para componentes primitivos.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PrimitiveStorage almacena datos de simulación como arrays primitivos densos:
 *
 *   float[] positionsX    → coordenadas X de todas las entidades
 *   float[] positionsY    → coordenadas Y de todas las entidades
 *   float[] velocitiesX   → velocidades X de todas las entidades
 *   float[] velocitiesY   → velocidades Y de todas las entidades
 *   float[] health        → vida de todas las entidades
 *   float[] lifetimes     → tiempo de vida restante
 *   int[]   flags         → estado flags (bitfield)
 *
 * ── STRUCTURE OF ARRAYS (SoA) ────────────────────────────────────────────
 *
 * Contraste con Array of Structures (AoS):
 *
 *   AoS (OO tradicional):
 *     class Entity { float x, y, vx, vy, health; }
 *     Entity[] entities = new Entity[10000];
 *
 *   Acceso secuencial toca:
 *     entity[0]: x, y, vx, vy, health (campos intercalados)
 *     entity[1]: x, y, vx, vy, health
 *     ...
 *
 *   Cache line de 64 bytes solo contiene ~10 entidades completas.
 *   Si solo necesitas X, cargas Y, VX, VY, Health innecesariamente.
 *
 *   SoA (DOD):
 *     float[] x  = new float[10000];
 *     float[] y  = new float[10000];
 *     float[] vx = new float[10000];
 *
 *   Acceso secuencial a X:
 *     x[0], x[1], x[2], ... (secuencial, sin huecos)
 *
 *   Cache line de 64 bytes contiene 16 floats consecutivos.
 *   Solo cargas lo que necesitas. Predicción de hardware funciona perfectamente.
 *
 * ── BENEFICIOS ───────────────────────────────────────────────────────────
 *
 * 1. Cache locality — datos secuenciales, sin indirecciones
 * 2. Vectorización automática — compilador/JIT puede usar SIMD
 * 3. Sin allocations en hot path — arrays preallocados
 * 4. Prefetching predictible — hardware sabe qué cargar después
 * 5. Densidad — sin padding, sin object headers
 *
 * ── DISEÑO CAPACITY-BASED ────────────────────────────────────────────────
 *
 * Los arrays tienen una capacity fija. Cuando se llena, se hace grow:
 *
 *   capacityInicial = 256
 *   → lleno → grow a 512
 *   → lleno → grow a 1024
 *   → lleno → grow a 2048
 *
 * Grow es costoso (realloc + copy), pero infrecuente si se dimensiona bien.
 *
 * ── ACCESO DIRECTO ───────────────────────────────────────────────────────
 *
 * Los sistemas acceden directamente a los arrays:
 *
 *   PrimitiveStorage storage = entityStore.getStorage();
 *   float[] posX = storage.positionsX();
 *   float[] posY = storage.positionsY();
 *   float[] velX = storage.velocitiesX();
 *
 *   for (int i = 0; i < count; i++) {
 *       posX[i] += velX[i] * deltaTime;
 *       posY[i] += velY[i] * deltaTime;
 *   }
 *
 * Sin getters/setters. Sin validaciones redundantes. Loop puro.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 *
 * PrimitiveStorage NO es thread-safe.
 *
 * Responsabilidad de sincronización es del SimulationPipeline que
 * coordina los sistemas. Para este HRFC, todo es single-threaded.
 *
 * Extensiones futuras pueden añadir paralelización si los sistemas
 * son data-independent.
 *
 * ── RESIZE Y COMPACTACIÓN ────────────────────────────────────────────────
 *
 * resize(newCapacity) — aumenta la capacidad, preservando datos existentes
 * swap(indexA, indexB) — intercambia dos slots (usado durante compactación)
 *
 * La compactación es responsabilidad de EntityStore, no de PrimitiveStorage.
 */
public final class PrimitiveStorage {

    private static final int INITIAL_CAPACITY = 256;
    private static final float GROW_FACTOR = 1.5f;

    private int capacity;

    // ── Position (float x, float y) ──────────────────────────────────────
    private float[] positionsX;
    private float[] positionsY;

    // ── Velocity (float vx, float vy) ────────────────────────────────────
    private float[] velocitiesX;
    private float[] velocitiesY;

    // ── Acceleration (float ax, float ay) ────────────────────────────────
    private float[] accelerationsX;
    private float[] accelerationsY;

    // ── Lifetime ──────────────────────────────────────────────────────────
    private float[] lifetimes; // segundos restantes

    // ── Rotation ──────────────────────────────────────────────────────────
    // Visual rotation en radianes (para bullets giratorias, drills, spinning saws)
    // NO confundir con dirección de velocity
    private float[] rotations;
    private float[] angularVelocities; // rad/s — RotationSystem integra esto en rotations

    // ── Physics ───────────────────────────────────────────────────────────
    private float[] mass;
    private float[] drag;
    private float[] gravityScale;

    // ── Metadata ──────────────────────────────────────────────────────────
    // EntityId del owner (quien disparó esta bullet) — FUNDAMENTAL para:
    // - Faction checks (friendly fire)
    // - Scoring/attribution
    // - Achievement tracking
    // - Damage stats
    private long[]  ownerEntityIds;

    // ── State ─────────────────────────────────────────────────────────────
    private int[]   flags;  // bitfield de estado
    private float[] damage; // daño que inflige

    /**
     * Constructor con capacidad inicial por defecto.
     */
    public PrimitiveStorage() {
        this(INITIAL_CAPACITY);
    }

    /**
     * Constructor con capacidad inicial especificada.
     *
     * @param initialCapacity capacidad inicial de los arrays
     */
    public PrimitiveStorage(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be > 0");
        }
        this.capacity = initialCapacity;
        allocateArrays(capacity);
    }

    /**
     * Retorna la capacidad actual de los arrays.
     */
    public int capacity() {
        return capacity;
    }

    // ── Accessors para Position ───────────────────────────────────────────

    public float[] positionsX()  { return positionsX; }
    public float[] positionsY()  { return positionsY; }

    // ── Accessors para Velocity ───────────────────────────────────────────

    public float[] velocitiesX() { return velocitiesX; }
    public float[] velocitiesY() { return velocitiesY; }

    // ── Accessors para Acceleration ───────────────────────────────────────

    public float[] accelerationsX() { return accelerationsX; }
    public float[] accelerationsY() { return accelerationsY; }

    // ── Accessors para Lifetime ───────────────────────────────────────────

    public float[] lifetimes() { return lifetimes; }

    // ── Accessors para Rotation ───────────────────────────────────────────

    public float[] rotations() { return rotations; }
    public float[] angularVelocities() { return angularVelocities; }

    // ── Accessors para Physics ────────────────────────────────────────────

    public float[] mass()         { return mass; }
    public float[] drag()         { return drag; }
    public float[] gravityScale() { return gravityScale; }

    // ── Accessors para Metadata ───────────────────────────────────────────

    public long[] ownerEntityIds() { return ownerEntityIds; }

    // ── Accessors para State ──────────────────────────────────────────────

    public int[]   flags()  { return flags; }
    public float[] damage() { return damage; }

    // ── Resize ────────────────────────────────────────────────────────────

    /**
     * Aumenta la capacidad de los arrays.
     * Los datos existentes se preservan. Las nuevas posiciones se inicializan a cero.
     *
     * @param newCapacity nueva capacidad (debe ser > capacity actual)
     * @throws IllegalArgumentException si newCapacity <= capacity actual
     */
    public void resize(int newCapacity) {
        if (newCapacity <= capacity) {
            throw new IllegalArgumentException(
                "newCapacity (" + newCapacity + ") must be > current capacity (" + capacity + ")"
            );
        }

        // Position
        positionsX = Arrays.copyOf(positionsX, newCapacity);
        positionsY = Arrays.copyOf(positionsY, newCapacity);

        // Velocity
        velocitiesX = Arrays.copyOf(velocitiesX, newCapacity);
        velocitiesY = Arrays.copyOf(velocitiesY, newCapacity);

        // Acceleration
        accelerationsX = Arrays.copyOf(accelerationsX, newCapacity);
        accelerationsY = Arrays.copyOf(accelerationsY, newCapacity);

        // Lifetime
        lifetimes = Arrays.copyOf(lifetimes, newCapacity);

        // Rotation
        rotations = Arrays.copyOf(rotations, newCapacity);
        angularVelocities = Arrays.copyOf(angularVelocities, newCapacity);

        // Physics
        mass = Arrays.copyOf(mass, newCapacity);
        drag = Arrays.copyOf(drag, newCapacity);
        gravityScale = Arrays.copyOf(gravityScale, newCapacity);

        // Metadata
        ownerEntityIds = Arrays.copyOf(ownerEntityIds, newCapacity);

        // State
        flags = Arrays.copyOf(flags, newCapacity);
        damage = Arrays.copyOf(damage, newCapacity);

        capacity = newCapacity;
    }

    /**
     * Sugiere una nueva capacidad aplicando el factor de crecimiento.
     * Usado internamente cuando el storage se llena.
     *
     * @return nueva capacidad sugerida
     */
    public int suggestGrowCapacity() {
        return Math.max(capacity + 1, (int) (capacity * GROW_FACTOR));
    }

    /**
     * Intercambia los datos de dos índices.
     * Usado durante compactación para mover la última entidad a un hueco.
     *
     * @param indexA primer índice
     * @param indexB segundo índice
     */
    public void swap(int indexA, int indexB) {
        if (indexA == indexB) return;

        // Position
        swapFloat(positionsX, indexA, indexB);
        swapFloat(positionsY, indexA, indexB);

        // Velocity
        swapFloat(velocitiesX, indexA, indexB);
        swapFloat(velocitiesY, indexA, indexB);

        // Acceleration
        swapFloat(accelerationsX, indexA, indexB);
        swapFloat(accelerationsY, indexA, indexB);

        // Lifetime
        swapFloat(lifetimes, indexA, indexB);

        // Rotation
        swapFloat(rotations, indexA, indexB);
        swapFloat(angularVelocities, indexA, indexB);

        // Physics
        swapFloat(mass, indexA, indexB);
        swapFloat(drag, indexA, indexB);
        swapFloat(gravityScale, indexA, indexB);

        // Metadata
        swapLong(ownerEntityIds, indexA, indexB);

        // State
        swapInt(flags, indexA, indexB);
        swapFloat(damage, indexA, indexB);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void allocateArrays(int size) {
        positionsX = new float[size];
        positionsY = new float[size];
        velocitiesX = new float[size];
        velocitiesY = new float[size];
        accelerationsX = new float[size];
        accelerationsY = new float[size];
        lifetimes = new float[size];
        rotations = new float[size];
        angularVelocities = new float[size];
        mass = new float[size];
        drag = new float[size];
        gravityScale = new float[size];
        ownerEntityIds = new long[size];
        flags = new int[size];
        damage = new float[size];
    }

    private static void swapFloat(float[] array, int i, int j) {
        float temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private static void swapInt(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private static void swapLong(long[] array, int i, int j) {
        long temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
}
