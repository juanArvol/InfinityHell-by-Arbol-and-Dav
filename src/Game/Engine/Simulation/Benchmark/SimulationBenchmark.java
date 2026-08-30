package Game.Engine.Simulation.Benchmark;

import Game.Engine.Simulation.ComponentMask;
import Game.Engine.Simulation.ComponentType;
import Game.Engine.Simulation.EntityId;
import Game.Engine.Simulation.SimulationHandle;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.Engine.Simulation.Storage.PrimitiveStorage;
import Game.Engine.Simulation.Systems.AccelerationSystem;
import Game.Engine.Simulation.Systems.LifetimeSystem;
import Game.Engine.Simulation.Systems.MovementSystem;
import Game.Engine.Simulation.Systems.SimulationPipeline;

/**
 * Benchmark para comparar rendimiento OO vs DOD en simulación de entidades.
 *
 * ── HRFC — Game.Engine Unified Simulation Data Architecture / ECS-DOD ─────
 *
 * ── OBJETIVO ──────────────────────────────────────────────────────────────
 *
 * Este benchmark mide el rendimiento de la nueva arquitectura DOD comparada
 * con un modelo OO tradicional en operaciones de simulación típicas:
 *
 *   - Integración de velocidad en posición
 *   - Integración de aceleración en velocidad
 *   - Decremento de lifetime
 *
 * ── MÉTRICAS ──────────────────────────────────────────────────────────────
 *
 * Para cada configuración (1K, 3.6K, 10K, 50K entidades) mide:
 *
 *   1. Tiempo de simulación por frame (nanosegundos)
 *   2. Throughput (entidades procesadas por segundo)
 *   3. Tiempo por entidad (nanosegundos/entidad)
 *   4. Tiempo de creación/inicialización
 *   5. Memoria aproximada utilizada
 *
 * ── CONFIGURACIONES DE PRUEBA ────────────────────────────────────────────
 *
 *   - 1,000 entidades    (carga baja, típica de escenas simples)
 *   - 3,600 entidades    (objetivo HRFC original para bullets)
 *   - 10,000 entidades   (carga alta, escenas complejas)
 *   - 50,000 entidades   (stress test, bullet hell extremo)
 *
 * ── METODOLOGÍA ──────────────────────────────────────────────────────────
 *
 * Para cada configuración:
 *
 *   1. Warmup: 100 iteraciones (permitir JIT optimization)
 *   2. Medición: 1000 iteraciones
 *   3. Calcular estadísticas: promedio, mínimo, máximo, desviación estándar
 *   4. Reportar resultados formateados
 *
 * ── MODELO OO (BASELINE) ─────────────────────────────────────────────────
 *
 * Clase tradicional con todos los datos encapsulados:
 *
 *   class SyntheticEntity {
 *       float posX, posY;
 *       float velX, velY;
 *       float accX, accY;
 *       float lifetime;
 *
 *       void update(double dt) {
 *           velX += accX * dt;
 *           velY += accY * dt;
 *           posX += velX * dt;
 *           posY += velY * dt;
 *           lifetime -= dt;
 *       }
 *   }
 *
 *   SyntheticEntity[] entities = new SyntheticEntity[N];
 *   for (SyntheticEntity e : entities) {
 *       e.update(deltaTime);
 *   }
 *
 * ── MODELO DOD (NUEVO) ───────────────────────────────────────────────────
 *
 * Arrays primitivos SoA con sistemas batch:
 *
 *   EntityStore store = new EntityStore();
 *   SimulationPipeline pipeline = new SimulationPipeline(store);
 *   pipeline.register(new AccelerationSystem());
 *   pipeline.register(new MovementSystem());
 *   pipeline.register(new LifetimeSystem());
 *
 *   pipeline.update(deltaTime);
 *
 * ── INTERPRETACIÓN DE RESULTADOS ─────────────────────────────────────────
 *
 * Esperado:
 *   - DOD debería ser 2-5x más rápido que OO para cargas grandes (10K+)
 *   - DOD puede ser similar o peor que OO para cargas pequeñas (<1K)
 *     debido al overhead de indirección (EntityRecord lookup)
 *   - La ventaja de DOD crece con el número de entidades
 *
 * Si DOD es más lento que OO para todas las cargas, revisar:
 *   - EntityRecord lookup (puede ser bottleneck)
 *   - ComponentMask validation (overhead por entidad)
 *   - Diseño de arrays (alignment, padding)
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Ejecutar benchmark completo
 *   SimulationBenchmark.main(new String[0]);
 *
 *   // O ejecutar configuración específica
 *   BenchmarkResult result = SimulationBenchmark.runDODBenchmark(10000, 1000);
 *   System.out.println(result.format());
 */
public final class SimulationBenchmark {

    private static final int[] ENTITY_COUNTS = {1_000, 3_600, 10_000, 50_000};
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;
    private static final double DELTA_TIME = 1.0 / 60.0; // 60 FPS

    // ── Main Entry Point ──────────────────────────────────────────────────

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  SIMULATION BENCHMARK — OO vs DOD");
        System.out.println("  HRFC — Game.Engine Unified Simulation Data Architecture");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();

        for (int entityCount : ENTITY_COUNTS) {
            System.out.println("─────────────────────────────────────────────────────────────");
            System.out.println("  ENTITY COUNT: " + formatNumber(entityCount));
            System.out.println("─────────────────────────────────────────────────────────────");
            System.out.println();

            // Benchmark OO
            System.out.println("  Running OO Baseline...");
            BenchmarkResult ooResult = runOOBenchmark(entityCount, MEASUREMENT_ITERATIONS);

            // Benchmark DOD
            System.out.println("  Running DOD Pipeline...");
            BenchmarkResult dodResult = runDODBenchmark(entityCount, MEASUREMENT_ITERATIONS);

            // Comparar
            System.out.println();
            System.out.println("  RESULTS:");
            System.out.println();
            System.out.println("  OO Baseline:");
            System.out.println(ooResult.formatIndented());
            System.out.println();
            System.out.println("  DOD Pipeline:");
            System.out.println(dodResult.formatIndented());
            System.out.println();

            double speedup = ooResult.avgNanos / dodResult.avgNanos;
            System.out.println("  SPEEDUP: " + String.format("%.2fx", speedup));
            if (speedup > 1.0) {
                System.out.println("  DOD is " + String.format("%.1f%%", (speedup - 1.0) * 100) + " faster");
            } else if (speedup < 1.0) {
                System.out.println("  DOD is " + String.format("%.1f%%", (1.0 - speedup) * 100) + " slower");
            }
            System.out.println();
        }

        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  BENCHMARK COMPLETE");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // ── OO Benchmark ──────────────────────────────────────────────────────

    public static BenchmarkResult runOOBenchmark(int entityCount, int iterations) {
        // Setup
        SyntheticEntity[] entities = new SyntheticEntity[entityCount];
        for (int i = 0; i < entityCount; i++) {
            entities[i] = new SyntheticEntity();
            entities[i].posX = i * 10.0f;
            entities[i].posY = i * 5.0f;
            entities[i].velX = (i % 2 == 0) ? 50.0f : -50.0f;
            entities[i].velY = (i % 2 == 0) ? -30.0f : 30.0f;
            entities[i].accX = 0.0f;
            entities[i].accY = 980.0f; // gravedad
            entities[i].lifetime = 5.0f;
        }

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            updateOO(entities, DELTA_TIME);
        }

        // Measurement
        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            updateOO(entities, DELTA_TIME);
            long end = System.nanoTime();
            times[i] = end - start;
        }

        return new BenchmarkResult("OO", entityCount, times);
    }

    private static void updateOO(SyntheticEntity[] entities, double deltaTime) {
        for (SyntheticEntity e : entities) {
            e.update(deltaTime);
        }
    }

    // ── DOD Benchmark ─────────────────────────────────────────────────────

    public static BenchmarkResult runDODBenchmark(int entityCount, int iterations) {
        // Setup
        EntityStore store = new EntityStore(entityCount);
        SimulationPipeline pipeline = new SimulationPipeline(store);
        pipeline.register(new AccelerationSystem());
        pipeline.register(new MovementSystem());
        pipeline.register(new LifetimeSystem());

        ComponentMask mask = ComponentMask.EMPTY
            .with(ComponentType.POSITION.id())
            .with(ComponentType.VELOCITY.id())
            .with(ComponentType.ACCELERATION.id())
            .with(ComponentType.LIFETIME.id());

        // Crear entidades
        for (int i = 0; i < entityCount; i++) {
            EntityId id = store.create(mask);
            SimulationHandle h = store.getHandle(id);
            PrimitiveStorage s = store.getStorage();

            int idx = h.index();
            s.positionsX()[idx] = i * 10.0f;
            s.positionsY()[idx] = i * 5.0f;
            s.velocitiesX()[idx] = (i % 2 == 0) ? 50.0f : -50.0f;
            s.velocitiesY()[idx] = (i % 2 == 0) ? -30.0f : 30.0f;
            s.accelerationsX()[idx] = 0.0f;
            s.accelerationsY()[idx] = 980.0f; // gravedad
            s.lifetimes()[idx] = 5.0f;
        }

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            pipeline.update(DELTA_TIME);
        }

        // Measurement
        long[] times = new long[iterations];
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            pipeline.update(DELTA_TIME);
            long end = System.nanoTime();
            times[i] = end - start;
        }

        return new BenchmarkResult("DOD", entityCount, times);
    }

    // ── SyntheticEntity (OO Model) ────────────────────────────────────────

    private static class SyntheticEntity {
        float posX, posY;
        float velX, velY;
        float accX, accY;
        float lifetime;

        void update(double dt) {
            // Acceleration → Velocity
            velX += accX * dt;
            velY += accY * dt;

            // Velocity → Position
            posX += velX * dt;
            posY += velY * dt;

            // Lifetime
            lifetime -= dt;
        }
    }

    // ── BenchmarkResult ───────────────────────────────────────────────────

    public static class BenchmarkResult {
        public final String name;
        public final int entityCount;
        public final long avgNanos;
        public final long minNanos;
        public final long maxNanos;
        public final double stdDevNanos;

        public BenchmarkResult(String name, int entityCount, long[] times) {
            this.name = name;
            this.entityCount = entityCount;

            // Calculate statistics
            long sum = 0;
            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for (long time : times) {
                sum += time;
                if (time < min) min = time;
                if (time > max) max = time;
            }

            this.avgNanos = sum / times.length;
            this.minNanos = min;
            this.maxNanos = max;

            // Standard deviation
            double sumSq = 0;
            for (long time : times) {
                double diff = time - avgNanos;
                sumSq += diff * diff;
            }
            this.stdDevNanos = Math.sqrt(sumSq / times.length);
        }

        public String format() {
            return String.format(
                "%s: avg=%.3fms, min=%.3fms, max=%.3fms, stddev=%.3fms, per-entity=%.1fns, throughput=%.2fM entities/s",
                name,
                avgNanos / 1_000_000.0,
                minNanos / 1_000_000.0,
                maxNanos / 1_000_000.0,
                stdDevNanos / 1_000_000.0,
                (double) avgNanos / entityCount,
                (entityCount / (avgNanos / 1_000_000_000.0)) / 1_000_000.0
            );
        }

        public String formatIndented() {
            return String.format(
                "    Avg:        %8.3f ms\n" +
                "    Min:        %8.3f ms\n" +
                "    Max:        %8.3f ms\n" +
                "    StdDev:     %8.3f ms\n" +
                "    Per-Entity: %8.1f ns\n" +
                "    Throughput: %8.2f M entities/s",
                avgNanos / 1_000_000.0,
                minNanos / 1_000_000.0,
                maxNanos / 1_000_000.0,
                stdDevNanos / 1_000_000.0,
                (double) avgNanos / entityCount,
                (entityCount / (avgNanos / 1_000_000_000.0)) / 1_000_000.0
            );
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private static String formatNumber(int n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }
}
