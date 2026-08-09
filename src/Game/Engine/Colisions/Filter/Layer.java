package Game.Engine.Colisions.Filter;

/**
 * Capas de colisión base del Engine (bitmask).
 *
 * ── HRFC-014 — GAP-6: Extensibilidad sin modificar el Engine ───────────
 *
 * Cada objeto pertenece a UNA capa (layer) y colisiona con las capas
 * que tenga en su máscara (mask):
 *
 *   layer = "yo soy esto"
 *   mask  = "yo colisiono con esto"
 *
 * Para que A colisione con B se necesita:
 *   (A.mask & B.layer) != 0   Y   (B.mask & A.layer) != 0
 *
 * ── Capas base del Engine ─────────────────────────────────────────────────
 *
 * Las capas 1–16 (bits 0–4) están reservadas para el Engine.
 * Los módulos del Game pueden definir capas propias en bits superiores.
 *
 * ── Extensión desde el Game sin modificar este archivo ───────────────────
 *
 * Para añadir capas de gameplay sin tocar el Engine, declarar constantes
 * en un catálogo propio usando bits a partir de GAME_LAYER_BASE:
 *
 *   public final class GameLayers {
 *       // Empezar desde el bit 5 (Layer.GAME_LAYER_BASE)
 *       public static final int TRAP        = Layer.GAME_LAYER_BASE;       // 32
 *       public static final int NPC         = Layer.GAME_LAYER_BASE << 1;  // 64
 *       public static final int PROJECTILE_BOSS = Layer.GAME_LAYER_BASE << 2; // 128
 *   }
 *
 * Luego definir perfiles:
 *
 *   public static final CollisionProfile TRAP = new CollisionProfile(
 *       GameLayers.TRAP,
 *       Layer.PLAYER | Layer.ENEMY
 *   );
 *
 * Ningún archivo del Engine necesita modificarse.
 */
public final class Layer {

    // ── Capas base del Engine (bits 0–4) ─────────────────────────────────

    /** Suelo, paredes, obstáculos estáticos del mundo. */
    public static final int WORLD  = 1;   // bit 0

    /** El jugador. */
    public static final int PLAYER = 2;   // bit 1

    /** Enemigos. */
    public static final int ENEMY  = 4;   // bit 2

    /** Proyectiles (balas, flechas, orbes). */
    public static final int BULLET = 8;   // bit 3

    /** Objetos recogibles (ítems, monedas, loot). */
    public static final int ITEM         = 16;  // bit 4

    /**
     * Objetos sólidos dinámicos del mundo (cajas, barriles, plataformas móviles).
     *
     * ── HRFC — World Objects extensibles ─────────────────────────────────
     *
     * DISTINCIÓN respecto a WORLD:
     *   WORLD         → terreno estático (BlockWorld, suelo, paredes fijas).
     *                   No se mueve. Nunca tiene Physics2DComponent.
     *                   CollisionsSystem lo trata como superficie inerte.
     *
     *   WORLD_DYNAMIC → objeto del mundo que puede moverse o ser empujado.
     *                   Puede tener Physics2DComponent y PushableComponent.
     *                   CollisionsSystem lo mueve en FASE 1 si tiene física.
     *                   Puede además proveer SurfaceComponent.
     *
     * Esta distinción permite que:
     *   - Los sistemas de física separen terreno (inerte) de objeto dinámico (activo).
     *   - CollisionProfile filtre correctamente: un Crate choca con WORLD y PLAYER
     *     pero su layer WORLD_DYNAMIC no lo confunde con terreno estático.
     *   - Futuras mecánicas (plataformas móviles, cajas apilables) usen WORLD_DYNAMIC
     *     sin modificar el comportamiento de WORLD estático.
     *
     * Uso: WorldObject con Physics2DComponent y/o PushableComponent.
     */
    public static final int WORLD_DYNAMIC = 32;  // bit 5

    // ── Base para capas de gameplay definidas fuera del Engine ────────────

    /**
     * Primer bit disponible para capas definidas por el Game.
     *
     * Los módulos del Game definen sus capas a partir de este valor,
     * garantizando que no colisionan con las capas base del Engine.
     *
     * NOTA: WORLD_DYNAMIC usa el bit 5 (32). Las capas de gameplay
     * deben empezar en el bit 6 (64) para no colisionar:
     *
     *   int TRAP   = Layer.GAME_LAYER_BASE;       // 64
     *   int NPC    = Layer.GAME_LAYER_BASE << 1;  // 128
     *   int SHIELD = Layer.GAME_LAYER_BASE << 2;  // 256
     *
     * El bitmask de colisión tiene 32 bits, por lo que hay 26 bits disponibles
     * para capas de gameplay (bits 6–31).
     */
    public static final int GAME_LAYER_BASE = 64; // bit 6

    private Layer() {}
}
