package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Restricción dura: bloquea absolutamente la cámara en los límites dados.
 *
 * ── DIFERENCIA CON WorldBoundsConstraint ─────────────────────────────────
 * WorldBoundsConstraint usa los límites del mundo completo y tiene prioridad
 * máxima (1000). Es el límite definitivo.
 *
 * HardConstraint se usa para restricciones temporales o locales:
 *   - Confinar la cámara a una sala durante un combate
 *   - Bloquear el scroll horizontal en un segmento del nivel
 *   - Restringir el movimiento durante una cinemática
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Bloquear solo el eje horizontal:
 *   HardConstraint hBlock = HardConstraint.horizontal(300, 1200);
 *   constraints.add(hBlock);
 *
 *   // Bloquear ambos ejes:
 *   HardConstraint box = HardConstraint.box(200, 100, 1000, 600);
 *   constraints.add(box);
 */
public final class HardConstraint implements CameraConstraint {

    private final double  minX;
    private final double  minY;
    private final double  maxX;
    private final double  maxY;
    private final boolean constrainX;
    private final boolean constrainY;
    private final int     priority;

    private boolean active  = true;
    private int     ticksElapsed = 0;
    private final int durationTicks;

    private HardConstraint(double minX, double minY, double maxX, double maxY,
                            boolean constrainX, boolean constrainY,
                            int durationTicks, int priority) {
        this.minX          = minX;
        this.minY          = minY;
        this.maxX          = maxX;
        this.maxY          = maxY;
        this.constrainX    = constrainX;
        this.constrainY    = constrainY;
        this.durationTicks = durationTicks;
        this.priority      = priority;
    }

    /** Restringe ambos ejes a la caja dada. */
    public static HardConstraint box(double minX, double minY,
                                      double maxX, double maxY) {
        return new HardConstraint(minX, minY, maxX, maxY, true, true, 0, 600);
    }

    /** Restringe solo el eje horizontal. */
    public static HardConstraint horizontal(double minX, double maxX) {
        return new HardConstraint(minX, Double.MIN_VALUE, maxX, Double.MAX_VALUE,
                                   true, false, 0, 600);
    }

    /** Restringe solo el eje vertical. */
    public static HardConstraint vertical(double minY, double maxY) {
        return new HardConstraint(Double.MIN_VALUE, minY, Double.MAX_VALUE, maxY,
                                   false, true, 0, 600);
    }

    /** Restricción temporal de caja (expira después de durationTicks). */
    public static HardConstraint temporaryBox(double minX, double minY,
                                               double maxX, double maxY,
                                               int durationTicks) {
        return new HardConstraint(minX, minY, maxX, maxY, true, true, durationTicks, 600);
    }

    public void deactivate() { active = false; }

    @Override
    public Vector2D constrain(double desiredX, double desiredY,
                               int virtualWidth, int virtualHeight, float zoom) {
        double visW = virtualWidth  / (double) zoom;
        double visH = virtualHeight / (double) zoom;

        double x = desiredX;
        double y = desiredY;

        if (constrainX) {
            double clampMaxX = Math.max(minX, maxX - visW);
            x = Math.max(minX, Math.min(x, clampMaxX));
        }
        if (constrainY) {
            double clampMaxY = Math.max(minY, maxY - visH);
            y = Math.max(minY, Math.min(y, clampMaxY));
        }

        return new Vector2D(x, y);
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public boolean isExpired() {
        if (!active) return true;
        if (durationTicks <= 0) return false;
        ticksElapsed++;
        return ticksElapsed >= durationTicks;
    }

    @Override
    public int getPriority() { return priority; }
}
