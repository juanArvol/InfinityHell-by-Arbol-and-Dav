package Game.Items.Types.Bullets.Modifiers;

import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileModifier;

/**
 * Modifier que transforma el tiempo de vida de un proyectil.
 *
 * Útil para efectos que alarguen o acorten el alcance del proyectil,
 * por ejemplo un poder que duplica el alcance o un debuff que lo reduce.
 *
 * Uso:
 *   ProjectileModifier longer = LifeTimeModifier.multiply(2.0); // doble alcance
 *   ProjectileModifier short  = LifeTimeModifier.set(5);        // 5 ticks fijos
 *   ProjectileModifier extra  = LifeTimeModifier.add(30);       // +30 ticks
 */
public final class LifeTimeModifier implements ProjectileModifier {

    private final int    addend;
    private final double factor;

    private LifeTimeModifier(int addend, double factor) {
        this.addend = addend;
        this.factor = factor;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    public static LifeTimeModifier add(int extraTicks) {
        return new LifeTimeModifier(extraTicks, 1.0);
    }

    public static LifeTimeModifier multiply(double factor) {
        return new LifeTimeModifier(0, factor);
    }

    public static ProjectileModifier set(int absoluteTicks) {
        return blueprint -> blueprint.withLifeTime(Math.max(1, absoluteTicks));
    }

    @Override
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        int newLife = (int) Math.max(1, blueprint.lifeTime() * factor + addend);
        return blueprint.withLifeTime(newLife);
    }
}
