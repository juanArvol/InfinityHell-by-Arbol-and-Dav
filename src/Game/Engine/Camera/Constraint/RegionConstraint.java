package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Restricción que confina la cámara a una región rectangular del mundo.
 *
 * ── USOS ──────────────────────────────────────────────────────────────────
 *   // Cámara confinada a la sala del boss:
 *   RegionConstraint bossRoom = new RegionConstraint(500, 200, 1000, 700);
 *   constraints.add(bossRoom);
 *
 *   // Al salir de la batalla, eliminar la restricción:
 *   constraints.remove(bossRoom);
 *
 * ── DURACIÓN OPCIONAL ─────────────────────────────────────────────────────
 * Si durationTicks > 0, la restricción expira automáticamente.
 */
public final class RegionConstraint implements CameraConstraint {

    private final double minX;
    private final double minY;
    private final double maxX;
    private final double maxY;
    private final double durationSeconds;
    private final int    priority;

    private double  elapsedSeconds = 0.0;
    private boolean active         = true;

    /**
     * @param minX, minY    esquina superior izquierda de la región (en coords de mundo)
     * @param maxX, maxY    esquina inferior derecha
     */
    public RegionConstraint(double minX, double minY, double maxX, double maxY) {
        this(minX, minY, maxX, maxY, 0, 500);
    }

    public RegionConstraint(double minX, double minY, double maxX, double maxY,
                             double durationSeconds, int priority) {
        this.minX             = minX;
        this.minY             = minY;
        this.maxX             = maxX;
        this.maxY             = maxY;
        this.durationSeconds  = durationSeconds;
        this.priority         = priority;
    }

    public void deactivate() { active = false; }

    public void update(double deltaTime) {
        if (durationSeconds > 0) {
            elapsedSeconds += deltaTime;
        }
    }

    @Override
    public Vector2D constrain(double desiredX, double desiredY,
                               int virtualWidth, int virtualHeight, float zoom) {
        double visW = virtualWidth  / (double) zoom;
        double visH = virtualHeight / (double) zoom;

        double clampMaxX = Math.max(minX, maxX - visW);
        double clampMaxY = Math.max(minY, maxY - visH);

        double x = Math.max(minX, Math.min(desiredX, clampMaxX));
        double y = Math.max(minY, Math.min(desiredY, clampMaxY));

        return new Vector2D(x, y);
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public boolean isExpired() {
        if (!active) return true;
        if (durationSeconds <= 0) return false;
        return elapsedSeconds >= durationSeconds;
    }

    @Override
    public int getPriority() { return priority; }
}
