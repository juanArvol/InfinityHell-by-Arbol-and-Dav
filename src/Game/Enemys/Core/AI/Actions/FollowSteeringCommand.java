package Game.Enemys.Core.AI.Actions;

import Game.Enemys.Core.AI.EnemyAction;
import Game.Enemys.Core.AI.EnemyContext;
import Game.Enemys.Core.Enemy;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Sigue al objetivo con steering suave.
 *
 * ── HRFC — Enemy Physics & Domain Refactor ───────────────────────────────
 *
 * AGREGADO: updateMaxSpeed() para actualizar el límite de velocidad
 * desde EntityStats en runtime.
 *
 * FlyingBehavior consulta enemy.getStats().getSpeed() cada frame y
 * actualiza este command, permitiendo que buffs/debuffs afecten el
 * movimiento sin reconstruir el command.
 *
 * FIX BUG-14 (conservado): steeringForce era 9999999 (teleport).
 * Ahora default = 0.15 → persecución fluida y realista.
 *
 * FIX BUG-16: updateContext() permite a FlyingBehavior reutilizar la misma
 * instancia entre frames en lugar de crear una nueva cada frame.
 * La instancia se actualiza con el contexto del frame actual antes de ejecutarse.
 */
public class FollowSteeringCommand implements EnemyAction {

    private EnemyContext ctx;
    private double maxSpeed;
    private final double steeringForce;

    /** Constructor con EnemyContext — el preferido desde FlyingBehavior. */
    public FollowSteeringCommand(EnemyContext ctx, double maxSpeed, double steeringForce) {
        this.ctx           = ctx;
        this.maxSpeed      = maxSpeed;
        this.steeringForce = steeringForce;
    }

    /** Defaults: maxSpeed=3.0, steeringForce=0.15 */
    public FollowSteeringCommand(EnemyContext ctx) {
        this(ctx, 3.0, 0.15);
    }

    /**
     * Actualiza el contexto para el frame actual.
     * Llamar desde FlyingBehavior.decideAction() antes de retornar la instancia
     * cacheada, evitando la creación de un nuevo objeto por frame.
     */
    public void updateContext(EnemyContext newCtx) {
        this.ctx = newCtx;
    }

    /**
     * Actualiza la velocidad máxima desde EntityStats.
     * Permite que buffs/debuffs afecten el movimiento dinámicamente.
     */
    public void updateMaxSpeed(double newMaxSpeed) {
        this.maxSpeed = newMaxSpeed;
    }

    @Override
    public void execute(Enemy enemy, double deltaTime) {
        enemy.getState().setMoving(true);

        Vector2D desired = ctx.getCenter()
                .subtract(enemy.getCenter());

        if (desired.lengthSquared() == 0) return;

        desired.normalizeLocal().scaleLocal(maxSpeed);

        Vector2D steering = desired.subtract(
            enemy.getPhysics().getVelocity()
        );

        steering.scaleLocal(steeringForce);

        enemy.getPhysics()
             .getVelocity()
             .addLocal(steering);

        enemy.getPhysics()
             .getVelocity()
             .limitLocal(maxSpeed);
    }
}
