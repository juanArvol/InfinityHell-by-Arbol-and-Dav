package Game.Enemys.AI.Behaviors;

import Game.Enemys.AI.Actions.FollowSteeringCommand;
import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Enemy;

/**
 * Comportamiento de vuelo — persigue al objetivo con steering suave.
 *
 * CAMBIO vs. original:
 *   - Ya no recibe `Player player` en el constructor.
 *   - El Player (o cualquier objetivo) llega via EnemyContext en decideAction().
 *   - FollowSteeringCommand recibe el contexto cada frame, no una referencia fija.
 *
 * Esto permite que el enemigo volador cambie de objetivo en runtime
 * (ej: player muerto → ir a un punto de patrulla) sin reconstruir el behavior.
 *
 * FIX BUG-16: La instancia de FollowSteeringCommand se crea UNA SOLA VEZ
 * y se reutiliza en cada frame mediante updateContext(). Esto elimina la
 * presión sobre el GC que producía instanciar un nuevo objeto por frame
 * por cada enemigo volador activo (N enemigos × 30 fps = N×30 objetos/seg).
 *
 * Invariante de seguridad: decideAction() siempre llama updateContext()
 * antes de retornar la instancia, garantizando que el contexto es fresco.
 *
 * Uso:
 *   new FlyingBehavior()                  // speed=3.0, steeringForce=0.15
 *   new FlyingBehavior(2.0, 0.1)          // más lento
 */
public class FlyingBehavior implements EnemyComport {

    private final double maxSpeed;
    private final double steeringForce;

    /** Instancia cacheada — creada una sola vez, reutilizada cada frame. */
    private FollowSteeringCommand cachedCommand;

    public FlyingBehavior() {
        this(3.0, 0.15);
    }

    public FlyingBehavior(double maxSpeed, double steeringForce) {
        this.maxSpeed      = maxSpeed;
        this.steeringForce = steeringForce;
    }

    @Override
    public EnemyAction decideAction(Enemy enemy, EnemyContext ctx) {
        if (cachedCommand == null) {
            // Primera llamada: crear la instancia con el contexto inicial.
            cachedCommand = new FollowSteeringCommand(ctx, maxSpeed, steeringForce);
        } else {
            // Frames posteriores: actualizar el contexto sin crear un objeto nuevo.
            cachedCommand.updateContext(ctx);
        }
        return cachedCommand;
    }
}
