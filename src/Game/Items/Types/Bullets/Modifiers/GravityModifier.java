package Game.Items.Types.Bullets.Modifiers;

import Game.Items.Types.Bullets.Movement.GravityMovement;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileModifier;

/**
 * Modifier que añade o reemplaza la gravedad del movimiento de un proyectil.
 *
 * Compone el movement actual del Blueprint con GravityMovement usando andThen().
 * Si el Blueprint ya tiene gravedad (declarada en el behavior o añadida por
 * otro modifier), se añade en secuencia — el comportamiento resultante es
 * acumulativo, no sustitutivo.
 *
 * Para reemplazar el movimiento completamente, usar MovementModifier.
 *
 * Uso:
 *   ProjectileModifier gravity = new GravityModifier(1.5);
 *   blueprint = gravity.apply(blueprint);
 *
 *   // Como lambda equivalente:
 *   blueprint = bp -> bp.andThenMovement(new GravityMovement(1.5));
 */
public final class GravityModifier implements ProjectileModifier {

    private final double gravity;

    /**
     * @param gravity aceleración gravitacional por frame.
     *                Positivo = cae hacia abajo. Negativo = sube (antigravedad).
     */
    public GravityModifier(double gravity) {
        this.gravity = gravity;
    }

    @Override
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        return blueprint.andThenMovement(new GravityMovement(gravity));
    }
}
