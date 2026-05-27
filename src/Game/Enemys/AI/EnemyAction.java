package Game.Enemys.AI;

import Game.Enemys.Enemy;

/**
 * Acción concreta que ejecuta un enemigo en un frame.
 * Sin cambios respecto al original — ya estaba bien diseñada.
 */
public interface EnemyAction {
    void execute(Enemy enemy);
}
