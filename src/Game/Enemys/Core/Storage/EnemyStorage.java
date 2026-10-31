package Game.Enemys.Core.Storage;

/**
 * Storage DOD para datos de enemies.
 *
 * ── HRFC — Enemy DOD Migration ────────────────────────────────────────────
 *
 * Similar a PrimitiveStorage de bullets, pero con datos específicos de enemies:
 *
 * DATOS HOT (accedidos cada frame por sistemas batch):
 *   - positionsX, positionsY      — posición en el mundo
 *   - velocitiesX, velocitiesY    — velocidad actual
 *   - accelerationsX, accelerationsY — aceleración acumulada
 *   - health, maxHealth           — vida actual y máxima
 *   - flags                       — estados booleanos (isAlive, canMove, etc.)
 *   - targetX, targetY            — objetivo de movimiento (para AI)
 *
 * DATOS WARM (accedidos ocasionalmente):
 *   - damage                      — daño base de contacto
 *   - speed                       — velocidad de movimiento base
 *   - detectionRadius             — radio de detección del player
 *   - attackCooldown              — cooldown entre ataques
 *
 * Arrays SoA (Structure of Arrays) para máxima cache locality.
 * Todos los arrays tienen la misma capacidad y se indexan con el mismo índice denso.
 */
public final class EnemyStorage {

    private int capacity;

    // ── Position & Movement (HOT) ──────────────────────────────────────────
    private float[] positionsX;
    private float[] positionsY;
    private float[] velocitiesX;
    private float[] velocitiesY;
    private float[] accelerationsX;
    private float[] accelerationsY;

    // ── Health (HOT) ───────────────────────────────────────────────────────
    private float[] health;
    private float[] maxHealth;

    // ── Flags (HOT) ────────────────────────────────────────────────────────
    // Bit flags: 0=isAlive, 1=canMove, 2=canAttack, 3=isAggro
    private int[] flags;

    // ── AI Target (HOT) ────────────────────────────────────────────────────
    private float[] targetX;
    private float[] targetY;

    // ── Stats (WARM) ───────────────────────────────────────────────────────
    private float[] damage;
    private float[] speed;
    private float[] detectionRadius;
    private float[] attackCooldown;
    private float[] attackTimer;

    public EnemyStorage(int initialCapacity) {
        this.capacity = initialCapacity;
        allocate();
    }

    private void allocate() {
        // Position & Movement
        positionsX = new float[capacity];
        positionsY = new float[capacity];
        velocitiesX = new float[capacity];
        velocitiesY = new float[capacity];
        accelerationsX = new float[capacity];
        accelerationsY = new float[capacity];

        // Health
        health = new float[capacity];
        maxHealth = new float[capacity];

        // Flags
        flags = new int[capacity];

        // AI Target
        targetX = new float[capacity];
        targetY = new float[capacity];

        // Stats
        damage = new float[capacity];
        speed = new float[capacity];
        detectionRadius = new float[capacity];
        attackCooldown = new float[capacity];
        attackTimer = new float[capacity];
    }

    public void resize(int newCapacity) {
        if (newCapacity <= capacity) return;

        int oldCapacity = capacity;
        capacity = newCapacity;

        // Resize all arrays preserving data
        positionsX = resize(positionsX, newCapacity);
        positionsY = resize(positionsY, newCapacity);
        velocitiesX = resize(velocitiesX, newCapacity);
        velocitiesY = resize(velocitiesY, newCapacity);
        accelerationsX = resize(accelerationsX, newCapacity);
        accelerationsY = resize(accelerationsY, newCapacity);

        health = resize(health, newCapacity);
        maxHealth = resize(maxHealth, newCapacity);

        flags = resize(flags, newCapacity);

        targetX = resize(targetX, newCapacity);
        targetY = resize(targetY, newCapacity);

        damage = resize(damage, newCapacity);
        speed = resize(speed, newCapacity);
        detectionRadius = resize(detectionRadius, newCapacity);
        attackCooldown = resize(attackCooldown, newCapacity);
        attackTimer = resize(attackTimer, newCapacity);
    }

    private float[] resize(float[] old, int newSize) {
        float[] fresh = new float[newSize];
        System.arraycopy(old, 0, fresh, 0, Math.min(old.length, newSize));
        return fresh;
    }

    private int[] resize(int[] old, int newSize) {
        int[] fresh = new int[newSize];
        System.arraycopy(old, 0, fresh, 0, Math.min(old.length, newSize));
        return fresh;
    }

    public void swap(int indexA, int indexB) {
        // Position & Movement
        swap(positionsX, indexA, indexB);
        swap(positionsY, indexA, indexB);
        swap(velocitiesX, indexA, indexB);
        swap(velocitiesY, indexA, indexB);
        swap(accelerationsX, indexA, indexB);
        swap(accelerationsY, indexA, indexB);

        // Health
        swap(health, indexA, indexB);
        swap(maxHealth, indexA, indexB);

        // Flags
        swapInt(flags, indexA, indexB);

        // AI Target
        swap(targetX, indexA, indexB);
        swap(targetY, indexA, indexB);

        // Stats
        swap(damage, indexA, indexB);
        swap(speed, indexA, indexB);
        swap(detectionRadius, indexA, indexB);
        swap(attackCooldown, indexA, indexB);
        swap(attackTimer, indexA, indexB);
    }

    private void swap(float[] arr, int a, int b) {
        float temp = arr[a];
        arr[a] = arr[b];
        arr[b] = temp;
    }

    private void swapInt(int[] arr, int a, int b) {
        int temp = arr[a];
        arr[a] = arr[b];
        arr[b] = temp;
    }

    public int suggestGrowCapacity() {
        return capacity + (capacity >> 1); // grow by 50%
    }

    // ── Accessors ──────────────────────────────────────────────────────────

    public int capacity() { return capacity; }

    public float[] positionsX() { return positionsX; }
    public float[] positionsY() { return positionsY; }
    public float[] velocitiesX() { return velocitiesX; }
    public float[] velocitiesY() { return velocitiesY; }
    public float[] accelerationsX() { return accelerationsX; }
    public float[] accelerationsY() { return accelerationsY; }

    public float[] health() { return health; }
    public float[] maxHealth() { return maxHealth; }

    public int[] flags() { return flags; }

    public float[] targetX() { return targetX; }
    public float[] targetY() { return targetY; }

    public float[] damage() { return damage; }
    public float[] speed() { return speed; }
    public float[] detectionRadius() { return detectionRadius; }
    public float[] attackCooldown() { return attackCooldown; }
    public float[] attackTimer() { return attackTimer; }
}
