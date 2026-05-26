package Game.Fisics;

import Game.World.Surface.SurfaceMaterial;

/**
 * Snapshot inmutable del estado del objeto físico en un frame.
 *
 * Se construye en Physics.moveX() y se pasa a todos los
 * MovementModifier activos. Así cada modificador puede tomar
 * decisiones basadas en el contexto sin acoplarse a Physics.
 *
 *   MovementModifier agotado = ctx -> ctx.running() ? 0.6 : 1.0;
 *   MovementModifier agua    = ctx -> ctx.onGround() ? 0.5 : 0.7;
 */
public record MovementContext(
        double  inputX,
        boolean onGround,
        boolean running,
        double  currentVx,
        double  baseAccel,
        SurfaceMaterial surface
) {}
