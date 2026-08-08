package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.ResettableMovement;
import Game.Items.Types.Bullets.Definition.Bullet;

/**
 * Movimiento sinusoidal — el proyectil ondula perpendicularmente a su dirección.
 *
 * Añade un componente de velocidad oscilante en el eje Y. Produce el efecto
 * "snake" o "wave" visible en bullet-hells.
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
 *
 * ── Pool ──────────────────────────────────────────────────────────────────
 *
 * Implementa ResettableMovement: frameCount puede resetearse a 0, lo que
 * permite al ProjectilePool reutilizar instancias de proyectiles sinusoidales.
 * El comportamiento post-reset es idéntico a una instancia recién creada.
 */
public final class SinusoidalMovement implements ResettableMovement {

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
     * No puede compartirse como singleton — cada proyectil necesita su propia instancia.
     * El pool puede reutilizar mediante reset().
     */
    @Override
    public boolean isStateless() {
        return false;
    }

    /**
     * Resetea el frame counter al estado inicial.
     * Llamado por ProjectilePool antes de reutilizar el proyectil.
     */
    @Override
    public void reset() {
        frameCount = 0;
    }
}
