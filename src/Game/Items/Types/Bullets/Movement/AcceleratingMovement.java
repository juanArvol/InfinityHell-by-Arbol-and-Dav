package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento con aceleración lineal — el proyectil gana o pierde velocidad.
 *
 * SISTEMA TEMPORAL PURO:
 *   - acceleration en units/s²
 *   - Integración: v_new = v_old + a × dt
 *   - NO depende de FPS, frames, ni conversiones
 *
 * Casos de uso:
 *   - Cohetes que arrancan despacio y aceleran
 *   - Proyectiles que se frenan (aceleración negativa)
 *   - Balas de jefe con advertencia lenta seguida de disparo veloz
 *   - Proyectiles con fricción/drag
 *
 * Uso:
 *   // Cohete: acelera a 500 units/s² hasta 1200 units/s
 *   ProjectileMovement m = new AcceleratingMovement(500.0, 1200.0);
 *
 *   // Fricción: desacelera a -200 units/s² hasta detenerse
 *   ProjectileMovement m = new AcceleratingMovement(-200.0, 0.0);
 */
public final class AcceleratingMovement implements ProjectileMovement {

    private final double acceleration;  // aceleración en units/s² (puede ser negativa)
    private final double targetSpeed;   // velocidad objetivo en units/s (0 = sin límite)

    /**
     * @param acceleration aceleración en units/s² (positiva acelera, negativa frena)
     * @param targetSpeed  velocidad objetivo en units/s
     *                     - Si acceleration > 0: velocidad máxima (clamp superior)
     *                     - Si acceleration < 0: velocidad mínima (clamp inferior, típicamente 0)
     */
    public AcceleratingMovement(double acceleration, double targetSpeed) {
        this.acceleration = acceleration;
        this.targetSpeed  = Math.abs(targetSpeed); // Siempre positivo (magnitud)
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        double vx = bullet.getPhysics().getXspeed();
        double vy = bullet.getPhysics().getYspeed();
        double currentSpeed = Math.hypot(vx, vy);
        
        if (currentSpeed < 1e-6) {
            // Velocidad casi cero: no hay dirección para acelerar/frenar
            return;
        }
        
        // Calcular nueva velocidad escalar
        double newSpeed = currentSpeed + (acceleration * dt);
        
        // Aplicar clamp según el signo de la aceleración
        if (acceleration > 0) {
            // Acelerando: clamp superior
            if (targetSpeed > 0) {
                newSpeed = Math.min(newSpeed, targetSpeed);
            }
        } else if (acceleration < 0) {
            // Frenando: clamp inferior
            newSpeed = Math.max(newSpeed, targetSpeed);
        }
        
        // Preservar dirección, aplicar nueva magnitud
        if (newSpeed > 1e-6) {
            double scale = newSpeed / currentSpeed;
            bullet.getPhysics().setXspeed(vx * scale);
            bullet.getPhysics().setYspeed(vy * scale);
        } else {
            // Detenido completamente
            bullet.getPhysics().setXspeed(0);
            bullet.getPhysics().setYspeed(0);
        }
    }

    /**
     * AcceleratingMovement no tiene estado interno mutable.
     * La misma instancia puede compartirse entre proyectiles del mismo tipo.
     */
    @Override
    public boolean isStateless() {
        return true;
    }
}
