package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ResettableMovement;

/**
 * Movimiento sinusoidal — el proyectil ondula perpendicularmente a su dirección.
 *
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 * ── CORRECCIÓN HRFC-DT-007 — Temporal Velocity Semantics ─────────────────
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
 * CORRECCIÓN CRÍTICA — BUG DE ACUMULACIÓN EXPONENCIAL:
 *
 *   PROBLEMA IDENTIFICADO:
 *     La implementación anterior SUMABA la oscilación a velocity en cada frame:
 *       ySpeed = ySpeed + osc₁  (frame 1)
 *       ySpeed = ySpeed + osc₂  (frame 2)
 *       ySpeed = ySpeed + osc₃  (frame 3)
 *     
 *     Esto producía acumulación exponencial de velocidad, haciendo que el
 *     proyectil acelerara sin control en dirección Y.
 *
 *   SOLUCIÓN:
 *     Capturar baseVelocity en el primer tick (velocidad inicial del proyectil)
 *     y aplicar la oscilación como OFFSET desde esa base, no como incremento:
 *       ySpeed = baseVelocity.y + amplitude × sin(...)
 *     
 *     Esto garantiza que la oscilación sea una modulación ALREDEDOR de la
 *     velocidad base, no una acumulación progresiva.
 *
 * SEMÁNTICA CORREGIDA:
 *   - amplitude en units/s: velocidad máxima de la oscilación
 *   - frequency en Hz: ciclos por segundo
 *   - baseVelocity: velocidad inicial del proyectil (capturada en primer frame)
 *   - velocity resultante: baseVelocity + oscillation(t)
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
 *   // amplitude=40: velocidad máxima de oscilación (u/s); frequency=12: 12 Hz
 *
 *   // Composición con homing:
 *   ProjectileMovement m = new HomingMovement(target, 60, 7).andThen(
 *       new SinusoidalMovement(20.0, 15.0)
 *   );
 *
 * ── Pool ──────────────────────────────────────────────────────────────────
 *
 * Implementa ResettableMovement: elapsedTime y baseVelocity pueden resetearse,
 * lo que permite al ProjectilePool reutilizar instancias de proyectiles
 * sinusoidales. El comportamiento post-reset es idéntico a una instancia
 * recién creada.
 */
public final class SinusoidalMovement implements ResettableMovement {

    private final double amplitude;  // magnitud máxima de la oscilación (unidades/s)
    private final double frequency;  // frecuencia de la onda en Hz (ciclos/segundo)
    private double elapsedTime = 0.0; // tiempo acumulado en segundos
    
    /**
     * Velocidad base capturada en el primer tick.
     * null = aún no capturado (estado inicial y post-reset).
     * 
     * La oscilación se aplica como offset desde esta base, garantizando que
     * el proyectil ondule ALREDEDOR de su trayectoria inicial en lugar de
     * acumular velocidad exponencialmente.
     */
    private Double baseVelocityY = null;

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
        // Capturar velocidad base en el primer tick
        if (baseVelocityY == null) {
            baseVelocityY = bullet.getPhysics().getYspeed();
        }
        
        elapsedTime += deltaTime;
        
        // Oscilación sinusoidal alrededor de la velocidad base
        double oscillation = amplitude * Math.sin(2 * Math.PI * frequency * elapsedTime);
        
        // CORRECCIÓN: Aplicar como offset desde base, NO como incremento acumulativo
        bullet.getPhysics().setYspeed(baseVelocityY + oscillation);
    }

    /**
     * SinusoidalMovement tiene estado interno (elapsedTime, baseVelocityY).
     * No puede compartirse como singleton — cada proyectil necesita su propia instancia.
     * El pool puede reutilizar mediante reset().
     */
    @Override
    public boolean isStateless() {
        return false;
    }

    /**
     * Resetea el tiempo acumulado y la velocidad base al estado inicial.
     * Llamado por ProjectilePool antes de reutilizar el proyectil.
     */
    @Override
    public void reset() {
        elapsedTime = 0.0;
        baseVelocityY = null;  // Se capturará en el primer tick del nuevo ciclo
    }
}
