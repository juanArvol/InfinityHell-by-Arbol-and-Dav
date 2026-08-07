package Game.World.Transition;

import Game.Engine.Entity.Components.Collisions.ColliderComponent;
import Game.Engine.GameObjects;
import Game.World.Core.World;
import java.awt.Rectangle;
import java.util.List;

/**
 * Valida si la posición destino de una transición es válida.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * TransitionValidator verifica que la entidad que transita no vaya a
 * quedar embebida dentro de otro objeto sólido en el mundo destino.
 *
 * ── CÓMO FUNCIONA ─────────────────────────────────────────────────────────
 * Para cada TransitionRequest, el validator:
 *   1. Obtiene el ColliderComponent del subject (si tiene uno).
 *   2. Calcula el AABB que tendría en la posición destino.
 *   3. Verifica si ese AABB intersecta con algún collider sólido del mundo destino.
 *
 * Si no hay intersección: la transición es válida.
 * Si hay intersección: la transición necesita resolución (TransitionResolver).
 *
 * ── OBJETOS SIN COLLIDER ──────────────────────────────────────────────────
 * Si el subject no tiene ColliderComponent, la transición siempre se considera
 * válida. No se puede validar lo que no ocupa espacio físico.
 *
 * ── EXTENSIBILIDAD ────────────────────────────────────────────────────────
 * Para validaciones adicionales (zona prohibida, trigger de misión, etc.)
 * implementar la interfaz ValidationRule e inyectarla.
 */
public final class TransitionValidator {

    /**
     * Regla adicional de validación inyectable.
     * Permite añadir restricciones sin modificar este validador.
     */
    @FunctionalInterface
    public interface ValidationRule {
        /**
         * @return true si la posición es válida según esta regla.
         */
        boolean isValid(TransitionRequest request, World targetWorld);
    }

    private final List<ValidationRule> extraRules;

    public TransitionValidator() {
        this.extraRules = List.of();
    }

    public TransitionValidator(List<ValidationRule> extraRules) {
        this.extraRules = extraRules != null ? extraRules : List.of();
    }

    // ── Validación ────────────────────────────────────────────────────────

    /**
     * Resultado de la validación.
     *
     * @param valid   true si la posición destino es libre.
     * @param reason  descripción del problema si no es válida.
     */
    public record ValidationResult(boolean valid, String reason) {
        public static ValidationResult ok()           { return new ValidationResult(true,  ""); }
        public static ValidationResult fail(String r) { return new ValidationResult(false, r); }
    }

    /**
     * Valida si el subject puede transitar a la posición indicada en el mundo destino.
     *
     * @param request     el request de transición a validar.
     * @param targetWorld el mundo destino donde el subject aparecería.
     * @return resultado de la validación.
     */
    public ValidationResult validate(TransitionRequest request, World targetWorld) {
        GameObjects subject = request.getSubject();

        // Si no tiene collider físico, no hay nada que validar
        ColliderComponent subjectCol = subject.getComponent(ColliderComponent.class);
        if (subjectCol == null) return ValidationResult.ok();

        // Calcular AABB del subject en la posición destino
        Rectangle subjectBounds = calculateBoundsAt(subjectCol, request.getTargetPosition());

        // Verificar intersección con colisionadores sólidos del mundo destino
        for (GameObjects other : targetWorld.getDynamicEntityRegistry().getAll()) {
            if (other == subject) continue;

            ColliderComponent otherCol = other.getComponent(ColliderComponent.class);
            if (otherCol == null || otherCol.isTrigger()) continue;
            if (!subjectCol.canCollideWith(otherCol)) continue;

            if (subjectBounds.intersects(otherCol.getBounds())) {
                return ValidationResult.fail(
                    "Posición destino bloqueada por " + other.getClass().getSimpleName() +
                    " en " + other.getTransform().getPosition()
                );
            }
        }

        // Reglas adicionales inyectadas
        for (ValidationRule rule : extraRules) {
            if (!rule.isValid(request, targetWorld)) {
                return ValidationResult.fail("Regla adicional rechazó la posición.");
            }
        }

        return ValidationResult.ok();
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /**
     * Calcula el AABB del collider del subject si estuviera en la posición dada.
     * Usa el tamaño actual del collider, solo desplaza su origen.
     */
    private Rectangle calculateBoundsAt(ColliderComponent col,
                                         Game.Engine.GameMath.Logic2D.Vector2D position) {
        Rectangle current = col.getBounds();
        return new Rectangle(
            (int) position.getX(),
            (int) position.getY(),
            current.width,
            current.height
        );
    }
}
