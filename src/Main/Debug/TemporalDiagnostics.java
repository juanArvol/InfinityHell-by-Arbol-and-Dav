package Main.Debug;

/**
 * TemporalDiagnostics — Sistema de diagnóstico temporal para HRFC.
 *
 * ── OBJETIVO ──────────────────────────────────────────────────────────────
 *
 * Instrumentar la propagación completa de deltaTime desde GameLoop hasta
 * cada consumidor temporal, identificando dónde se pierde velocidad de
 * simulación.
 *
 * IMPORTANTE:
 *   Este sistema NO asume que la causa del slowdown sea únicamente:
 *     - targetFps=31
 *     - Los cinco subsistemas frame-based conocidos
 *
 *   El objetivo es MEDIR Y VALIDAR empíricamente antes de implementar
 *   cambios masivos.
 *
 * ── MÉTRICAS CRÍTICAS ─────────────────────────────────────────────────────
 *
 * 1. rawDeltaTime       → System.nanoTime() diff sin clamping
 * 2. effectiveDeltaTime → valor post-clamp que recibe GameState
 * 3. FPS                → renders por segundo
 * 4. UPS                → updates por segundo (simulation rate)
 * 5. Σ deltaTime        → acumulación temporal durante 1 segundo real
 *
 * 6. Propagación:
 *    GameLoop → GameState → WorldManager → AISystem
 *                                       → CollisionsSystem
 *                                       → CameraSystem
 *
 * ── PRUEBA DE IDENTIDAD TEMPORAL ──────────────────────────────────────────
 *
 * Tras 1 segundo real transcurrido:
 *   Σ effectiveDeltaTime ≈ 1.0 segundo
 *
 * Si obtiene:
 *   Σ effectiveDeltaTime ≈ 0.5s → el juego simula la mitad del tiempo real
 *   Σ effectiveDeltaTime ≈ 1.0s → temporal integrity correcta
 *   Σ effectiveDeltaTime ≈ 1.5s → el juego simula más rápido que tiempo real
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 * GameLoop.run():
 *   TemporalDiagnostics.recordRawDelta(rawDeltaTime);
 *   TemporalDiagnostics.recordEffectiveDelta(clampedDeltaTime);
 *   TemporalDiagnostics.recordUpdate();
 *   TemporalDiagnostics.recordRender();
 *
 * Cada segundo:
 *   TemporalDiagnostics.printReport();
 *
 * ── OUTPUT ESPERADO ───────────────────────────────────────────────────────
 *
 *   [TEMPORAL] ─────────────────────────────────────────────
 *   FPS:  60    UPS:  60
 *   rawΔt avg:    0.0167s    (tiempo real promedio)
 *   effectiveΔt:  0.0167s    (tiempo simulado promedio)
 *   Σ effectiveΔt: 1.000s    (1 segundo simulado en 1 segundo real)
 *   Target FPS:    60        Max delta: 0.0833s
 *   Temporal integrity: OK   (ratio 1.00)
 *   ───────────────────────────────────────────────────────
 *
 * Si el ratio es 0.5 → identifica que algo está ralentizando tiempo simulado.
 */
public final class TemporalDiagnostics {

    // ── Configuración ─────────────────────────────────────────────────────────
    private static boolean enabled = true;
    private static final long REPORT_INTERVAL_NS = 1_000_000_000L; // 1 segundo

    // ── Estado de medición ────────────────────────────────────────────────────
    private static long lastReportTime = System.nanoTime();

    private static double rawDeltaSum = 0.0;
    private static double effectiveDeltaSum = 0.0;
    private static int    updateCount = 0;
    private static int    renderCount = 0;

    private static double maxRawDelta = 0.0;
    private static double minRawDelta = Double.MAX_VALUE;
    private static double maxEffectiveDelta = 0.0;
    private static double minEffectiveDelta = Double.MAX_VALUE;

    // ── Configuración del loop (inyectada) ────────────────────────────────────
    private static int    targetFps = 60;
    private static double maxDeltaCatchUp = 5.0;

    // ── API Pública ───────────────────────────────────────────────────────────

    public static void enable() {
        enabled = true;
    }

    public static void disable() {
        enabled = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Configura los parámetros del GameLoop para el diagnóstico.
     */
    public static void configure(int targetFps, double maxDeltaCatchUp) {
        TemporalDiagnostics.targetFps = targetFps;
        TemporalDiagnostics.maxDeltaCatchUp = maxDeltaCatchUp;
    }

    /**
     * Registra el tiempo real transcurrido sin clamping.
     */
    public static void recordRawDelta(double rawDeltaSeconds) {
        if (!enabled) return;
        rawDeltaSum += rawDeltaSeconds;
        maxRawDelta = Math.max(maxRawDelta, rawDeltaSeconds);
        minRawDelta = Math.min(minRawDelta, rawDeltaSeconds);
    }

    /**
     * Registra el deltaTime efectivo post-clamp que recibe GameState.
     */
    public static void recordEffectiveDelta(double effectiveDeltaSeconds) {
        if (!enabled) return;
        effectiveDeltaSum += effectiveDeltaSeconds;
        maxEffectiveDelta = Math.max(maxEffectiveDelta, effectiveDeltaSeconds);
        minEffectiveDelta = Math.min(minEffectiveDelta, effectiveDeltaSeconds);
    }

    /**
     * Registra un simulation update.
     */
    public static void recordUpdate() {
        if (!enabled) return;
        updateCount++;
    }

    /**
     * Registra un render.
     */
    public static void recordRender() {
        if (!enabled) return;
        renderCount++;
    }

    /**
     * Genera y muestra el reporte de diagnóstico cada segundo.
     * Retorna true si generó reporte (para que GameLoop pueda hacer reset).
     */
    public static boolean checkAndReport() {
        if (!enabled) return false;

        long now = System.nanoTime();
        long elapsed = now - lastReportTime;

        if (elapsed >= REPORT_INTERVAL_NS) {
            printReport(elapsed / 1_000_000_000.0);
            reset();
            lastReportTime = now;
            return true;
        }
        return false;
    }

    /**
     * Genera reporte inmediato (forzado).
     */
    public static void forceReport() {
        if (!enabled) return;
        long now = System.nanoTime();
        long elapsed = now - lastReportTime;
        printReport(elapsed / 1_000_000_000.0);
        reset();
        lastReportTime = now;
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private static void printReport(double realTimeElapsed) {
        double avgRawDelta = updateCount > 0 ? rawDeltaSum / updateCount : 0.0;
        double avgEffectiveDelta = updateCount > 0 ? effectiveDeltaSum / updateCount : 0.0;

        double temporalRatio = realTimeElapsed > 0.0 ? effectiveDeltaSum / realTimeElapsed : 0.0;
        String integrityStatus = getIntegrityStatus(temporalRatio);

        double targetDelta = 1.0 / targetFps;
        double maxDelta = maxDeltaCatchUp * targetDelta;

        System.out.println("[TEMPORAL] ─────────────────────────────────────────");
        System.out.printf("FPS: %3d    UPS: %3d%n", renderCount, updateCount);
        System.out.printf("Real time elapsed:    %.3fs%n", realTimeElapsed);
        System.out.printf("rawΔt avg:      %.4fs   (min: %.4fs  max: %.4fs)%n",
            avgRawDelta, minRawDelta == Double.MAX_VALUE ? 0.0 : minRawDelta, maxRawDelta);
        System.out.printf("effectiveΔt avg: %.4fs   (min: %.4fs  max: %.4fs)%n",
            avgEffectiveDelta, minEffectiveDelta == Double.MAX_VALUE ? 0.0 : minEffectiveDelta, maxEffectiveDelta);
        System.out.printf("Σ effectiveΔt:   %.3fs   (simulated time in 1 real second)%n", effectiveDeltaSum);
        System.out.printf("Target FPS:      %d     Target Δt: %.4fs    Max Δt: %.4fs%n",
            targetFps, targetDelta, maxDelta);
        System.out.printf("Temporal ratio:  %.3f   Status: %s%n", temporalRatio, integrityStatus);
        System.out.println("───────────────────────────────────────────────────");
    }

    private static String getIntegrityStatus(double ratio) {
        if (ratio >= 0.95 && ratio <= 1.05) {
            return "OK (1:1 real time)";
        } else if (ratio < 0.95) {
            return String.format("SLOW (%.0f%% of real time)", ratio * 100);
        } else {
            return String.format("FAST (%.0f%% of real time)", ratio * 100);
        }
    }

    private static void reset() {
        rawDeltaSum = 0.0;
        effectiveDeltaSum = 0.0;
        updateCount = 0;
        renderCount = 0;
        maxRawDelta = 0.0;
        minRawDelta = Double.MAX_VALUE;
        maxEffectiveDelta = 0.0;
        minEffectiveDelta = Double.MAX_VALUE;
    }

    /**
     * Test unitario: simular un segundo a diferentes FPS.
     */
    public static void runIntegrityTest() {
        System.out.println("[TEMPORAL TEST] Starting integrity test...");

        // Test 1: 60 FPS perfecto
        reset();
        configure(60, 5.0);
        for (int i = 0; i < 60; i++) {
            recordRawDelta(1.0 / 60.0);
            recordEffectiveDelta(1.0 / 60.0);
            recordUpdate();
            recordRender();
        }
        System.out.println("\nTest 1: 60 FPS perfect");
        printReport(1.0);

        // Test 2: 30 FPS perfecto
        reset();
        configure(30, 5.0);
        for (int i = 0; i < 30; i++) {
            recordRawDelta(1.0 / 30.0);
            recordEffectiveDelta(1.0 / 30.0);
            recordUpdate();
            recordRender();
        }
        System.out.println("\nTest 2: 30 FPS perfect");
        printReport(1.0);

        // Test 3: Frame-based degradation (simulando frame counter)
        reset();
        configure(60, 5.0);
        for (int i = 0; i < 30; i++) { // solo 30 updates en 1 segundo real
            recordRawDelta(1.0 / 60.0);
            recordEffectiveDelta(1.0 / 60.0);
            recordUpdate();
            recordRender();
        }
        System.out.println("\nTest 3: Frame-based degradation (30 updates @ 60 FPS target)");
        printReport(1.0);

        System.out.println("[TEMPORAL TEST] Complete.\n");
    }
}
