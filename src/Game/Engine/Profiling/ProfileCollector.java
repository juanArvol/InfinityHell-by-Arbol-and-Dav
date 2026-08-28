package Game.Engine.Profiling;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Recolector de perfiles de rendimiento de frames.
 *
 * ── HRFC — Bottleneck Diagnosis Infrastructure ────────────────────────────
 *
 * ProfileCollector acumula FrameProfiles durante una sesión de profiling
 * y genera reportes de diagnóstico.
 *
 * CARACTERÍSTICAS:
 *   - Acumulación de múltiples frames
 *   - Estadísticas agregadas (promedio, min, max, percentiles)
 *   - Export a CSV para análisis externo
 *   - Detección automática de cuellos de botella
 *   - Modo ring buffer (mantiene últimos N frames, no todos)
 *
 * USO:
 *   ProfileCollector collector = new ProfileCollector(enabled: true);
 *   // cada frame:
 *   FrameProfile profile = new FrameProfile();
 *   profile.activeProjectiles = ...;
 *   profile.simulationMs = ...;
 *   collector.record(profile);
 *
 *   // al finalizar prueba:
 *   collector.printSummary();
 *   collector.exportCSV("profiling_results.csv");
 */
public class ProfileCollector {
    private final boolean enabled;
    private final int maxFrames;
    private final List<FrameProfile> frames;
    private long startTimeNanos;

    /**
     * Constructor con ring buffer habilitado.
     *
     * @param enabled si true, acumula profiles; si false, no hace nada (zero overhead)
     * @param maxFrames número máximo de frames a retener (0 = sin límite)
     */
    public ProfileCollector(boolean enabled, int maxFrames) {
        this.enabled = enabled;
        this.maxFrames = maxFrames;
        this.frames = new ArrayList<>();
        this.startTimeNanos = System.nanoTime();
    }

    /**
     * Constructor sin límite de frames.
     */
    public ProfileCollector(boolean enabled) {
        this(enabled, 0);  // sin límite
    }

    /**
     * Registra un FrameProfile.
     * Si !enabled, no hace nada (zero overhead).
     * Si maxFrames > 0 y se alcanzó el límite, elimina el frame más antiguo.
     */
    public void record(FrameProfile profile) {
        if (!enabled) return;

        synchronized (frames) {
            if (maxFrames > 0 && frames.size() >= maxFrames) {
                frames.remove(0);  // eliminar el más antiguo (FIFO)
            }
            frames.add(profile);
        }
    }

    /**
     * Resetea el collector (borra todos los frames acumulados).
     */
    public void reset() {
        if (!enabled) return;
        synchronized (frames) {
            frames.clear();
            startTimeNanos = System.nanoTime();
        }
    }

    /**
     * Retorna el número de frames recolectados.
     */
    public int size() {
        return enabled ? frames.size() : 0;
    }

    /**
     * Imprime un resumen estadístico de los frames recolectados.
     */
    public void printSummary() {
        if (!enabled || frames.isEmpty()) {
            System.out.println("[ProfileCollector] No data collected.");
            return;
        }

        System.out.println("═══════════════════════════════════════════════════════════════════════════");
        System.out.println("  HRFC — BOTTLENECK DIAGNOSIS REPORT");
        System.out.println("═══════════════════════════════════════════════════════════════════════════");
        System.out.println();

        double totalDurationSec = (System.nanoTime() - startTimeNanos) / 1_000_000_000.0;
        System.out.printf("Profiling Duration:  %.2f seconds%n", totalDurationSec);
        System.out.printf("Frames Collected:    %d frames%n", frames.size());
        System.out.println();

        // ── Promedios ──────────────────────────────────────────────────────
        double avgActiveProjectiles = average(frames, f -> f.activeProjectiles);
        double avgFps = average(frames, f -> f.fps);
        double avgFrameTime = average(frames, f -> f.frameTimeMs);
        double avgSimulation = average(frames, f -> f.simulationMs);
        double avgBehavior = average(frames, f -> f.behaviorMs);
        double avgMovement = average(frames, f -> f.movementMs);
        double avgPhysics = average(frames, f -> f.physicsMs);
        double avgCollision = average(frames, f -> f.collisionMs);
        double avgRendering = average(frames, f -> f.renderingMs);
        double avgBulletRender = average(frames, f -> f.bulletRenderMs);
        double avgOther = average(frames, f -> f.otherMs);

        System.out.println("─── AVERAGE PERFORMANCE ───────────────────────────────────────────────────");
        System.out.printf("Active Projectiles:  %.1f%n", avgActiveProjectiles);
        System.out.printf("FPS:                 %.1f%n", avgFps);
        System.out.printf("Frame Time:          %.2f ms%n", avgFrameTime);
        System.out.println();
        System.out.printf("  Simulation:        %.2f ms  (%.1f%%)%n",
            avgSimulation, (avgSimulation / avgFrameTime) * 100);
        System.out.printf("    Behavior:        %.2f ms  (%.1f%%)%n",
            avgBehavior, (avgBehavior / avgFrameTime) * 100);
        System.out.printf("    Movement:        %.2f ms  (%.1f%%)%n",
            avgMovement, (avgMovement / avgFrameTime) * 100);
        System.out.printf("    Physics:         %.2f ms  (%.1f%%)%n",
            avgPhysics, (avgPhysics / avgFrameTime) * 100);
        System.out.printf("    Collision:       %.2f ms  (%.1f%%)%n",
            avgCollision, (avgCollision / avgFrameTime) * 100);
        System.out.printf("  Rendering:         %.2f ms  (%.1f%%)%n",
            avgRendering, (avgRendering / avgFrameTime) * 100);
        System.out.printf("    Bullet Render:   %.2f ms  (%.1f%%)%n",
            avgBulletRender, (avgBulletRender / avgFrameTime) * 100);
        System.out.printf("  Other:             %.2f ms  (%.1f%%)%n",
            avgOther, (avgOther / avgFrameTime) * 100);
        System.out.println();

        // ── Min / Max ──────────────────────────────────────────────────────
        double maxFrameTime = max(frames, f -> f.frameTimeMs);
        double maxSimulation = max(frames, f -> f.simulationMs);
        double maxCollision = max(frames, f -> f.collisionMs);
        double maxRendering = max(frames, f -> f.renderingMs);
        int maxProjectiles = (int) max(frames, f -> f.activeProjectiles);

        System.out.println("─── WORST CASE ────────────────────────────────────────────────────────────");
        System.out.printf("Max Projectiles:     %d%n", maxProjectiles);
        System.out.printf("Max Frame Time:      %.2f ms%n", maxFrameTime);
        System.out.printf("Max Simulation:      %.2f ms%n", maxSimulation);
        System.out.printf("Max Collision:       %.2f ms%n", maxCollision);
        System.out.printf("Max Rendering:       %.2f ms%n", maxRendering);
        System.out.println();

        // ── Bottleneck Detection ───────────────────────────────────────────
        System.out.println("─── BOTTLENECK ANALYSIS ───────────────────────────────────────────────────");
        String primaryBottleneck = detectPrimaryBottleneck(
            avgSimulation, avgCollision, avgRendering, avgBulletRender
        );
        System.out.printf("PRIMARY BOTTLENECK:  %s%n", primaryBottleneck);

        // Scaling analysis
        if (frames.size() > 10) {
            String scaling = analyzeScaling();
            System.out.printf("SCALING:             %s%n", scaling);
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════════════════════");
    }

    /**
     * Exporta los frames recolectados a CSV para análisis externo.
     */
    public void exportCSV(String filePath) {
        if (!enabled || frames.isEmpty()) {
            System.out.println("[ProfileCollector] No data to export.");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Header
            writer.println("frame,activeProjectiles,fps,frameTimeMs,simulationMs," +
                "behaviorMs,movementMs,physicsMs,collisionMs,renderingMs,bulletRenderMs,otherMs");

            // Data rows
            for (FrameProfile f : frames) {
                writer.printf("%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                    f.frameNumber, f.activeProjectiles, f.fps,
                    f.frameTimeMs, f.simulationMs, f.behaviorMs, f.movementMs,
                    f.physicsMs, f.collisionMs, f.renderingMs, f.bulletRenderMs, f.otherMs);
            }

            System.out.printf("[ProfileCollector] Exported %d frames to: %s%n", frames.size(), filePath);
        } catch (IOException e) {
            System.err.println("[ProfileCollector] Failed to export CSV: " + e.getMessage());
        }
    }

    // ── Private Helpers ────────────────────────────────────────────────────

    private interface ValueExtractor {
        double extract(FrameProfile f);
    }

    private static double average(List<FrameProfile> profiles, ValueExtractor extractor) {
        if (profiles.isEmpty()) return 0.0;
        double sum = 0.0;
        for (FrameProfile f : profiles) {
            sum += extractor.extract(f);
        }
        return sum / profiles.size();
    }

    private static double max(List<FrameProfile> profiles, ValueExtractor extractor) {
        if (profiles.isEmpty()) return 0.0;
        double maxVal = Double.NEGATIVE_INFINITY;
        for (FrameProfile f : profiles) {
            double val = extractor.extract(f);
            if (val > maxVal) maxVal = val;
        }
        return maxVal;
    }

    private String detectPrimaryBottleneck(double avgSim, double avgCol, double avgRender, double avgBulletRender) {
        // Determinar el subsistema que consume más tiempo promedio
        double maxTime = Math.max(avgSim, Math.max(avgCol, avgRender));

        if (maxTime == avgRender) {
            if (avgBulletRender > avgRender * 0.7) {
                return "Rendering (Bullets specifically)";
            } else {
                return "Rendering (General)";
            }
        } else if (maxTime == avgCol) {
            return "Collision Detection";
        } else if (maxTime == avgSim) {
            return "Simulation (Behavior + Movement + Physics)";
        } else {
            return "Unknown";
        }
    }

    private String analyzeScaling() {
        // Análisis simple de escalamiento: comparar tiempo vs proyectiles
        // Si correlación alta → O(n); si cuadrática → O(n²)
        
        // Agrupar frames por cantidad de proyectiles
        int[] buckets = new int[6];  // 0-100, 100-250, 250-500, 500-1000, 1000-2000, 2000+
        double[] avgTimes = new double[6];
        int[] counts = new int[6];

        for (FrameProfile f : frames) {
            int bucket = getBucket(f.activeProjectiles);
            avgTimes[bucket] += f.frameTimeMs;
            counts[bucket]++;
        }

        // Promediar por bucket
        for (int i = 0; i < buckets.length; i++) {
            if (counts[i] > 0) {
                avgTimes[i] /= counts[i];
            }
        }

        // Comparar escalamiento 100→500 vs 500→2000
        // Si tiempo crece linealmente → O(n)
        // Si tiempo crece cuadráticamente → O(n²)
        if (counts[2] > 0 && counts[4] > 0) {
            double ratio100to500 = avgTimes[2] / Math.max(0.001, avgTimes[1]);
            double ratio500to2000 = avgTimes[4] / Math.max(0.001, avgTimes[2]);

            if (ratio500to2000 > ratio100to500 * 2.5) {
                return "O(n²) or worse — superlinear growth detected";
            } else if (ratio500to2000 > ratio100to500 * 1.5) {
                return "O(n log n) — subquadratic growth";
            } else {
                return "O(n) — linear scaling (expected)";
            }
        }

        return "Insufficient data for scaling analysis";
    }

    private int getBucket(int projectiles) {
        if (projectiles < 100) return 0;
        if (projectiles < 250) return 1;
        if (projectiles < 500) return 2;
        if (projectiles < 1000) return 3;
        if (projectiles < 2000) return 4;
        return 5;
    }

    /**
     * Retorna el frame con el peor frameTime.
     */
    public FrameProfile getWorstFrame() {
        if (!enabled || frames.isEmpty()) return null;
        FrameProfile worst = frames.get(0);
        for (FrameProfile f : frames) {
            if (f.frameTimeMs > worst.frameTimeMs) {
                worst = f;
            }
        }
        return worst;
    }

    /**
     * Retorna el frame con más proyectiles activos.
     */
    public FrameProfile getFrameWithMostProjectiles() {
        if (!enabled || frames.isEmpty()) return null;
        FrameProfile most = frames.get(0);
        for (FrameProfile f : frames) {
            if (f.activeProjectiles > most.activeProjectiles) {
                most = f;
            }
        }
        return most;
    }

    /**
     * Actualiza el último frame registrado con tiempo de rendering.
     * Llamar desde WorldManager después de draw().
     *
     * @param renderTimeMs tiempo de rendering en milisegundos
     */
    public void updateLastFrameRenderTime(double renderTimeMs) {
        if (!enabled || frames.isEmpty()) return;
        synchronized (frames) {
            FrameProfile last = frames.get(frames.size() - 1);
            last.renderingMs = renderTimeMs;
            last.frameTimeMs = last.simulationMs + last.renderingMs;
            last.computeOther();
        }
    }
}
