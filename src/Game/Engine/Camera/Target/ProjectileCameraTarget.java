package Game.Engine.Camera.Target;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Target de cámara que sigue a un proyectil u objeto dinámico de vida finita.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Cámara sigue a un proyectil especial mientras vive:
 *   ProjectileCameraTarget t = ProjectileCameraTarget.of(
 *       bullet,
 *       bullet::isPendingDestruction
 *   );
 *   cameraSystem.pushTarget(t, 80); // prioridad alta durante vida del proyectil
 *
 * ── CICLO DE VIDA ─────────────────────────────────────────────────────────
 * El target expira cuando:
 *   - El proyectil es destruido (isDestroyed retorna true), O
 *   - Se supera el maxDurationSeconds (0 = sin límite por tiempo)
 *
 * Cuando expira, la cámara vuelve al target anterior de forma automática.
 *
 * ── LEAD FACTOR ───────────────────────────────────────────────────────────
 * Un factor de adelanto (leadFactor > 0) desplaza la vista en la dirección
 * de movimiento del proyectil, permitiendo ver qué hay delante de él.
 */
public final class ProjectileCameraTarget implements CameraTarget {

    private final Supplier<Vector2D> positionSupplier;
    private final BooleanSupplier    isDestroyed;
    private final double             maxDurationSeconds;
    private final float              leadFactor;
    private final int                priority;

    private double   elapsedSeconds = 0.0;
    private Vector2D lastPos        = null;

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * @param projectile    el GameObjects del proyectil
     * @param isDestroyedFn condición que retorna true cuando el proyectil muere
     */
    public static ProjectileCameraTarget of(GameObjects projectile,
                                            BooleanSupplier isDestroyedFn) {
        return new ProjectileCameraTarget(
            () -> projectile.getTransform().getPosition(),
            isDestroyedFn,
            0, 0.0f, 200
        );
    }

    public static ProjectileCameraTarget of(GameObjects projectile,
                                            BooleanSupplier isDestroyedFn,
                                            double maxDurationSeconds,
                                            float leadFactor) {
        return new ProjectileCameraTarget(
            () -> projectile.getTransform().getPosition(),
            isDestroyedFn,
            maxDurationSeconds, leadFactor, 200
        );
    }

    public ProjectileCameraTarget(Supplier<Vector2D> positionSupplier,
                                   BooleanSupplier isDestroyed,
                                   double maxDurationSeconds,
                                   float leadFactor,
                                   int priority) {
        this.positionSupplier   = positionSupplier;
        this.isDestroyed        = isDestroyed;
        this.maxDurationSeconds = maxDurationSeconds;
        this.leadFactor         = leadFactor;
        this.priority           = priority;
    }

    @Override
    public Vector2D getPosition() {
        Vector2D current = positionSupplier.get();
        if (current == null) return lastPos;

        if (leadFactor == 0.0f || lastPos == null) {
            return current;
        }

        // Look-ahead en la dirección de movimiento
        double dx = (current.getX() - lastPos.getX()) * leadFactor;
        double dy = (current.getY() - lastPos.getY()) * leadFactor;
        return new Vector2D(current.getX() + dx, current.getY() + dy);
    }

    @Override
    public void update(double deltaTime) {
        Vector2D pos = positionSupplier.get();
        if (pos != null) lastPos = new Vector2D(pos.getX(), pos.getY());
        elapsedSeconds += deltaTime;
    }

    @Override
    public boolean isExpired() {
        if (isDestroyed.getAsBoolean()) return true;
        return maxDurationSeconds > 0 && elapsedSeconds >= maxDurationSeconds;
    }

    @Override
    public int getPriority() { return priority; }
}
