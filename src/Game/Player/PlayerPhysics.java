package Game.Player;

import Game.Engine.Physics.KineticPhysics.MovementContext;
import Game.Engine.Physics.KineticPhysics.Types.Physics2D;

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

    // ── Constantes de movimiento ──────────────────────────────────────────
    // Centralizar estos valores evita que el tuning se disperse en múltiples
    // expresiones literales dentro de moveX(). Cambiar el comportamiento del
    // jugador requiere editar solo esta sección.

    /** Velocidad máxima horizontal caminando (píxeles virtuales/frame). */
    private static final double WALK_SPEED_GROUND = 70;
    /** Velocidad máxima horizontal corriendo (píxeles virtuales/frame). */
    private static final double RUN_SPEED_GROUND  = 135;

    /** Velocidad máxima horizontal en el aire caminando. */
    private static final double WALK_SPEED_AIR    = 10;
    /** Velocidad máxima horizontal en el aire corriendo. */
    private static final double RUN_SPEED_AIR     = 14.5;

    /** Factor de deslizamiento en el suelo (0=stop inmediato, 1=sin fricción). */
    private static final double SLIDE_GROUND      = 0.9;
    /** Factor de deslizamiento en el aire (menor que en el suelo → más control aéreo). */
    private static final double SLIDE_AIR         = 0.74;

    /** Aceleración base en el suelo. */
    private static final double ACCEL_GROUND      = 2.5;
    /** Aceleración base en el aire (menor → menos control aéreo brusco). */
    private static final double ACCEL_AIR         = 1.07;

    public PlayerPhysics(double gravity) {
        super(gravity);
        mass    = 1.0;
        aGround = ACCEL_GROUND;
        aAir    = ACCEL_AIR;
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
        speedMaxPiso = running ? RUN_SPEED_GROUND : WALK_SPEED_GROUND;
        speedMaxAir  = running ? RUN_SPEED_AIR    : WALK_SPEED_AIR;

        // IMPORTANTE: asignar slide ANTES de super.moveX()
        slide = onGround ? SLIDE_GROUND : SLIDE_AIR;

        super.moveX(inputX, onGround, running);
    }
}
