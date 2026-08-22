package Game.Enemys.Core.AI;

import Game.Enemys.Core.Enemy;

/**
 * Acción concreta que ejecuta un enemigo en un frame.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 * Actualizado para operar sobre Game.Enemys.Core.Enemy — el núcleo único
 * del nuevo framework. Contrato sin cambios: recibe Enemy, ejecuta algo.
 *
 * ── HRFC Phase 3 — Temporal Migration ─────────────────────────────────────
 * MIGRACIÓN: execute() ahora recibe deltaTime para propagación temporal.
 */
public interface EnemyAction {
    /**
     * Ejecuta la acción sobre el enemigo.
     *
     * @param enemy enemigo objetivo
     * @param deltaTime tiempo del simulation step en segundos
     */
    void execute(Enemy enemy, double deltaTime);
}
