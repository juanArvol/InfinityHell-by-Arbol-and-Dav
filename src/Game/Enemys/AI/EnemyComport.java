package Game.Enemys.AI;

import Game.Enemys.Enemy;

/**
 * Comportamiento de IA de un enemigo.
 *
 * CAMBIO vs. original: el segundo parámetro era `Player player`.
 * Ahora es `EnemyContext ctx` — abstracción del objetivo.
 *
 * Retro-compatible: todo código que pasaba `player` solo necesita
 * pasarlo envuelto en `EnemyContext.of(player)`.
 *
 * Ver EnemyContext para los factory methods disponibles.
 */
public interface EnemyComport {

    EnemyAction decideAction(Enemy enemy, EnemyContext ctx);
}
