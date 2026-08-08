package Game.Items.Types.Bullets.Modifiers;

import Game.Engine.Colisions.Filter.CollisionProfile;
import Game.Items.Types.Bullets.ProjectileBlueprint;
import Game.Items.Types.Bullets.ProjectileModifier;

/**
 * Modifier que establece el CollisionProfile de un proyectil.
 *
 * Elimina el invariante manual de:
 *   "crear el Bullet y luego llamar setCollisionProfile()"
 *
 * El profile queda declarado en el Blueprint y BulletFactory lo aplica
 * automáticamente. El caller nunca tiene que recordarlo.
 *
 * Uso:
 *   // Proyectil enemigo:
 *   ProjectileModifier enemyProfile =
 *       new CollisionProfileModifier(CollisionProfile.ENEMY_BULLET);
 *   blueprint = enemyProfile.apply(blueprint);
 *
 *   // O más idiomático, directamente en from():
 *   ProjectileBlueprint bp = ProjectileBlueprint.from(
 *       behavior, speed, damage, CollisionProfile.ENEMY_BULLET);
 */
public final class CollisionProfileModifier implements ProjectileModifier {

    /** Modifier de conveniencia para proyectiles enemigos. */
    public static final CollisionProfileModifier ENEMY =
            new CollisionProfileModifier(CollisionProfile.ENEMY_BULLET);

    /** Modifier de conveniencia para proyectiles del jugador. */
    public static final CollisionProfileModifier PLAYER =
            new CollisionProfileModifier(CollisionProfile.PLAYER_BULLET);

    private final CollisionProfile profile;

    public CollisionProfileModifier(CollisionProfile profile) {
        if (profile == null) throw new IllegalArgumentException(
                "CollisionProfileModifier: profile no puede ser null");
        this.profile = profile;
    }

    @Override
    public ProjectileBlueprint apply(ProjectileBlueprint blueprint) {
        return blueprint.withCollisionProfile(profile);
    }
}
