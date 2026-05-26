package Game.Engine.Filter;

/**
 * Perfil de colisión: define a qué capa pertenece un objeto y con qué capas choca.
 *
 * Reemplaza a CollisionProfileMask + CollisionProfiles con una sola clase clara.
 * Los perfiles predefinidos están como constantes estáticas aquí mismo.
 *
 * Uso:
 *   collider.setProfile(CollisionProfile.PLAYER);
 *   collider.setProfile(CollisionProfile.BULLET);
 */
public final class CollisionProfile {

    public final int layer;
    public final int mask;

    public CollisionProfile(int layer, int mask) {
        this.layer = layer;
        this.mask  = mask;
    }

    // ── Perfiles predefinidos ──────────────────────────────────────────────

    /** Suelo, paredes, obstáculos. Choca con todo. */
    public static final CollisionProfile WORLD = new CollisionProfile(
            Layer.WORLD,
            Layer.PLAYER | Layer.ENEMY | Layer.BULLET
    );

    /** El jugador. Choca con mundo, balas y enemigos. */
    public static final CollisionProfile PLAYER = new CollisionProfile(
            Layer.PLAYER,
            Layer.WORLD | Layer.ENEMY | Layer.BULLET
    );

    /** Enemigo normal. Choca con mundo y jugador. */
    public static final CollisionProfile ENEMY = new CollisionProfile(
            Layer.ENEMY,
            Layer.WORLD | Layer.PLAYER | Layer.BULLET
    );

    /** Bala del jugador. Choca con mundo y enemigos. NO con el jugador ni otras balas. */
    public static final CollisionProfile BULLET = new CollisionProfile(
            Layer.BULLET,
            Layer.WORLD | Layer.ENEMY | Layer.PLAYER
    );

    /** Bala del enemigo. Choca con mundo y jugador. */
    public static final CollisionProfile ENEMY_BULLET = new CollisionProfile(
            Layer.BULLET,
            Layer.WORLD | Layer.PLAYER
    );

    public static final CollisionProfile WORLD_ITEM = new CollisionProfile(
            Layer.ITEM,
            Layer.WORLD | Layer.PLAYER
    );
    // ── Utilidad ──────────────────────────────────────────────────────────

    /** @return true si este perfil puede chocar con el otro según las masks. */
    public boolean canCollideWith(CollisionProfile other) {
        return (this.mask & other.layer) != 0
            && (other.mask & this.layer) != 0;
    }
}
