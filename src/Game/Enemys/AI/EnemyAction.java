package Game.Enemys.AI;

import Game.Enemys.Core.Enemy;

/**
 * Acción concreta que ejecuta un enemigo en un frame.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 * Actualizado para operar sobre Game.Enemys.Core.Enemy — el núcleo único
 * del nuevo framework. Contrato sin cambios: recibe Enemy, ejecuta algo.
 */
public interface EnemyAction {
    void execute(Enemy enemy);
}
