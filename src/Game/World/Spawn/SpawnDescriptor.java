package Game.World.Spawn;

/**
 * Descriptor inmutable de un spawn: qué, dónde, con qué condición.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * SpawnDescriptor agrupa los datos de configuración de un tipo de spawn.
 * Es el "plano" del spawn — no tiene estado propio de ejecución.
 *
 * ── DIFERENCIA CON SpawnRequest ───────────────────────────────────────────
 * SpawnDescriptor describe un tipo de spawn reutilizable (la plantilla).
 * SpawnRequest representa una solicitud concreta de ejecución (un uso).
 *
 * Ejemplo:
 *   SpawnDescriptor zombieSpawn = SpawnDescriptor.builder()
 *       .id("wave_zombie")
 *       .strategy(pos -> EnemyFactory.create(EnemyId.ZOMBIE, pos))
 *       .point(SpawnPoint.worldBounds(world.getWidth(), world.getHeight(), 60))
 *       .maxInstances(5)
 *       .build();
 *
 * ── CAMPOS ────────────────────────────────────────────────────────────────
 * id            → identificador único del descriptor (para SpawnRegistry)
 * strategy      → cómo construir el objeto
 * spawnPoint    → dónde puede aparecer
 * maxInstances  → cuántas instancias simultáneas permite (0 = sin límite)
 * cooldownTicks → tiempo mínimo entre spawns de este descriptor (0 = sin cooldown)
 */
public final class SpawnDescriptor {

    private final String        id;
    private final SpawnStrategy strategy;
    private final SpawnPoint    spawnPoint;
    private final int           maxInstances;
    private final int           cooldownTicks;

    private SpawnDescriptor(Builder b) {
        if (b.id       == null) throw new IllegalArgumentException("SpawnDescriptor.id is required");
        if (b.strategy == null) throw new IllegalArgumentException("SpawnDescriptor.strategy is required");
        this.id            = b.id;
        this.strategy      = b.strategy;
        this.spawnPoint    = b.spawnPoint;
        this.maxInstances  = b.maxInstances;
        this.cooldownTicks = b.cooldownTicks;
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String        getId()            { return id;            }
    public SpawnStrategy getStrategy()      { return strategy;      }
    public SpawnPoint    getSpawnPoint()    { return spawnPoint;    }
    public int           getMaxInstances()  { return maxInstances;  }
    public int           getCooldownTicks() { return cooldownTicks; }

    // ── Builder ───────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {

        private String        id;
        private SpawnStrategy strategy;
        private SpawnPoint    spawnPoint    = null;
        private int           maxInstances  = 0;   // 0 = sin límite
        private int           cooldownTicks = 0;   // 0 = sin cooldown

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder strategy(SpawnStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder point(SpawnPoint spawnPoint) {
            this.spawnPoint = spawnPoint;
            return this;
        }

        /** Máximo de instancias simultáneas de este tipo. 0 = sin límite. */
        public Builder maxInstances(int max) {
            this.maxInstances = max;
            return this;
        }

        /** Ticks mínimos entre spawns de este descriptor. 0 = sin cooldown. */
        public Builder cooldown(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        public SpawnDescriptor build() {
            return new SpawnDescriptor(this);
        }
    }
}
