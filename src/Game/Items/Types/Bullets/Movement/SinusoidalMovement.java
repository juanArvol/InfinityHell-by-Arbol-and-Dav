package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ResettableMovement;

/**
 * Movimiento sinusoidal — el proyectil ondula perpendicularmente a su dirección.
 *
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 *
 * MIGRACIÓN TEMPORAL:
 *   Ahora usa tiempo acumulado en lugar de frameCount para independencia
 *   del framerate. La frecuencia se expresa en Hz (ciclos/segundo) en lugar
 *   de ciclos/frame.
 *
 *   ANTES (frame-based):
 *     frequency = 0.2 ciclos/frame → 12 Hz a 60 FPS, 0.8 Hz a 4 FPS
 *
 *   AHORA (time-based):
 *     frequency = 12.0 Hz → 12 ciclos/segundo independientemente del FPS
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
 *   ProjectileMovement m = new SinusoidalMovement(40.0, 12.0);
 *   // amplitude=40: desplazamiento máximo en Y (u/s); frequency=12: 12 Hz
 *
 *   // Composición con homing:
 *   ProjectileMovement m = new HomingMovement(target, 60, 7).andThen(
 *       new SinusoidalMovement(20.0, 15.0)
 *   );
 *
 * ── Pool ──────────────────────────────────────────────────────────────────
 *
 * Implementa ResettableMovement: elapsedTime puede resetearse a 0, lo que
 * permite al ProjectilePool reutilizar instancias de proyectiles sinusoidales.
 * El comportamiento post-reset es idéntico a una instancia recién creada.
 */
public final class SinusoidalMovement implements ResettableMovement {

    private final double amplitude;  // magnitud máxima de la oscilación (unidades/s)
    private final double frequency;  // frecuencia de la onda en Hz (ciclos/segundo)
    private double elapsedTime = 0.0; // tiempo acumulado en segundos

    /**
     * @param amplitude  magnitud máxima de la oscilación en Y (unidades/segundo)
     * @param frequency  frecuencia de la onda en Hz - ciclos por segundo (típico: 5-15 Hz)
     */
    public SinusoidalMovement(double amplitude, double frequency) {
        this.amplitude = amplitude;
        this.frequency = frequency;
    }

    @Override
    public void tick(Bullet bullet, double deltaTime) {
        elapsedTime += deltaTime;
        // Oscilación basada en tiempo real
        double oscillation = amplitude * Math.sin(2 * Math.PI * frequency * elapsedTime);
        bullet.getPhysics().setYspeed(bullet.getPhysics().getYspeed() + oscillation);
    }

    /**
     * SinusoidalMovement tiene estado interno (elapsedTime).
     * No puede compartirse como singleton — cada proyectil necesita su propia instancia.
     * El pool puede reutilizar mediante reset().
     */
    @Override
    public boolean isStateless() {
        return false;
    }

    /**
     * Resetea el tiempo acumulado al estado inicial.
     * Llamado por ProjectilePool antes de reutilizar el proyectil.
     */
    @Override
    public void reset() {
        elapsedTime = 0.0;
    }
}
