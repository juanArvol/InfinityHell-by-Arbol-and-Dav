package Game.Enemys.AI;

import Game.Enemys.Enemy;

/**
 * Máquina de IA del enemigo — delega en EnemyComport para decidir qué hacer.
 *
 * CAMBIO vs. original: update() ya no recibe `Player player` sino `EnemyContext ctx`.
 * Esto desacopla la IA de la clase concreta Player.
 *
 * El EnemyContext lo construye Enemy.update() (ver Enemy.java).
 * EnemyAI solo sabe que tiene un "objetivo" con posición y centro.
 *
 * Permite cambiar el comportamiento en runtime sin recrear el enemigo:
 *   ai.setBehavior(new FleeBehavior());  // cambio a huida cuando vida baja
 */
public class EnemyAI {

    private EnemyComport behavior;

    public EnemyAI(EnemyComport behavior) {
        this.behavior = behavior;
    }

    public void setBehavior(EnemyComport behavior) {
        this.behavior = behavior;
    }

    public EnemyComport getBehavior() {
        return behavior;
    }

    public void update(Enemy enemy, EnemyContext ctx) {
        EnemyAction action = behavior.decideAction(enemy, ctx);
        if (action != null) {
            action.execute(enemy);
        }
    }
}
