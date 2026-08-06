package Game.Items.Types.Bullets.Movement;

import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Items.Types.Bullets.Bullet;
import Game.Items.Types.Bullets.ProjectileMovement;

/**
 * Movimiento boomerang — el proyectil avanza, frena y vuelve al origen.
 *
 * Fases:
 *   1. OUTWARD  — avanza en la dirección inicial acelerando/desacelerando.
 *   2. RETURNING — gira hacia el punto de origen y regresa.
 *   3. ARRIVED  — cuando llega al origen, el proyectil muere.
 *
 * Casos de uso:
 *   - Boomerangs y hachas de retorno
 *   - Latigos de energía
 *   - Proyectiles teledirigidos de retorno
 *   - Yo-yo de combate
 *
 * Uso:
 *   Vector2D origin = player.getPosition().copy();
 *   ProjectileMovement m = new BoomerangMovement(origin, 45, 10.0);
 *   // travelTicks=45: frames de vuelo hacia afuera antes de volver
 *   // returnSpeed=10: velocidad de regreso
 */
public final class BoomerangMovement implements ProjectileMovement {

    private final Vector2D origin;
    private final int      travelTicks;
    private final double   returnSpeed;
    /** Frame counter. Renombrado de 'tick' para evitar confusión con el método tick(). */
    private int frameCount = 0;

    /**
     * @param origin       posición de origen (se captura al construir el movimiento)
     * @param travelTicks  ticks que el proyectil avanza antes de volver
     * @param returnSpeed  velocidad de regreso (unidades/frame)
     */
    public BoomerangMovement(Vector2D origin, int travelTicks, double returnSpeed) {
        this.origin      = new Vector2D(origin.getX(), origin.getY());
        this.travelTicks = travelTicks;
        this.returnSpeed = returnSpeed;
    }

    @Override
    public void tick(Bullet bullet) {
        frameCount++;

        if (frameCount <= travelTicks) {
            return;
        }

        Vector2D pos = bullet.getTransform().getPosition();
        double dx   = origin.getX() - pos.getX();
        double dy   = origin.getY() - pos.getY();
        double dist = Math.hypot(dx, dy);

        if (dist < returnSpeed) {
            bullet.getBulletLife().kill();
            return;
        }

        double scale = returnSpeed / dist;
        bullet.getPhysics().setXspeed(dx * scale);
        bullet.getPhysics().setYspeed(dy * scale);
    }

    /**
     * BoomerangMovement tiene estado interno (frameCount).
     * NO es seguro compartir una instancia entre proyectiles.
     */
    @Override
    public boolean isStateless() {
        return false;
    }
}
