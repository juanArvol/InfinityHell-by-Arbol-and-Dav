package Game.Engine.Colisions.Filter;

/**
 * Capas de colisión (bitmask).
 * Cada objeto pertenece a UNA capa y colisiona con las que tenga en su máscara.
 *
 *   layer  = "yo soy esto"
 *   mask   = "yo colisiono con esto"
 *
 * Para que A colisione con B se necesita que:
 *   (A.mask & B.layer) != 0   Y   (B.mask & A.layer) != 0
 */
public final class Layer {
    public static final int WORLD  = 1;   // 0001  suelo, paredes, obstáculos
    public static final int PLAYER = 2;   // 0010  jugador
    public static final int ENEMY  = 4;   // 0100  enemigos
    public static final int BULLET = 8;   // 1000  balas
    public static final int ITEM = 16;    // 10000 objetos

    private Layer() {}
}
