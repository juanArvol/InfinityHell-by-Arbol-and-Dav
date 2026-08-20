package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Core.Enemy;

/**
 * Mueve al enemigo en el eje X.
 *
 * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────────
 *
 * MIGRACIÓN: execute() ahora requiere deltaTime para propagación a physics.moveX().
 * La velocidad del enemigo debe estar expresada en units/s, no units/frame.
 */
public class MoveCommand implements EnemyAction {

    private final double speed;
    private final boolean right;

    /**
     * @param speed velocidad de movimiento en units/s (antes: units/frame)
     * @param right true para moverse a la derecha, false para izquierda
     */
    public MoveCommand(double speed, boolean right) {
        this.speed = speed;
        this.right = right;
    }

    /**
     * Ejecuta el comando de movimiento.
     *
     * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────
     *
     * @param enemy enemigo objetivo
     * @param deltaTime tiempo del simulation step en segundos
     */
    @Override
    public void execute(Enemy enemy, double deltaTime) {
        enemy.getState().setMoving(true);
        enemy.getPhysics().moveX(
            right ? speed : -speed,
            enemy.getState().isEnElSuelo(),
            false,
            deltaTime
        );
    }
}
