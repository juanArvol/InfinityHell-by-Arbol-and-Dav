package Game.Items.Types.Bullets.Modifiers;

import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileModifier;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Modifier que reemplaza o compone el movimiento de un proyectil.
 *
 * Dos modos:
 *   - replace:  el nuevo movement reemplaza completamente el existente.
 *   - compose:  el nuevo movement se añade al final (andThen).
 *
 * Preferir compose para efectos adicionales (gravedad, oscilación).
 * Preferir replace cuando el movimiento base no es relevante (homing total).
 *
 * Uso:
 *   // Añadir oscilación sinusoidal a cualquier proyectil:
 *   ProjectileModifier wave = MovementModifier.compose(new SinusoidalMovement(3, 0.2));
 *   blueprint = wave.apply(blueprint);
 *
 *   // Reemplazar movimiento completamente con homing:
 *   ProjectileModifier homing = MovementModifier.replace(new HomingMovement(target, 90, 8));
 *   blueprint = homing.apply(blueprint);
 */
public final class MovementModifier implements ProjectileModifier {

    private final ProjectileMovement newMovement;
    private final boolean            replace;

    private MovementModifier(ProjectileMovement newMovement, boolean replace) {
        this.newMovement = newMovement;
        this.replace     = replace;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Compone el nuevo movement al final del existente (andThen).
     * El movement original del Blueprint se mantiene activo.
     */
    public static MovementModifier compose(ProjectileMovement extra) {
        return new MovementModifier(extra, false);
    }

    /**
     * Reemplaza completamente el movement del Blueprint.
     * El movement original se descarta.
     */
    public static MovementModifier replace(ProjectileMovement replacement) {
        return new MovementModifier(replacement, true);
    }

    @Override
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        if (replace) {
            return blueprint.withMovement(newMovement);
        } else {
            return blueprint.andThenMovement(newMovement);
        }
    }
}
