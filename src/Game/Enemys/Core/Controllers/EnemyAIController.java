package Game.Enemys.Core.Controllers;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Enemy;

/**
 * Controlador de IA del Enemy.
 *
 * Evolución de EnemyAI para el nuevo framework.
 *
 * ── Diferencia con EnemyAI (legacy) ─────────────────────────────────────
 * EnemyAI era correcto en concepto pero vivía en Game.Enemys.AI y
 * operaba sobre la clase Enemy legacy. Este controlador opera sobre
 * Game.Enemys.Core.Enemy y se integra con EnemyMovementController.
 *
 * El comportamiento (EnemyComport) sigue siendo el mismo contrato:
 * recibe Enemy + EnemyContext y devuelve una EnemyAction que se ejecuta.
 *
 * ── Cambio de comportamiento en runtime ──────────────────────────────────
 * setBehavior() permite reemplazar el comportamiento sin reconstruir el Enemy.
 * Útil para: fases (fase 2 cambia de AggressiveBehavior a BerserkBehavior),
 * efectos (confusión → RandomBehavior), scripting de Bosses.
 *
 * ── Múltiples comportamientos ────────────────────────────────────────────
 * Si un enemigo necesita varios comportamientos simultáneos (ej: un Boss
 * que persigue Y dispara), el AttackController maneja los patrones
 * independientemente. EnemyAIController se enfoca en la lógica de movimiento/
 * decisión, no en los ataques.
 */
public final class EnemyAIController {

    private EnemyComport behavior;

    public EnemyAIController(EnemyComport behavior) {
        this.behavior = behavior;
    }

    /**
     * Reemplaza el comportamiento activo.
     * El cambio es efectivo desde el siguiente frame.
     */
    public void setBehavior(EnemyComport behavior) {
        this.behavior = behavior;
    }

    public EnemyComport getBehavior() {
        return behavior;
    }

    /**
     * Evalúa la IA y ejecuta la acción decidida.
     * Llamado por Enemy.update() cada frame.
     *
     * @param enemy el Enemy propietario.
     * @param ctx   contexto del objetivo; puede ser null.
     */
    public void update(Enemy enemy, EnemyContext ctx) {
        if (behavior == null || ctx == null) return;

        EnemyAction action = behavior.decideAction(enemy, ctx);
        if (action != null) {
            action.execute(enemy);
        }
    }
}
