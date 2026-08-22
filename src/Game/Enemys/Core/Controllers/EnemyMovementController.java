package Game.Enemys.Core.Controllers;

import Game.Enemys.Core.Contracts.MovementStrategy;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.AI.EnemyContext;

/**
 * Controlador de movimiento del Enemy.
 *
 * Enemy nunca sabe cómo se mueve — delega en este controlador.
 * El controlador delega en la MovementStrategy activa.
 *
 * ── Por qué existe este controlador ─────────────────────────────────────
 * Una estrategia de movimiento puede cambiarse en runtime sin que Enemy
 * lo sepa: una fase transiciona de GroundMovement a FlyingMovement
 * llamando setStrategy(), y el Enemy sigue funcionando igual.
 *
 * ── Uso en un assembler ──────────────────────────────────────────────────
 *   enemy.getMovementController().setStrategy(new GroundMovement());
 *
 * ── Uso en una fase ──────────────────────────────────────────────────────
 *   enemy.getMovementController().setStrategy(new FlyingMovement());
 */
public final class EnemyMovementController {

    private MovementStrategy strategy;

    public EnemyMovementController() {}

    /**
     * Asigna una nueva estrategia de movimiento.
     * Llama onDeactivate() en la anterior y onActivate() en la nueva.
     *
     * @param strategy nueva estrategia; null desactiva el movimiento.
     * @param enemy    el Enemy propietario (para notificar ciclo de vida).
     */
    public void setStrategy(MovementStrategy strategy, Enemy enemy) {
        if (this.strategy != null) {
            this.strategy.onDeactivate(enemy);
        }
        this.strategy = strategy;
        if (this.strategy != null) {
            this.strategy.onActivate(enemy);
        }
    }

    /**
     * Versión sin notificación de ciclo de vida.
     * Usar solo durante el ensamblado inicial (antes de que el Enemy esté vivo).
     */
    public void setStrategy(MovementStrategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Ejecuta el movimiento del frame actual.
     * Llamado por Enemy.update().
     *
     * ── HRFC — Real DeltaTime Authority ──────────────────────────────────
     * Recibe deltaTime para propagarlo a MovementStrategy.
     * Las estrategias pueden usar deltaTime para movimientos temporales correctos.
     *
     * @param enemy el Enemy a mover.
     * @param ctx   contexto del objetivo; puede ser null.
     * @param deltaTime tiempo real del simulation step en segundos
     */
    public void update(Enemy enemy, EnemyContext ctx, double deltaTime) {
        if (strategy != null) {
            strategy.move(enemy, ctx, deltaTime);
        }
    }

    public MovementStrategy getStrategy() { return strategy; }

    public boolean hasStrategy() { return strategy != null; }
}
