package Game.Items.Types.Bullets.Movement;

import Game.Items.Types.Bullets.Definition.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento con aceleración — el proyectil gana o pierde velocidad linealmente.
 *
 * Multiplica la velocidad actual por un factor por frame, hasta alcanzar un
 * límite máximo. Un factor > 1 acelera, < 1 desacelera (fricción).
 *
 * Casos de uso:
 *   - Cohetes que arrancan despacio y aceleran
 *   - Proyectiles que se frenan antes de desaparecer
 *   - Balas de jefe que emiten un "aviso" lento y luego disparan veloz
 *   - Proyectiles que rebotan y pierden energía gradualmente
 *
 * Uso:
 *   // Cohete: empieza lento, acelera hasta x20
 *   ProjectileMovement m = new AcceleratingMovement(1.08, 20.0);
 *
 *   // Fricción: el proyectil se frena hasta detenerse
 *   ProjectileMovement m = new AcceleratingMovement(0.95, 0.0);
 */
public final class AcceleratingMovement implements ProjectileMovement {

    private final double accelFactor;
    private final double maxSpeed;

    /**
     * @param accelFactor multiplicador de velocidad por frame (>1 acelera, <1 frena)
     * @param maxSpeed    velocidad máxima absoluta (0 = sin límite superior si se frena)
     */
    public AcceleratingMovement(double accelFactor, double maxSpeed) {
        this.accelFactor = accelFactor;
        this.maxSpeed    = maxSpeed;
    }

    @Override
    public void tick(Bullet bullet) {
        double vx = bullet.getPhysics().getXspeed() * accelFactor;
        double vy = bullet.getPhysics().getYspeed() * accelFactor;

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
