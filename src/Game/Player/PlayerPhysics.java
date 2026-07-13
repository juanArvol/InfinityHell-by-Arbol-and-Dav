package Game.Player;

import Game.Engine.GameMath.Physics.MovementContext;
import Game.Engine.GameMath.Physics.Types.Physics2D;

/**
 * Física del jugador.
 *
 * Implementación específica del Game que extiende Physics2D del Engine.
 * Vive en Game.Player porque es una regla del juego (parámetros de
 * movimiento del jugador), no infraestructura reutilizable.
 *
 * MIGRADO DESDE: Game.Engine.GameMath.Physics.Implementation.PlayerPhysics
 * RAZÓN: PlayerPhysics contiene valores de gameplay específicos del jugador
 * (RUN_BOOST, speedMaxPiso, slide). No es infraestructura genérica del Engine.
 *
 * ── Capas de modificador ─────────────────────────────────────────────────
 *
 *   mAccel = baseAccel
 *          × entityModifier      → (1/mass) × runBoost
 *          × surfaceModifier     → friction × accelScale  (en Physics2D)
 *          × statusModifier      → herido, stun, buff     (externo via statusStack)
 *          × environmentModifier → viento, agua           (externo via environmentStack)
 *          × airControlModifier  → airControl del surface (en Physics2D)
 */
public class PlayerPhysics extends Physics2D {

    /** Multiplicador de aceleración cuando el jugador corre. */
    private static final double RUN_BOOST = 1.6;

    public PlayerPhysics(double gravity) {
        super(gravity);
        mass    = 1.0;
        aGround = 2.5;
        aAir    = 1.07;
    }

    /**
     * Capa de entidad del jugador: masa + boost de carrera.
     */
    @Override
    protected double computeEntityModifier(MovementContext ctx) {
        double base = 1.0 / mass;
        return ctx.running() ? base * RUN_BOOST : base;
    }

    @Override
    public void moveX(double inputX, boolean onGround, boolean running) {
        speedMaxPiso = running ? 135 : 70;
        speedMaxAir  = running ? 14.5 : 10;

        // IMPORTANTE: asignar slide ANTES de super.moveX()
        slide = onGround ? 0.9 : 0.74;

        super.moveX(inputX, onGround, running);
    }
}
