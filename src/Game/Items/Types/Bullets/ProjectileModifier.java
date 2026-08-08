package Game.Items.Types.Bullets;

/**
 * Transformación de un ProjectileBlueprint antes de que se instancie el Bullet.
 *
 * ── HRFC — Projectile Construction & Transformation Pipeline ─────────────
 *
 * ProjectileModifier opera sobre la DEFINICIÓN del proyectil, no sobre una
 * instancia viva. Es la abstracción que permite modificar cualquier aspecto
 * de un proyectil sin que BulletFactory sepa qué transformación se aplica.
 *
 * ── SEPARACIÓN vs ProjectileTransformer ──────────────────────────────────
 *
 *   ProjectileModifier   = transforma la definición (pre-build).
 *                          Sin efectos secundarios en el mundo.
 *                          Puro — solo lee y produce Blueprints.
 *
 *   ProjectileTransformer= transforma una instancia viva (post-build).
 *                          Puede tener efectos en posición, colisión, etc.
 *
 * ── USO ───────────────────────────────────────────────────────────────────
 *
 *   // Modifier inline:
 *   ProjectileModifier doubleSpeed = bp -> bp.withSpeed(bp.speed() * 2);
 *
 *   // Modifier como clase:
 *   ProjectileModifier gravity = new Modifiers.GravityModifier(1.5);
 *
 *   // Pipeline:
 *   ProjectileBlueprint bp = ProjectileBlueprint.from(behavior, speed, damage);
 *   bp = gravity.apply(bp);
 *   bp = damageBoost.apply(bp);
 *   Bullet bullet = BulletFactory.build(bp, position, direction);
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 *
 * Los modifiers se componen con andThen(), igual que ProjectileMovement.
 * El orden importa: se aplican en secuencia izquierda → derecha.
 *
 *   ProjectileModifier combined = mod1.andThen(mod2).andThen(mod3);
 *
 * ── @FunctionalInterface ─────────────────────────────────────────────────
 *
 * ProjectileModifier es una interfaz funcional para permitir lambdas y
 * method references. Preferir lambdas para modifiers simples.
 */
@FunctionalInterface
public interface ProjectileModifier {

    /**
     * Transforma un Blueprint y retorna el resultado.
     *
     * Debe ser puro: no modificar el Blueprint original, no tener
     * efectos secundarios en el mundo. Usar los withers del Blueprint.
     *
     * @param blueprint definición actual del proyectil
     * @return Blueprint transformado (puede ser el mismo si no hay cambios)
     */
    ProjectileBlueprint apply(ProjectileBlueprint blueprint);

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Compone este modifier con otro, aplicándolos en secuencia.
     * Equivalente a: after.apply(this.apply(blueprint))
     *
     * @param after modifier a aplicar después de éste
     * @return modifier combinado
     */
    default ProjectileModifier andThen(ProjectileModifier after) {
        return blueprint -> after.apply(this.apply(blueprint));
    }

    // ── Identity ──────────────────────────────────────────────────────────

    /**
     * Modifier que no hace nada — identidad de la composición.
     * Útil como valor por defecto cuando no hay modifiers activos.
     */
    ProjectileModifier IDENTITY = blueprint -> blueprint;
}
