package Game.Engine.Simulation.Systems;

import Game.Engine.Simulation.Storage.EntityStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pipeline determinista que ejecuta sistemas de simulación en orden.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * SimulationPipeline coordina la ejecución de todos los sistemas de simulación
 * en un orden determinista y explícito.
 *
 * Cada frame:
 *   1. Recibe deltaTime del GameLoop
 *   2. Ejecuta cada sistema registrado en orden
 *   3. (Opcionalmente) compacta el EntityStore
 *   4. (Opcionalmente) recolecta métricas de profiling
 *
 * ── DETERMINISMO ─────────────────────────────────────────────────────────
 *
 * El orden de ejecución está determinado por el orden de registro.
 * Los sistemas se ejecutan secuencialmente, uno tras otro.
 *
 * Ejemplo de orden típico:
 *
 *   1. AccelerationSystem   → velocity += acceleration * dt
 *   2. MovementSystem       → position += velocity * dt
 *   3. LifetimeSystem       → lifetime -= dt, marcar muertos
 *   4. PhysicsSystem        → aplicar gravedad, drag, etc.
 *   5. CollisionSystem      → detectar y resolver colisiones
 *   6. SpatialSystem        → actualizar spatial hash
 *
 * Este orden es crítico. Por ejemplo:
 *   - Acceleration debe ejecutarse ANTES de Movement
 *   - Movement debe ejecutarse ANTES de Collision
 *   - Lifetime debe ejecutarse para marcar entidades muertas
 *
 * ── REGISTRO DE SISTEMAS ─────────────────────────────────────────────────
 *
 * Los sistemas se registran mediante register():
 *
 *   SimulationPipeline pipeline = new SimulationPipeline(entityStore);
 *   pipeline.register(new AccelerationSystem());
 *   pipeline.register(new MovementSystem());
 *   pipeline.register(new LifetimeSystem());
 *
 * El orden de llamadas a register() determina el orden de ejecución.
 *
 * ── COMPACTACIÓN ─────────────────────────────────────────────────────────
 *
 * El pipeline puede configurarse para compactar automáticamente el
 * EntityStore después de ejecutar todos los sistemas:
 *
 *   pipeline.setAutoCompact(true);
 *
 * Esto elimina entidades muertas y mantiene el storage denso.
 *
 * Alternativamente, el caller puede compactar manualmente:
 *
 *   pipeline.update(deltaTime);
 *   entityStore.compact();
 *
 * ── PROFILING ────────────────────────────────────────────────────────────
 *
 * El pipeline puede recolectar métricas de tiempo por sistema:
 *
 *   pipeline.setProfilingEnabled(true);
 *   pipeline.update(deltaTime);
 *   ProfilingStats stats = pipeline.getLastStats();
 *   System.out.println(stats.format());
 *
 * Esto permite identificar sistemas lentos o bottlenecks.
 *
 * ── THREAD SAFETY ────────────────────────────────────────────────────────
 *
 * SimulationPipeline NO es thread-safe.
 * Debe ejecutarse desde un único thread (normalmente el GameLoop thread).
 *
 * Extensiones futuras pueden añadir paralelización si los sistemas
 * son data-independent (ej: spatial queries, particle systems).
 *
 * ── EXTENSIBILIDAD ───────────────────────────────────────────────────────
 *
 * Los dominios pueden registrar sus propios sistemas:
 *
 *   // Game.Bullets registra sus sistemas
 *   pipeline.register(new ProjectileMovementSystem());
 *   pipeline.register(new ProjectileBehaviorSystem());
 *
 *   // Game.Enemies registra los suyos
 *   pipeline.register(new EnemyAISystem());
 *   pipeline.register(new EnemyMovementSystem());
 *
 * El pipeline no necesita conocer los dominios — solo ejecuta
 * sistemas en orden.
 *
 * ── EJEMPLO DE USO ───────────────────────────────────────────────────────
 *
 *   // Setup (una vez al inicio)
 *   EntityStore store = new EntityStore();
 *   SimulationPipeline pipeline = new SimulationPipeline(store);
 *   pipeline.register(new AccelerationSystem());
 *   pipeline.register(new MovementSystem());
 *   pipeline.register(new LifetimeSystem());
 *   pipeline.setAutoCompact(true);
 *
 *   // Game loop
 *   while (running) {
 *       double deltaTime = timer.getDeltaTime();
 *       pipeline.update(deltaTime);
 *       render();
 *   }
 */
public final class SimulationPipeline {

    private final EntityStore entityStore;
    private final List<SimulationSystem> systems;
    private boolean autoCompact;
    private boolean profilingEnabled;
    private ProfilingStats lastStats;

    /**
     * Constructor.
     *
     * @param entityStore EntityStore que contiene los datos de simulación
     */
    public SimulationPipeline(EntityStore entityStore) {
        if (entityStore == null) {
            throw new IllegalArgumentException("entityStore cannot be null");
        }
        this.entityStore = entityStore;
        this.systems = new ArrayList<>();
        this.autoCompact = false;
        this.profilingEnabled = false;
    }

    /**
     * Registra un sistema de simulación.
     * El orden de registro determina el orden de ejecución.
     *
     * @param system sistema a registrar
     */
    public void register(SimulationSystem system) {
        if (system == null) {
            throw new IllegalArgumentException("system cannot be null");
        }
        systems.add(system);
    }

    /**
     * Retorna vista inmutable de los sistemas registrados.
     *
     * @return lista de sistemas en orden de ejecución
     */
    public List<SimulationSystem> getSystems() {
        return Collections.unmodifiableList(systems);
    }

    /**
     * Ejecuta todos los sistemas registrados en orden.
     *
     * @param deltaTime tiempo del simulation step en segundos
     */
    public void update(double deltaTime) {
        if (profilingEnabled) {
            updateWithProfiling(deltaTime);
        } else {
            updateNormal(deltaTime);
        }

        if (autoCompact) {
            entityStore.compact();
        }
    }

    /**
     * Actualización normal sin profiling.
     */
    private void updateNormal(double deltaTime) {
        for (SimulationSystem system : systems) {
            system.update(entityStore, deltaTime);
        }
    }

    /**
     * Actualización con profiling de tiempos.
     */
    private void updateWithProfiling(double deltaTime) {
        ProfilingStats stats = new ProfilingStats(systems.size());

        for (int i = 0; i < systems.size(); i++) {
            SimulationSystem system = systems.get(i);
            
            long startNanos = System.nanoTime();
            system.update(entityStore, deltaTime);
            long endNanos = System.nanoTime();

            long durationNanos = endNanos - startNanos;
            stats.recordSystem(i, system.name(), durationNanos);
        }

        lastStats = stats;
    }

    /**
     * Habilita o deshabilita compactación automática después de cada update.
     *
     * @param enabled true para habilitar, false para deshabilitar
     */
    public void setAutoCompact(boolean enabled) {
        this.autoCompact = enabled;
    }

    /**
     * Retorna si la compactación automática está habilitada.
     */
    public boolean isAutoCompactEnabled() {
        return autoCompact;
    }

    /**
     * Habilita o deshabilita profiling de sistemas.
     *
     * @param enabled true para habilitar, false para deshabilitar
     */
    public void setProfilingEnabled(boolean enabled) {
        this.profilingEnabled = enabled;
    }

    /**
     * Retorna si el profiling está habilitado.
     */
    public boolean isProfilingEnabled() {
        return profilingEnabled;
    }

    /**
     * Retorna las estadísticas de profiling del último update.
     * Solo válido si profilingEnabled == true.
     *
     * @return ProfilingStats del último update, o null si profiling deshabilitado
     */
    public ProfilingStats getLastStats() {
        return lastStats;
    }

    /**
     * Retorna el EntityStore asociado a este pipeline.
     */
    public EntityStore getEntityStore() {
        return entityStore;
    }

    /**
     * Retorna el número de sistemas registrados.
     */
    public int getSystemCount() {
        return systems.size();
    }

    // ── ProfilingStats ────────────────────────────────────────────────────

    /**
     * Estadísticas de profiling por sistema.
     */
    public static final class ProfilingStats {
        private final String[] systemNames;
        private final long[] durationNanos;
        private int count;

        ProfilingStats(int capacity) {
            this.systemNames = new String[capacity];
            this.durationNanos = new long[capacity];
            this.count = 0;
        }

        void recordSystem(int index, String name, long durationNanos) {
            systemNames[index] = name;
            this.durationNanos[index] = durationNanos;
            count = Math.max(count, index + 1);
        }

        /**
         * Retorna el número de sistemas registrados.
         */
        public int count() {
            return count;
        }

        /**
         * Retorna el nombre del sistema en el índice especificado.
         */
        public String getSystemName(int index) {
            if (index < 0 || index >= count) return null;
            return systemNames[index];
        }

        /**
         * Retorna la duración en nanosegundos del sistema en el índice especificado.
         */
        public long getDurationNanos(int index) {
            if (index < 0 || index >= count) return 0;
            return durationNanos[index];
        }

        /**
         * Retorna la duración en milisegundos del sistema en el índice especificado.
         */
        public double getDurationMillis(int index) {
            return getDurationNanos(index) / 1_000_000.0;
        }

        /**
         * Retorna la duración total de todos los sistemas en nanosegundos.
         */
        public long getTotalDurationNanos() {
            long total = 0;
            for (int i = 0; i < count; i++) {
                total += durationNanos[i];
            }
            return total;
        }

        /**
         * Retorna la duración total de todos los sistemas en milisegundos.
         */
        public double getTotalDurationMillis() {
            return getTotalDurationNanos() / 1_000_000.0;
        }

        /**
         * Formatea las estadísticas como texto legible.
         */
        public String format() {
            if (count == 0) return "No systems profiled";

            StringBuilder sb = new StringBuilder();
            sb.append("SimulationPipeline Profiling:\n");
            sb.append("═══════════════════════════════════════════════\n");

            double totalMs = getTotalDurationMillis();

            for (int i = 0; i < count; i++) {
                String name = systemNames[i];
                double ms = getDurationMillis(i);
                double percent = (ms / totalMs) * 100.0;

                sb.append(String.format("  %-30s %8.3f ms  (%5.1f%%)\n",
                    name, ms, percent));
            }

            sb.append("───────────────────────────────────────────────\n");
            sb.append(String.format("  %-30s %8.3f ms\n", "TOTAL", totalMs));

            return sb.toString();
        }

        @Override
        public String toString() {
            return format();
        }
    }
}
