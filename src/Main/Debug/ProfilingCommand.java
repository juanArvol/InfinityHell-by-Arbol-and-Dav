package Main.Debug;

import Game.Engine.Profiling.ProfilingConfig;

/**
 * Comandos de debug para activar/desactivar profiling desde la aplicación.
 *
 * ── HRFC — Bottleneck Diagnosis Infrastructure ────────────────────────────
 *
 * ProfilingCommand permite al usuario activar profiling sin recompilar.
 * Los comandos se pueden invocar desde una consola de debug o UI.
 *
 * COMANDOS DISPONIBLES:
 *   - startProfiling(): activa profiling estándar
 *   - stopProfiling(): desactiva profiling y exporta resultados
 *   - startStressTest(): activa profiling con configuración de estrés
 *   - startRenderingIsolation(): test 2 (sin rendering de proyectiles)
 *   - startSimulationIsolation(): test 3 (sin lógica de simulation)
 *   - printSummary(): imprime resumen sin detener
 *   - exportResults(): exporta CSV sin detener
 *   - reset(): limpia frames acumulados
 *
 * USO:
 *   // En una consola de debug o UI:
 *   ProfilingCommand.startStressTest();
 *   // ... jugar con WeaponPistola disparando 360 proyectiles ...
 *   ProfilingCommand.stopProfiling();
 *
 * KEYBOARD SHORTCUTS (sugeridos para GameState):
 *   F9  → startStressTest()
 *   F10 → stopProfiling()
 *   F11 → printSummary()
 */
public class ProfilingCommand {

    /**
     * Activa profiling estándar.
     * Recolecta hasta 10K frames, sin verbose logging.
     */
    public static void startProfiling() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        config.setEnabled(true);
        System.out.println("[ProfilingCommand] Profiling started.");
    }

    /**
     * Desactiva profiling, imprime resumen y exporta CSV.
     */
    public static void stopProfiling() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        if (config.isEnabled()) {
            config.setEnabled(false);  // esto dispara auto-export
            System.out.println("[ProfilingCommand] Profiling stopped.");
        } else {
            System.out.println("[ProfilingCommand] Profiling was not active.");
        }
    }

    /**
     * Activa profiling en modo stress test.
     * 
     * Configuración óptima para pruebas de estrés con WeaponPistola:
     *   - enabled: true
     *   - maxFrames: 5000 (últimos 5K frames)
     *   - verbose: false
     *   - autoExport: true
     */
    public static void startStressTest() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        config.enableStressTest();
        System.out.println("[ProfilingCommand] Stress Test profiling started.");
        System.out.println("[ProfilingCommand] Shoot continuously with WeaponPistola (360 bullets/shot).");
    }

    /**
     * Activa profiling en modo de aislamiento de rendering.
     * 
     * Test 2 del HRFC: deshabilita rendering de proyectiles para determinar
     * si el cuello de botella está en el renderer.
     */
    public static void startRenderingIsolation() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        config.enableRenderingIsolationTest();
        System.out.println("[ProfilingCommand] Rendering Isolation Test started.");
        System.out.println("[ProfilingCommand] Bullets will update but NOT render.");
    }

    /**
     * Activa profiling en modo de aislamiento de simulation.
     * 
     * Test 3 del HRFC: reduce lógica de simulation para determinar si el
     * cuello de botella está en behavior/movement.
     */
    public static void startSimulationIsolation() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        config.enableSimulationIsolationTest();
        System.out.println("[ProfilingCommand] Simulation Isolation Test started.");
        System.out.println("[ProfilingCommand] Simulation logic reduced to minimal.");
    }

    /**
     * Imprime el resumen de profiling sin detener la recolección.
     */
    public static void printSummary() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        if (config.isEnabled()) {
            config.printSummary();
        } else {
            System.out.println("[ProfilingCommand] Profiling is not active.");
        }
    }

    /**
     * Exporta resultados a CSV sin detener profiling.
     */
    public static void exportResults() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        if (config.isEnabled()) {
            config.exportResults();
        } else {
            System.out.println("[ProfilingCommand] Profiling is not active.");
        }
    }

    /**
     * Resetea el collector (limpia frames acumulados) sin detener profiling.
     */
    public static void reset() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        if (config.isEnabled()) {
            config.reset();
            System.out.println("[ProfilingCommand] Profiling data reset.");
        } else {
            System.out.println("[ProfilingCommand] Profiling is not active.");
        }
    }

    /**
     * Muestra el estado actual del profiling.
     */
    public static void status() {
        ProfilingConfig config = ProfilingConfig.getInstance();
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  PROFILING STATUS");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("Enabled:              " + config.isEnabled());
        System.out.println("Verbose Logging:      " + config.isVerboseLogging());
        System.out.println("Max Frames:           " + config.getMaxFrames());
        System.out.println("Export Path:          " + config.getExportPath());
        System.out.println();
        System.out.println("Isolation Modes:");
        System.out.println("  Rendering:          " + config.isRenderingIsolation());
        System.out.println("  Simulation:         " + config.isSimulationIsolation());
        System.out.println("  Collision:          " + config.isCollisionIsolation());
        System.out.println();
        if (config.getCollector() != null) {
            System.out.println("Frames Collected:     " + config.getCollector().size());
        }
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // ── Ejemplo de integración con keyboard shortcuts ─────────────────────

    /**
     * Ejemplo de cómo integrar con keyboard shortcuts en GameState.
     * 
     * En GameState, agregar un KeyListener:
     * 
     * <pre>
     * keyboard.onKeyPressed(KeyCode.F9, () -> ProfilingCommand.startStressTest());
     * keyboard.onKeyPressed(KeyCode.F10, () -> ProfilingCommand.stopProfiling());
     * keyboard.onKeyPressed(KeyCode.F11, () -> ProfilingCommand.printSummary());
     * keyboard.onKeyPressed(KeyCode.F12, () -> ProfilingCommand.status());
     * </pre>
     */
    public static void printUsageExample() {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  PROFILING COMMAND USAGE EXAMPLE");
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println();
        System.out.println("// Start stress test");
        System.out.println("ProfilingCommand.startStressTest();");
        System.out.println();
        System.out.println("// Play with WeaponPistola (360 bullets/shot)");
        System.out.println("// Observe FPS drop from 60 to 3-5 FPS");
        System.out.println();
        System.out.println("// Stop and export results");
        System.out.println("ProfilingCommand.stopProfiling();");
        System.out.println();
        System.out.println("// Results will be in profiling_results.csv");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    // ── Main (para testing standalone) ────────────────────────────────────

    public static void main(String[] args) {
        printUsageExample();
    }
}
