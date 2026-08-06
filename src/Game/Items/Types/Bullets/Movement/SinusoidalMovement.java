package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento sinusoidal — el proyectil ondula perpendicularmente a su dirección.
 *
 * Añade un componente de velocidad oscilante en el eje Y (o perpendicular
 * a la trayectoria si se extiende con rotación). Produce el efecto "snake"
 * o "wave" visible en muchos bullet-hells.
 *
 * Casos de uso:
 *   - Proyectiles serpenteantes de jefes
 *   - Ondas de energía
 *   - Balas de dispersión oscilatoria
 *   - Efectos mágicos ondulantes
 *
 * Uso:
 *   ProjectileMovement m = new SinusoidalMovement(4.0, 0.2);
 *   // amplitude=4: desplazamiento máximo en Y; frequency=0.2: ciclos/frame
 *
 *   // Composición con homing:
 *   ProjectileMovement m = new HomingMovement(target, 60, 7).andThen(
 *       new SinusoidalMovement(2.0, 0.3)
 *   );
 */
public final class SinusoidalMovement implements ProjectileMovement {

    private final double amplitude;
    private final double frequency;
    /** Frame counter interno. Renombrado de 'tick' para evitar confusión con el método tick(). */
    private int frameCount = 0;

    /**
     * @param amplitude  magnitud máxima de la oscilación en Y (unidades/frame)
     * @param frequency  frecuencia de la onda en ciclos/frame (típico: 0.05–0.3)
     */
    public SinusoidalMovement(double amplitude, double frequency) {
        this.amplitude = amplitude;
        this.frequency = frequency;
    }

    @Override
    public void tick(Bullet bullet) {
        frameCount++;
        double oscillation = amplitude * Math.sin(2 * Math.PI * frequency * frameCount);
        bullet.getPhysics().setYspeed(bullet.getPhysics().getYspeed() + oscillation);
    }

    /**
     * SinusoidalMovement tiene estado interno (frameCount).
     * NO es seguro compartir una instancia entre proyectiles.
     */
    @Override
    public boolean isStateless() {
        return false;
    }
}
