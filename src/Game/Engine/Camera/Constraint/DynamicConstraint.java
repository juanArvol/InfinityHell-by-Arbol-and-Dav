package Game.Engine.Camera.Constraint;

import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Restricción controlada dinámicamente por una función externa.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *   // Restricción que se activa/desactiva según el estado del juego:
 *   DynamicConstraint combat = DynamicConstraint.of(
 *       (dx, dy, vw, vh, zoom) -> {
 *           if (!player.isInCombat()) return new Vector2D(dx, dy);
 *           
 *           // HRFC: Usar RectangleH.clampPoint() en lugar de Math.max/min anidados
 *           // Más declarativo y menos propenso a errores
 *           Game.Engine.GameMath.Logic2D.RectangleH bossRoomBounds = 
 *               new Game.Engine.GameMath.Logic2D.RectangleH(
 *                   bossRoom.x, bossRoom.y,
 *                   bossRoom.width, bossRoom.height
 *               );
 *           
 *           java.awt.geom.Point2D.Double clamped = bossRoomBounds.clampPoint(dx, dy);
 *           return new Vector2D(clamped.x, clamped.y);
 *       },
 *       () -> !player.isAlive()   // expira cuando el player muere
 *   );
 *
 *   // Ejemplo legacy (aún funcional pero menos expresivo):
 *   // double clampX = Math.max(bossRoom.x, Math.min(dx, bossRoom.x + bossRoom.width));
 *   // double clampY = Math.max(bossRoom.y, Math.min(dy, bossRoom.y + bossRoom.height));
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * DynamicConstraint es la "escotilla de escape" del sistema de constraints:
 * permite cualquier lógica sin necesidad de crear una clase nueva.
 * Para lógica reutilizable entre contextos, crear una clase propia.
 */
public final class DynamicConstraint implements CameraConstraint {

    @FunctionalInterface
    public interface ConstraintFunction {
        Vector2D apply(double desiredX, double desiredY,
                       int virtualWidth, int virtualHeight, float zoom);
    }

    @FunctionalInterface
    public interface ExpiryCheck {
        boolean isExpired();
    }

    private final ConstraintFunction function;
    private final ExpiryCheck        expiryCheck;
    private final int                priority;
    private boolean                  active = true;

    public DynamicConstraint(ConstraintFunction function, ExpiryCheck expiryCheck,
                              int priority) {
        this.function    = function;
        this.expiryCheck = expiryCheck;
        this.priority    = priority;
    }

    public static DynamicConstraint of(ConstraintFunction function) {
        return new DynamicConstraint(function, () -> false, 300);
    }

    public static DynamicConstraint of(ConstraintFunction function,
                                        ExpiryCheck expiryCheck) {
        return new DynamicConstraint(function, expiryCheck, 300);
    }

    public void deactivate() { active = false; }

    @Override
    public Vector2D constrain(double desiredX, double desiredY,
                               int virtualWidth, int virtualHeight, float zoom) {
        return function.apply(desiredX, desiredY, virtualWidth, virtualHeight, zoom);
    }

    @Override
    public boolean isActive() { return active; }

    @Override
    public boolean isExpired() { return !active || expiryCheck.isExpired(); }

    @Override
    public int getPriority() { return priority; }
}
