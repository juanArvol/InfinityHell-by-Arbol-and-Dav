package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento compuesto — combina múltiples estrategias de movimiento.
 *
 * Aplica cada estrategia en orden. Las modificaciones de una estrategia
 * son visibles para las siguientes en el mismo frame.
 *
 * Ejemplos de combinaciones útiles:
 *   - HomingMovement + SinusoidalMovement → misil serpenteante
 *   - AcceleratingMovement + GravityMovement → proyectil pesado que acelera
 *   - OrbitalMovement + SinusoidalMovement → órbita ondulante
 *
 * Uso:
 *   ProjectileMovement m = new CompositeMovement(
 *       new HomingMovement(target, 90, 8.0),
 *       new SinusoidalMovement(3.0, 0.2)
 *   );
 *
 *   // O via andThen() (equivalente):
 *   ProjectileMovement m = new HomingMovement(target, 90, 8.0)
 *       .andThen(new SinusoidalMovement(3.0, 0.2));
 */
public final class CompositeMovement implements ProjectileMovement {

    private final ProjectileMovement[] movements;

    public CompositeMovement(ProjectileMovement... movements) {
        if (movements == null || movements.length == 0) {
            throw new IllegalArgumentException("CompositeMovement necesita al menos una estrategia");
        }
        this.movements = movements;
    }

    @Override
    public void tick(Bullet bullet) {
        for (ProjectileMovement m : movements) {
            m.tick(bullet);
        }
    }
}
