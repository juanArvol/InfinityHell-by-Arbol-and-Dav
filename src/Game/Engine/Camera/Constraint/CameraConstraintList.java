package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Lista priorizada de CameraConstraints.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Gestiona la colección de constraints activas, las aplica en orden de
 * prioridad ascendente (mayor prioridad aplica al final, "gana" sobre las demás)
 * y elimina las expiradas automáticamente.
 *
 * ── PIPELINE DE APLICACIÓN ────────────────────────────────────────────────
 *   1. Ordenar por prioridad ascendente.
 *   2. Pasar (desiredX, desiredY) a la primera constraint.
 *   3. El resultado pasa como input a la siguiente constraint.
 *   4. La última constraint (mayor prioridad) produce el resultado final.
 *
 * Este pipeline en cadena permite que WorldBoundsConstraint (alta prioridad)
 * siempre tenga la última palabra, incluso si SoftConstraint movió la posición.
 */
public final class CameraConstraintList {

    private final List<CameraConstraint> constraints = new ArrayList<>();

    // ── Gestión ───────────────────────────────────────────────────────────

    public void add(CameraConstraint constraint) {
        constraints.add(constraint);
    }

    public void remove(CameraConstraint constraint) {
        constraints.remove(constraint);
    }

    public void clear() {
        constraints.clear();
    }

    public boolean isEmpty() { return constraints.isEmpty(); }

    // ── Aplicación ────────────────────────────────────────────────────────

    /**
     * Aplica todas las constraints activas en cadena de prioridad.
     *
     * @param desiredX      posición X deseada (top-left de la vista)
     * @param desiredY      posición Y deseada
     * @param virtualWidth  ancho del viewport
     * @param virtualHeight alto del viewport
     * @param zoom          zoom actual
     * @return posición final después de aplicar todas las constraints.
     */
    public Vector2D apply(double desiredX, double desiredY,
                          int virtualWidth, int virtualHeight, float zoom) {
        // Eliminar expiradas
        constraints.removeIf(CameraConstraint::isExpired);

        if (constraints.isEmpty()) {
            return new Vector2D(desiredX, desiredY);
        }

        // Ordenar por prioridad ascendente (mayor prioridad aplica al final)
        constraints.sort(Comparator.comparingInt(CameraConstraint::getPriority));

        double x = desiredX;
        double y = desiredY;

        for (CameraConstraint constraint : constraints) {
            if (!constraint.isActive() || constraint.isExpired()) continue;
            Vector2D result = constraint.constrain(x, y, virtualWidth, virtualHeight, zoom);
            if (result != null) {
                x = result.getX();
                y = result.getY();
            }
        }

        return new Vector2D(x, y);
    }

    public int size() { return constraints.size(); }
}
