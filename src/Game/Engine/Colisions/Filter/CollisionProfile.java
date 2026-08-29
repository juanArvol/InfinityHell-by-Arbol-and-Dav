package Game.Engine.Colisions.Filter;

/**
 * Perfil de colisión: define a qué capa pertenece un objeto y con qué capas choca.
 *
 * ── HRFC — Projectile System Refactor ────────────────────────────────────
 *
 * PROBLEMA ANTERIOR:
 *   CollisionProfile.BULLET tenía máscara Layer.WORLD | Layer.ENEMY | Layer.PLAYER.
 *   Esto significaba que las balas del jugador podían colisionar con el propio
 *   jugador — friendly fire involuntario. No existía distinción entre proyectiles
 *   aliados y enemigos a nivel de layer.
 *
 * SOLUCIÓN:
 *   Se añaden perfiles específicos por origen del proyectil:
 *
 *     PLAYER_BULLET — proyectil disparado por el jugador o aliados.
 *                     Choca con WORLD y ENEMY. NO choca con PLAYER.
 *
 *     ENEMY_BULLET  — proyectil disparado por enemigos.
 *                     Choca con WORLD y PLAYER. NO choca con ENEMY.
 *
 *   BULLET (legacy) se mantiene por compatibilidad pero queda deprecated.
 *   Todo código nuevo debe usar PLAYER_BULLET o ENEMY_BULLET.
 *
 * ── HRFC — Deep Optimization: CollisionMatrix Integration ─────────────────
 *
 * EXTENSIÓN:
 *   Cada CollisionProfile ahora tiene una categoría de CollisionMatrix.
 *   Esto permite filtrado rápido de pares de colisión antes del broadphase.
 *
 * ── EXTENSIÓN FUTURA ──────────────────────────────────────────────────────
 *   Si se añaden facciones adicionales (aliados NPC, torretas del jugador, etc.)
 *   añadir nuevos perfiles aquí. No modificar los existentes.
 *
 *   Ejemplo futuro:
 *     ALLY_BULLET — proyectil de NPC aliado.
 *                   Choca con WORLD y ENEMY. NO choca con PLAYER ni ALLY.
 */
public final class CollisionProfile {

    public final int layer;
    public final int mask;
    
    // HRFC — Deep Optimization: CollisionMatrix category
    public final CollisionMatrix.Category category;

    public CollisionProfile(int layer, int mask, CollisionMatrix.Category category) {
        this.layer = layer;
        this.mask  = mask;
        this.category = category;
    }
    
    // Constructor legacy sin categoría (default a TRIGGER)
    @Deprecated
    public CollisionProfile(int layer, int mask) {
        this(layer, mask, CollisionMatrix.Category.TRIGGER);
    }

    // ── Perfiles predefinidos ──────────────────────────────────────────────

    /** Suelo, paredes, obstáculos. Choca con todo. */
    public static final CollisionProfile WORLD = new CollisionProfile(
            Layer.WORLD,
            Layer.PLAYER | Layer.ENEMY | Layer.BULLET,
            CollisionMatrix.Category.SOLID
    );

    /** El jugador. Choca con mundo, balas enemigas y enemigos. */
    public static final CollisionProfile PLAYER = new CollisionProfile(
            Layer.PLAYER,
            Layer.WORLD | Layer.ENEMY | Layer.BULLET,
            CollisionMatrix.Category.PLAYER
    );

    /** Enemigo normal. Choca con mundo, jugador y balas del jugador. */
    public static final CollisionProfile ENEMY = new CollisionProfile(
            Layer.ENEMY,
            Layer.WORLD | Layer.PLAYER | Layer.BULLET,
            CollisionMatrix.Category.ENEMY
    );

    /**
     * Proyectil del jugador o aliado.
     * Choca con WORLD y ENEMY. NO choca con el propio PLAYER.
     *
     * Usar para: balas del jugador, invocaciones aliadas, torretas del jugador.
     */
    public static final CollisionProfile PLAYER_BULLET = new CollisionProfile(
            Layer.BULLET,
            Layer.WORLD | Layer.ENEMY,
            CollisionMatrix.Category.BULLET
    );

    /**
     * Proyectil de enemigo.
     * Choca con WORLD y PLAYER. NO choca con otros ENEMY.
     *
     * Usar para: disparos de enemigos, patrones de boss, trampas hostiles.
     */
    public static final CollisionProfile ENEMY_BULLET = new CollisionProfile(
            Layer.BULLET,
            Layer.WORLD | Layer.PLAYER,
            CollisionMatrix.Category.BULLET
    );

    /**
     * @deprecated Usar {@link #PLAYER_BULLET} o {@link #ENEMY_BULLET} según el origen.
     *             Este perfil choca con PLAYER, causando friendly fire involuntario.
     *             Mantenido solo para compatibilidad con código no migrado.
     */
    @Deprecated
    public static final CollisionProfile BULLET = new CollisionProfile(
            Layer.BULLET,
            Layer.WORLD | Layer.ENEMY | Layer.PLAYER,
            CollisionMatrix.Category.BULLET
    );

    public static final CollisionProfile WORLD_ITEM = new CollisionProfile(
            Layer.ITEM,
            Layer.WORLD | Layer.PLAYER,
            CollisionMatrix.Category.ITEM
    );

    /**
     * Objeto sólido dinámico del mundo (caja, barril, plataforma móvil, etc.).
     *
     * ── HRFC — World Objects extensibles ─────────────────────────────────
     *
     * DISTINCIÓN respecto a WORLD:
     *   WORLD         → terreno estático inerte (BlockWorld, suelo, paredes).
     *                   CollisionsSystem no lo mueve en FASE 1.
     *
     *   WORLD_DYNAMIC → objeto del mundo que puede tener Physics2DComponent
     *                   y ser movido/empujado. CollisionsSystem lo incluye
     *                   en FASE 1 si tiene física. Puede además proveer
     *                   SurfaceComponent para propiedades de superficie.
     *
     * Choca con: WORLD (terreno), PLAYER, ENEMY, BULLET y otros WORLD_DYNAMIC.
     *
     * Uso canónico:
     *   WorldObject crate = new WorldObject(pos, tex, w, h, CollisionProfile.WORLD_DYNAMIC);
     *   crate.addComponent(new Physics2DComponent(cratePhysics));
     *   crate.addComponent(new PushableComponent(0.8));
     */
    public static final CollisionProfile WORLD_DYNAMIC = new CollisionProfile(
            Layer.WORLD_DYNAMIC,
            Layer.WORLD | Layer.PLAYER | Layer.ENEMY | Layer.BULLET | Layer.WORLD_DYNAMIC,
            CollisionMatrix.Category.SOLID
    );

    // ── Utilidad ──────────────────────────────────────────────────────────

    /** @return true si este perfil puede chocar con el otro según las masks. */
    public boolean canCollideWith(CollisionProfile other) {
        return (this.mask & other.layer) != 0
            && (other.mask & this.layer) != 0;
    }
}
