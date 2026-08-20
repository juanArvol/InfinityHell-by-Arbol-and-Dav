package Game.Enemys.AI.Actions;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Core.Enemy;

/**
 * Detiene al enemigo en el eje X.
 *
 * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────────
 * MIGRACIÓN: execute() ahora recibe deltaTime (no usado en este comando).
 */
public class IdleCommand implements EnemyAction {

    @Override
    public void execute(Enemy enemy, double deltaTime) {
        enemy.getState().setMoving(false);
        enemy.getPhysics().stopX();
    }
}
