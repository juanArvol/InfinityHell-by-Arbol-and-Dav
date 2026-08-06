package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.function.Supplier;

/**
 * Restricción que impide que la cámara se aleje más de maxDistance píxeles
 * de un punto de referencia (típicamente el player o el target activo).
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // La cámara nunca puede estar a más de 300px del player:
 *   MaxFollowConstraint follow = new MaxFollowConstraint(
 *       () -> player.getTransform().getPosition(), 300
 *   );
 *   constraints.add(follow);
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 * Si la posición deseada de la cámara está más allá de maxDistance
 * del punto de referencia (medido desde el centro del viewport),
 * la posición se ajusta para mantenerse dentro del círculo de maxDistance.
 */
public final class MaxFollowConstraint implements CameraConstraint {

    private final Supplier<Vector2D> referenceSupplier;
    private final double             maxDistance;
    private final int                priority;

    /**
     * @param referenceSupplier proveedor de la posición de referencia
     * @param maxDistance       distancia máxima permitida en píxeles
     */
    public MaxFollowConstraint(Supplier<Vector2D> referenceSupplier, double maxDistance) {
        this(referenceSupplier, maxDistance, 400);
    }

    public MaxFollowConstraint(Supplier<Vector2D> referenceSupplier,
                                double maxDistance, int priority) {
        this.referenceSupplier = referenceSupplier;
        this.maxDistance       = maxDistance;
        this.priority          = priority;
    }

    @Override
    public Vector2D constrain(double desiredX, double desiredY,
                               int virtualWidth, int virtualHeight, float zoom) {
        Vector2D reference = referenceSupplier.get();
        if (reference == null) return new Vector2D(desiredX, desiredY);

        // Centro del viewport en coords de mundo
        double visW = virtualWidth  / (double) zoom;
        double visH = virtualHeight / (double) zoom;
        double centerX = desiredX + visW / 2.0;
        double centerY = desiredY + visH / 2.0;

        // Distancia entre el centro del viewport y el punto de referencia
        double dx = centerX - reference.getX();
        double dy = centerY - reference.getY();
        double dist = Math.hypot(dx, dy);

        if (dist <= maxDistance) {
            // Dentro del límite: sin cambio
            return new Vector2D(desiredX, desiredY);
        }

        // Fuera del límite: empujar el centro del viewport hacia el punto de referencia
        double scale = maxDistance / dist;
        double newCenterX = reference.getX() + dx * scale;
        double newCenterY = reference.getY() + dy * scale;

        return new Vector2D(newCenterX - visW / 2.0, newCenterY - visH / 2.0);
    }

    @Override
    public int getPriority() { return priority; }
}
