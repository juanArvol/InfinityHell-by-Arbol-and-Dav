package Game.Engine.Profiling;

/**
 * Perfil de rendimiento de un frame individual.
 *
 * ── HRFC — Bottleneck Diagnosis Infrastructure ────────────────────────────
 *
 * FrameProfile captura los tiempos de ejecución de cada subsistema en un
 * frame específico para diagnóstico de cuellos de botella.
 *
 * SUBSISTEMAS INSTRUMENTADOS:
 *   - Simulation (total del update cycle)
 *   - Behavior (ProjectileMovement + BulletBehavior)
 *   - Physics (integración de fuerzas y gravedad)
 *   - Collision (detección y resolución)
 *   - Rendering (total del render cycle)
 *     - Bullet Rendering (específicamente los proyectiles)
 *   - Other (tiempo no atribuido a ningún subsistema)
 *
 * DATOS POR FRAME:
 *   - activeProjectiles: número de proyectiles activos durante el frame
 *   - fps: frames por segundo del momento de captura
 *   - frameTimeMs: tiempo total del frame (simulation + render)
 *
 * USO:
 *   FrameProfile profile = new FrameProfile();
 *   profile.activeProjectiles = pool.getActiveInstances();
 *   profile.simulationMs = ...;
 *   ProfileCollector.record(profile);
 */
public class FrameProfile {
    // ── Metadata del frame ────────────────────────────────────────────────
    public long frameNumber = 0;
    public int activeProjectiles = 0;
    public int fps = 0;
    public double frameTimeMs = 0.0;

    // ── Simulation breakdown ───────────────────────────────────────────────
    public double simulationMs = 0.0;
    public double behaviorMs = 0.0;
    public double movementMs = 0.0;
    public double physicsMs = 0.0;
    public double collisionMs = 0.0;

    // ── Collision breakdown ────────────────────────────────────────────────
    public double collisionBroadPhaseMs = 0.0;
    public double collisionNarrowPhaseMs = 0.0;
    public double collisionDispatchMs = 0.0;

    // ── Rendering breakdown ────────────────────────────────────────────────
    public double renderingMs = 0.0;
    public double bulletRenderMs = 0.0;

    // ── Otro ───────────────────────────────────────────────────────────────
    public double otherMs = 0.0;

    /**
     * Calcula el overhead no atribuido a ningún subsistema.
     * other = frameTime - (simulation + rendering)
     */
    public void computeOther() {
        otherMs = frameTimeMs - (simulationMs + renderingMs);
        if (otherMs < 0.0) otherMs = 0.0;  // evitar negativos por redondeo
    }

    /**
     * Representación legible del profile.
     */
    @Override
    public String toString() {
        return String.format(
            "Frame #%d | Projectiles=%d | FPS=%d | Total=%.2fms " +
            "[ Sim=%.2fms (Bhv=%.2fms Mv=%.2fms Phys=%.2fms Col=%.2fms) " +
            "Render=%.2fms (Bullets=%.2fms) Other=%.2fms ]",
            frameNumber, activeProjectiles, fps, frameTimeMs,
            simulationMs, behaviorMs, movementMs, physicsMs, collisionMs,
            renderingMs, bulletRenderMs, otherMs
        );
    }
}
