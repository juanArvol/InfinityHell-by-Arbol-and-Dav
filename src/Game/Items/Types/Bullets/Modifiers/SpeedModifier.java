package Game.Items.Types.Bullets.Modifiers;

import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileModifier;

/**
 * Modifier que transforma la velocidad escalar de un proyectil.
 *
 * Modifica el campo speed del Blueprint. La velocidad final X/Y se calcula
 * en BulletFactory.build() al multiplicar speed por la dirección normalizada.
 *
 * Uso:
 *   ProjectileModifier fast    = SpeedModifier.multiply(1.5);  // +50% velocidad
 *   ProjectileModifier slow    = SpeedModifier.multiply(0.5);  // -50% velocidad
 *   ProjectileModifier fixed   = SpeedModifier.set(12.0);      // velocidad fija
 *   ProjectileModifier extra   = SpeedModifier.add(3.0);       // +3 u/frame
 */
public final class SpeedModifier implements ProjectileModifier {

    private final double addend;
    private final double factor;

    private SpeedModifier(double addend, double factor) {
        this.addend = addend;
        this.factor = factor;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    public static SpeedModifier add(double bonus) {
        return new SpeedModifier(bonus, 1.0);
    }

    public static SpeedModifier multiply(double factor) {
        return new SpeedModifier(0.0, factor);
    }

    public static ProjectileModifier set(double absolute) {
        return blueprint -> blueprint.withSpeed(Math.max(0, absolute));
    }

    @Override
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        double newSpeed = Math.max(0, blueprint.speed() * factor + addend);
        return blueprint.withSpeed(newSpeed);
    }
}
