package Game.Engine.GameMath.Physics.Implementation;

import Game.Engine.GameMath.Physics.MovementContext;
import Game.Engine.GameMath.Physics.Types.Physics2D;

/**
 * Física del jugador.
 *
 * ── Capas de modificador ─────────────────────────────────────────────────
 *
 *   mAccel = baseAccel
 *          × entityModifier      → (1/mass) × runBoost
 *          × surfaceModifier     → friction × accelScale  (en Physics)
 *          × statusModifier      → herido, stun, buff     (externo vía setter)
 *          × environmentModifier → viento, agua           (externo vía setter)
 *          × airControlModifier  → airControl del surface (en Physics)
 *
 * ── Cómo aplicar efectos de estado ──────────────────────────────────────
 *
 *   // Un solo efecto
 *   physics.statusStack().add("wounded", ctx -> ctx.onGround() ? 0.70 : 1.0);
 *
 *   // Varios efectos apilados — se multiplican entre sí cada frame
 *   physics.statusStack().add("poison",  ctx -> 0.60);
 *   physics.statusStack().add("haste",   ctx -> 1.30);
 *   // factor resultante: 0.60 × 0.70 × 1.30 = 0.546
 *
 *   // Stun total (FROZEN = 0.0, congela todo lo demás)
 *   physics.statusStack().add("stun", MovementModifier.FROZEN);
 *
 *   // Al expirar el efecto, se remueve limpiamente
 *   physics.statusStack().remove("poison");
 *
 *   // Zona de viento (desde el sistema de zonas del mundo)
 *   physics.environmentStack().add("wind", ctx ->
 *       ctx.inputX() > 0 ? 1.4 : 0.6   // a favor o en contra
 *   );
 *
 * ── Fix salto (documentado en Player.java) ──────────────────────────────
 *
 *   Slide se asigna ANTES de llamar super.moveX() para que el frame
 *   del salto use el slide correcto y no el del frame anterior.
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
     *
     * El boost de carrera vive aquí (en la capa que le corresponde)
     * y no en speedMax, lo que hace el efecto más orgánico: correr
     * acumula velocidad más rápido, no solo levanta el techo.
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
