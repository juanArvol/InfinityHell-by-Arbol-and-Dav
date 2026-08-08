package Game.Items.Types.Bullets;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline composable de ProjectileModifiers.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * Permite acumular modifiers y aplicarlos en orden sobre un Blueprint.
 * Reusable: el mismo pipeline puede aplicarse a múltiples Blueprints.
 *
 * ── DISEÑO ────────────────────────────────────────────────────────────────
 *
 * El pipeline es mutable durante su construcción (add/remove) pero su
 * operación apply() es pura — no modifica el Blueprint original.
 *
 * Para pipelines inmutables, usar la composición funcional de ProjectileModifier:
 *   ProjectileModifier combined = mod1.andThen(mod2).andThen(mod3);
 *
 * ProjectileModifierPipeline es preferible cuando los modifiers se acumulan
 * dinámicamente (amuletos del jugador, efectos de status, mods de arma).
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   ProjectileModifierPipeline pipeline = new ProjectileModifierPipeline();
 *
 *   // Añadir modifiers (orden de aplicación = orden de adición):
 *   pipeline.add(new GravityModifier(1.5));
 *   pipeline.add(DamageModifier.add(10));
 *   pipeline.add(CollisionProfileModifier.ENEMY);
 *
 *   // Aplicar:
 *   ProjectileBlueprint bp = ProjectileBlueprint.from(behavior, speed, damage);
 *   bp = pipeline.apply(bp);
 *   Bullet bullet = BulletFactory.build(bp, position, direction);
 *
 * ── ORDEN ─────────────────────────────────────────────────────────────────
 *
 * Los modifiers se aplican en el orden en que fueron añadidos.
 * El orden importa: un SpeedModifier.multiply(2) seguido de un
 * DamageModifier produce el doble de speed en el resultado.
 */
public final class ProjectileModifierPipeline {

    private final List<ProjectileModifier> modifiers = new ArrayList<>();

    // ── Construcción ──────────────────────────────────────────────────────

    /**
     * Añade un modifier al final del pipeline.
     * Fluent API para encadenamiento.
     */
    public ProjectileModifierPipeline add(ProjectileModifier modifier) {
        if (modifier != null && modifier != ProjectileModifier.IDENTITY) {
            modifiers.add(modifier);
        }
        return this;
    }

    /**
     * Añade todos los modifiers de otro pipeline al final de éste.
     */
    public ProjectileModifierPipeline addAll(ProjectileModifierPipeline other) {
        modifiers.addAll(other.modifiers);
        return this;
    }

    /**
     * Elimina todos los modifiers. Útil para reutilizar el pipeline entre frames.
     */
    public ProjectileModifierPipeline clear() {
        modifiers.clear();
        return this;
    }

    // ── Aplicación ────────────────────────────────────────────────────────

    /**
     * Aplica todos los modifiers del pipeline en orden sobre el Blueprint.
     *
     * Si el pipeline está vacío, retorna el Blueprint sin cambios.
     * Puro: no modifica el Blueprint original, trabaja con withers.
     *
     * @param blueprint Blueprint a transformar
     * @return Blueprint resultante después de aplicar todos los modifiers
     */
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        ProjectileBlueprint result = blueprint;
        for (ProjectileModifier modifier : modifiers) {
            result = modifier.apply(result);
        }
        return result;
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** @return true si no hay modifiers activos. */
    public boolean isEmpty() {
        return modifiers.isEmpty();
    }

    /** @return número de modifiers en el pipeline. */
    public int size() {
        return modifiers.size();
    }

    /**
     * Retorna una vista de un solo modifier que aplica todo el pipeline.
     * Útil para pasar el pipeline donde se espera un ProjectileModifier.
     */
    public ProjectileModifier asModifier() {
        if (modifiers.isEmpty()) return ProjectileModifier.IDENTITY;
        ProjectileModifier combined = modifiers.get(0);
        for (int i = 1; i < modifiers.size(); i++) {
            combined = combined.andThen(modifiers.get(i));
        }
        return combined;
    }

    // ── Factory ───────────────────────────────────────────────────────────

    /**
     * Crea un pipeline con los modifiers dados en orden.
     */
    public static ProjectileModifierPipeline of(ProjectileModifier... modifiers) {
        ProjectileModifierPipeline pipeline = new ProjectileModifierPipeline();
        for (ProjectileModifier mod : modifiers) {
            pipeline.add(mod);
        }
        return pipeline;
    }
}
