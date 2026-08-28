package Game.Engine.Profiling;

/**
 * Configuración global de profiling.
 *
 * ── HRFC — Bottleneck Diagnosis Infrastructure ────────────────────────────
 *
 * ProfilingConfig centraliza toda la configuración de profiling para
 * diagnóstico de cuellos de botella.
 *
 * CONFIGURACIÓN:
 *   - enabled: activa/desactiva profiling globalmente (default: false)
 *   - verboseLogging: imprime cada frame profile en consola (default: false)
 *   - maxFrames: ring buffer size (0 = sin límite)
 *   - autoExportOnShutdown: exporta CSV al cerrar (default: true si enabled)
 *   - exportPath: ruta del CSV de resultados
 *
 * MODOS ESPECIALES:
 *   - renderingIsolation: deshabilita rendering de proyectiles (test 2)
 *   - simulationIsolation: reduce lógica de simulation (test 3)
 *   - collisionIsolation: mide collision puro sin dispatch
 *
 * SINGLETON PATTERN:
 *   Usar ProfilingConfig.getInstance() para acceso global.
 *   Configurar desde bootstrap o debug settings.
 *
 * USO:
 *   ProfilingConfig config = ProfilingConfig.getInstance();
 *   config.setEnabled(true);
 *   config.setMaxFrames(1000);
 *   
 *   // Después de la sesión:
 *   config.exportResults();
 */
public class ProfilingConfig {
    private static final ProfilingConfig INSTANCE = new ProfilingConfig();

    // ── Configuration ──────────────────────────────────────────────────────
    private boolean enabled = false;
    private boolean verboseLogging = false;
    private int maxFrames = 10000;  // últimos 10K frames (ring buffer)
    private boolean autoExportOnShutdown = true;
    private String exportPath = "profiling_results.csv";

    // ── Isolation Modes (diagnostic tests) ────────────────────────────────
    private boolean renderingIsolation = false;   // skip bullet rendering
    private boolean simulationIsolation = false;  // reduce simulation load
    private boolean collisionIsolation = false;   // measure collision only

    // ── ProfileCollector ───────────────────────────────────────────────────
    private ProfileCollector collector = null;

    private ProfilingConfig() {
        // Private constructor — singleton
    }

    public static ProfilingConfig getInstance() {
        return INSTANCE;
    }

    // ── Enable/Disable Profiling ──────────────────────────────────────────

    /**
     * Activa profiling y crea el collector.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled && collector == null) {
            collector = new ProfileCollector(true, maxFrames);
            System.out.println("[ProfilingConfig] Profiling ENABLED.");
            System.out.println("[ProfilingConfig] Collecting up to " + maxFrames + " frames.");
        } else if (!enabled && collector != null) {
            System.out.println("[ProfilingConfig] Profiling DISABLED.");
            if (autoExportOnShutdown) {
                exportResults();
            }
            collector = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVerboseLogging(boolean verbose) {
        this.verboseLogging = verbose;
    }

    public boolean isVerboseLogging() {
        return verboseLogging;
    }

    public void setMaxFrames(int maxFrames) {
        this.maxFrames = maxFrames;
        if (collector != null) {
            // Recrear collector con nuevo límite
            boolean wasEnabled = enabled;
            setEnabled(false);
            setEnabled(wasEnabled);
        }
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    public void setAutoExportOnShutdown(boolean auto) {
        this.autoExportOnShutdown = auto;
    }

    public void setExportPath(String path) {
        this.exportPath = path;
    }

    public String getExportPath() {
        return exportPath;
    }

    // ── Isolation Modes ────────────────────────────────────────────────────

    public void setRenderingIsolation(boolean isolated) {
        this.renderingIsolation = isolated;
        if (isolated) {
            System.out.println("[ProfilingConfig] RENDERING ISOLATION MODE: bullet rendering DISABLED.");
        } else {
            System.out.println("[ProfilingConfig] Rendering isolation OFF.");
        }
    }

    public boolean isRenderingIsolation() {
        return renderingIsolation;
    }

    public void setSimulationIsolation(boolean isolated) {
        this.simulationIsolation = isolated;
        if (isolated) {
            System.out.println("[ProfilingConfig] SIMULATION ISOLATION MODE: reduced simulation logic.");
        } else {
            System.out.println("[ProfilingConfig] Simulation isolation OFF.");
        }
    }

    public boolean isSimulationIsolation() {
        return simulationIsolation;
    }

    public void setCollisionIsolation(boolean isolated) {
        this.collisionIsolation = isolated;
        if (isolated) {
            System.out.println("[ProfilingConfig] COLLISION ISOLATION MODE: measure collision without dispatch.");
        } else {
            System.out.println("[ProfilingConfig] Collision isolation OFF.");
        }
    }

    public boolean isCollisionIsolation() {
        return collisionIsolation;
    }

    // ── ProfileCollector Access ────────────────────────────────────────────

    /**
     * Retorna el ProfileCollector activo.
     * null si profiling está deshabilitado.
     */
    public ProfileCollector getCollector() {
        return collector;
    }

    /**
     * Registra un FrameProfile.
     * Si profiling deshabilitado, no hace nada (zero overhead).
     */
    public void recordFrame(FrameProfile profile) {
        if (collector != null) {
            collector.record(profile);
            if (verboseLogging) {
                System.out.println(profile);
            }
        }
    }

    /**
     * Exporta resultados a CSV.
     */
    public void exportResults() {
        if (collector != null) {
            collector.printSummary();
            collector.exportCSV(exportPath);
        }
    }

    /**
     * Resetea el collector (borra frames acumulados).
     */
    public void reset() {
        if (collector != null) {
            collector.reset();
            System.out.println("[ProfilingConfig] Collector reset.");
        }
    }

    /**
     * Imprime el resumen de profiling actual.
     */
    public void printSummary() {
        if (collector != null) {
            collector.printSummary();
        } else {
            System.out.println("[ProfilingConfig] Profiling is disabled.");
        }
    }

    /**
     * Configuración rápida para pruebas de estrés.
     * 
     * Activa profiling con configuración óptima para diagnóstico:
     *   - enabled: true
     *   - maxFrames: 5000 (últimos 5K frames)
     *   - verbose: false (no spam consola)
     *   - autoExport: true
     */
    public void enableStressTest() {
        setMaxFrames(5000);
        setAutoExportOnShutdown(true);
        setVerboseLogging(false);
        setEnabled(true);
        System.out.println("[ProfilingConfig] STRESS TEST MODE activated.");
    }

    /**
     * Configuración rápida para aislamiento de rendering.
     * 
     * Test 2 del HRFC: deshabilita rendering de proyectiles para medir
     * si el cuello de botella está en el renderer.
     */
    public void enableRenderingIsolationTest() {
        enableStressTest();
        setRenderingIsolation(true);
        System.out.println("[ProfilingConfig] TEST 2: Rendering Isolation");
    }

    /**
     * Configuración rápida para aislamiento de simulation.
     * 
     * Test 3 del HRFC: reduce lógica de simulation para medir si el
     * cuello de botella está en behavior/movement.
     */
    public void enableSimulationIsolationTest() {
        enableStressTest();
        setSimulationIsolation(true);
        System.out.println("[ProfilingConfig] TEST 3: Simulation Isolation");
    }
}
