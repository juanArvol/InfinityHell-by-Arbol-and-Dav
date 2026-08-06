package Game.Items.Types.Bullets.Movement;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.GameObjects;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;
import java.util.function.Supplier;

/**
 * Movimiento de seguimiento (homing) — el proyectil gira hacia un objetivo.
 *
 * En cada frame, calcula la dirección al objetivo y rota el vector de
 * velocidad actual hasta ese ángulo, limitado por turnSpeed. Esto produce
 * una curva suave en lugar de un giro abrupto.
 *
 * Casos de uso:
 *   - Misiles teledirigidos
 *   - Orbes de seguimiento
 *   - Proyectiles mágicos que persiguen al jugador
 *   - Invocaciones que buscan enemigos
 *
 * El target se proporciona como Supplier<GameObjects> para que sea lazy:
 * el objetivo puede cambiar en runtime (target muere, nuevo target) sin
 * reconstruir el HomingMovement.
 *
 * Uso:
 *   // El proyectil persigue al enemigo más cercano (actualizable):
 *   GameObjects target = world.getNearestEnemy(spawnPos);
 *   ProjectileMovement m = new HomingMovement(() -> target, 120.0, 8.0);
 *
 *   // El proyectil siempre persigue al jugador:
 *   ProjectileMovement m = new HomingMovement(player::getTransform, 90.0, 6.0);
 */
public final class HomingMovement implements ProjectileMovement {

    /** Proveedor lazy del objetivo. Null o () -> null = sin objetivo activo. */
    private final Supplier<GameObjects> targetSupplier;

    /** Velocidad de giro en grados por frame. 180° = giro instantáneo. */
    private final double turnSpeedDeg;

    /** Velocidad de movimiento del proyectil (magnitud del vector). */
    private final double speed;

    /**
     * @param targetSupplier proveedor del objeto objetivo (puede retornar null)
     * @param turnSpeedDeg   velocidad de giro máxima en grados/frame (1–360)
     * @param speed          velocidad de avance del proyectil en unidades/frame
     */
    public HomingMovement(Supplier<GameObjects> targetSupplier,
                          double turnSpeedDeg,
                          double speed) {
        this.targetSupplier = targetSupplier;
        this.turnSpeedDeg   = Math.toRadians(turnSpeedDeg);
        this.speed          = speed;
    }

    @Override
    public void tick(Bullet bullet) {
        GameObjects target = targetSupplier.get();
        if (target == null) return; // sin objetivo — movimiento recto

        Vector2D bulletPos = bullet.getTransform().getPosition();
        Vector2D targetPos = target.getTransform().getPosition();

        // Dirección al objetivo
        double dx = targetPos.getX() - bulletPos.getX();
        double dy = targetPos.getY() - bulletPos.getY();
        double dist = Math.hypot(dx, dy);
        if (dist < 1e-6) return; // ya llegó

        double targetAngle  = Math.atan2(dy, dx);
        double currentAngle = Math.atan2(
                bullet.getPhysics().getYspeed(),
                bullet.getPhysics().getXspeed()
        );

        // Diferencia angular más corta (entre -π y π)
        double diff = targetAngle - currentAngle;
        while (diff >  Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;

        // Limitar el giro por frame
        double rotation = Math.copySign(Math.min(Math.abs(diff), turnSpeedDeg), diff);
        double newAngle = currentAngle + rotation;

        bullet.getPhysics().setXspeed(Math.cos(newAngle) * speed);
        bullet.getPhysics().setYspeed(Math.sin(newAngle) * speed);
    }
}
