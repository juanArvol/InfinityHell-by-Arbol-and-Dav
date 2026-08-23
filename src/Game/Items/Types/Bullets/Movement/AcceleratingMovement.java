package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento con aceleración — el proyectil gana o pierde velocidad exponencialmente.
 *
 * MIGRACIÓN TEMPORAL:
 *   AcceleratingMovement aplica aceleración exponencial frame-independent.
 *
 *   factor_dt = factor^(dt × FPS_BASE)
 *   donde FPS_BASE = 30 (framerate de referencia del sistema)
 *
 * Casos de uso:
 *   - Cohetes que arrancan despacio y aceleran (factor > 1.0)
 *   - Proyectiles que se frenan antes de desaparecer (factor < 1.0)
 *   - Balas de jefe que emiten un "aviso" lento y luego disparan veloz
 *   - Proyectiles que rebotan y pierden energía gradualmente
 *
 * Uso:
 *   // Cohete: empieza lento, acelera hasta 1200 units/s
 *   ProjectileMovement m = new AcceleratingMovement(1.08, 1200.0);
 *
 *   // Fricción: el proyectil se frena hasta detenerse
 *   ProjectileMovement m = new AcceleratingMovement(0.95, 0.0);
 */
public final class AcceleratingMovement implements ProjectileMovement {

    private final double accelFactor;  // multiplicador @ 30 FPS (>1 acelera, <1 frena)
    private final double maxSpeed;     // velocidad máxima en units/s

    /**
     * @param accelFactor multiplicador de velocidad @ 30 FPS (>1 acelera, <1 frena)
     * @param maxSpeed    velocidad máxima en units/s (0 = sin límite superior si se frena)
     */
    public AcceleratingMovement(double accelFactor, double maxSpeed) {
        this.accelFactor = accelFactor;
        this.maxSpeed    = maxSpeed;
    }

    @Override
    public void tick(Bullet bullet, double dt) {
        // ── Temporal Factor Conversion ────────────────────────────────────
        // accelFactor se interpreta como "multiplicador por frame @ FPS_BASE"
        // donde FPS_BASE = 30 (el framerate de referencia del sistema legacy).
        //
        // Para frame-independence: factor_dt = factor^(dt × FPS_BASE)
        //
        // JUSTIFICACIÓN DEL 30:
        //   El sistema fue diseñado originalmente @ 30 FPS. Los valores de
        //   accelFactor (ej: 1.08) están calibrados para ese framerate.
        //   La conversión exponencial preserva el comportamiento observable
        //   a cualquier framerate.
        //
        // EJEMPLOS:
        //   @ 30 FPS (dt=1/30): factor^((1/30)×30) = factor^1 = factor
        //   @ 60 FPS (dt=1/60): factor^((1/60)×30) = factor^0.5 = sqrt(factor)
        //   @ 120 FPS (dt=1/120): factor^((1/120)×30) = factor^0.25
        //
        double temporalFactor = Math.pow(accelFactor, dt * 30.0);
        
        double vx = bullet.getPhysics().getXspeed() * temporalFactor;
        double vy = bullet.getPhysics().getYspeed() * temporalFactor;

        if (maxSpeed > 0) {
            double speed = Math.hypot(vx, vy);
            if (speed > maxSpeed) {
                double scale = maxSpeed / speed;
                vx *= scale;
                vy *= scale;
            }
        }

        bullet.getPhysics().setXspeed(vx);
        bullet.getPhysics().setYspeed(vy);
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
