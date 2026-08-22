package Game.Enemys.Bosses.Sans.AI;

import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.AI.EnemyAction;
import Game.Enemys.Core.AI.EnemyComport;
import Game.Enemys.Core.AI.EnemyContext;
import Game.Enemys.Core.AI.Actions.IdleCommand;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Comportamiento de IA de Sans — esquiva y reposicionamiento.
 *
 * ── HRFC-006 ──────────────────────────────────────────────────────────────
 * Migrado de EnemyVariables a EnemyStats.
 *
 *   Rango de teleporte → enemy.getStats().getTeleportRange()
 *
 * Ya no existe la clave String "sans.teleport_range" en EnemyVariables.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * Sans no persigue al jugador. Su IA:
 *   1. Evalúa la distancia al jugador.
 *   2. Si el jugador está dentro del rango personal → teletransportarse.
 *   3. Si no → Idle (Sans es vago por naturaleza).
 *
 * SansMovement gestiona el movimiento orbital continuo.
 * SansDodgeBehavior gestiona únicamente la decisión de teleportarse.
 */
public final class SansDodgeBehavior implements EnemyComport {

    /** Distancia mínima por defecto si teleportRange no está configurada. */
    private static final double FALLBACK_PERSONAL_SPACE = 120.0;

    private final EnemyAction        idle     = new IdleCommand();
    private final SansTeleportAction teleport = new SansTeleportAction();

    @Override
    public EnemyAction decideAction(Enemy enemy, EnemyContext ctx) {
        Vector2D playerPos = ctx.getPosition();
        Vector2D sansPos   = enemy.getTransform().getPosition();

        double dx     = playerPos.getX() - sansPos.getX();
        double dy     = playerPos.getY() - sansPos.getY();
        double distSq = dx * dx + dy * dy;

        // Leer el rango personal directamente de EnemyStats
        double minDist = enemy.getStats().getTeleportRange();
        if (minDist <= 0) minDist = FALLBACK_PERSONAL_SPACE;

        if (distSq < minDist * minDist) {
            return teleport;
        }

        return idle;
    }
}
