package Game.Enemys.AI;

import Game.Enemys.Core.Enemy;

/**
 * Comportamiento de IA de un enemigo.
 *
 * ── HRFC-005 ─────────────────────────────────────────────────────────────
 * Actualizado para operar sobre Game.Enemys.Core.Enemy.
 *
 * EnemyComport decide QUÉ acción ejecutar dado el estado del Enemy
 * y el contexto del objetivo actual. La acción resultante la ejecuta
 * EnemyAIController — que es quien llama decideAction() cada frame.
 */
public interface EnemyComport {

    EnemyAction decideAction(Enemy enemy, EnemyContext ctx);
}
