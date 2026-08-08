package Game.Items.Types.Bullets.Modifiers;

import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileModifier;

/**
 * Modifier que transforma el daño de un proyectil.
 *
 * Soporta dos modos:
 *   - Aditivo:      damage += bonus
 *   - Multiplicativo: damage *= factor
 *
 * Se pueden encadenar múltiples DamageModifiers en el pipeline.
 *
 * Uso:
 *   // +10 de daño fijo:
 *   ProjectileModifier mod = DamageModifier.add(10);
 *   blueprint = mod.apply(blueprint);
 *
 *   // x1.5 de daño (crítico):
 *   ProjectileModifier crit = DamageModifier.multiply(1.5);
 *   blueprint = crit.apply(blueprint);
 *
 *   // Encadenado:
 *   blueprint = DamageModifier.add(5).andThen(DamageModifier.multiply(1.2)).apply(blueprint);
 */
public final class DamageModifier implements ProjectileModifier {

    private final double addend;
    private final double factor;

    private DamageModifier(double addend, double factor) {
        this.addend = addend;
        this.factor = factor;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Modifier aditivo: damage += bonus.
     * @param bonus bonus de daño (puede ser negativo para reducir)
     */
    public static DamageModifier add(double bonus) {
        return new DamageModifier(bonus, 1.0);
    }

    /**
     * Modifier multiplicativo: damage *= factor.
     * @param factor multiplicador (ej: 1.5 = +50%, 0.5 = -50%)
     */
    public static DamageModifier multiply(double factor) {
        return new DamageModifier(0.0, factor);
    }

    /**
     * Modifier combinado: damage = damage * factor + addend.
     * @param factor  multiplicador aplicado primero
     * @param addend  bonus añadido después del factor
     */
    public static DamageModifier combined(double factor, double addend) {
        return new DamageModifier(addend, factor);
    }

    /**
     * Modifier que fija el daño a un valor absoluto.
     * @param absolute valor de daño fijo
     */
    public static ProjectileModifier set(double absolute) {
        return blueprint -> blueprint.withDamage(absolute);
    }

    @Override
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        double newDamage = blueprint.damage() * factor + addend;
        return blueprint.withDamage(newDamage);
    }
}
