package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Restricción de posición de la cámara.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Una CameraConstraint toma la posición calculada por el CameraController
 * (y modificada por los CameraModifiers) y la ajusta para cumplir
 * alguna restricción.
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ───────────────────────────────────────
 * CameraController → calcula HACIA DÓNDE quiere ir la cámara
 * CameraModifier   → altera CÓMO se ve la imagen
 * CameraConstraint → limita DÓNDE puede ir la cámara
 *
 * ── TIPOS DE RESTRICCIÓN ──────────────────────────────────────────────────
 *   Hard constraint → la cámara no puede pasar del límite (bloqueo absoluto)
 *   Soft constraint → la cámara se ralentiza antes del límite (resistencia)
 *
 * ── ORDEN DE APLICACIÓN ───────────────────────────────────────────────────
 * Las constraints se aplican en orden de prioridad descendente.
 * Una constraint de mayor prioridad puede anular el resultado de una menor.
 *
 * ── IMPLEMENTACIONES ──────────────────────────────────────────────────────
 *   WorldBoundsConstraint   → no salir de los límites del mundo
 *   RegionConstraint        → limitar a una región rectangular
 *   MaxFollowConstraint     → no alejarse más de N píxeles del target
 *   DynamicConstraint       → restricción controlada por script/evento
 *   SoftConstraint          → resistencia antes del límite
 *   HardConstraint          → bloqueo absoluto
 */
public interface CameraConstraint {

    /**
     * Aplica la restricción a la posición calculada.
     *
     * @param desiredX    posición X deseada (top-left de la vista)
     * @param desiredY    posición Y deseada
     * @param virtualWidth  ancho del viewport virtual
     * @param virtualHeight alto del viewport virtual
     * @param zoom         zoom actual de la cámara
     * @return posición ajustada para cumplir la restricción.
     *         Si no hay restricción activa, retornar new Vector2D(desiredX, desiredY).
     */
    Vector2D constrain(double desiredX, double desiredY,
                       int virtualWidth, int virtualHeight, float zoom);

    /**
     * True si esta constraint está activa.
     * Las constraints inactivas se ignoran pero no se eliminan.
     */
    default boolean isActive() { return true; }

    /**
     * True si esta constraint ha expirado y debe ser eliminada.
     */
    default boolean isExpired() { return false; }

    /**
     * Prioridad de aplicación. Mayor valor = se aplica después (sobre las demás).
     * WorldBoundsConstraint debe tener la prioridad más alta para ser el
     * límite definitivo.
     */
    default int getPriority() { return 0; }
}
