package Game.Enemys.Bosses.Sans.AI;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.AI.EnemyComport;
import Game.Enemys.AI.EnemyContext;
import Game.Enemys.AI.Actions.IdleCommand;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Enemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Comportamiento de IA de Sans — esquiva y reposicionamiento.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * Sans no persigue al jugador como un enemigo normal. Su IA:
 *   1. Evalúa si está demasiado cerca del jugador.
 *   2. Si sí → activa la esquiva (teletransporte o retroceso).
 *   3. Si no → se queda en Idle (Sans es vago por naturaleza).
 *
 * ── Teletransporte ───────────────────────────────────────────────────────
 * SansTeleportAction se resuelve internamente: calcula una posición aleatoria
 * dentro del rango configurado en EnemyVariables y aplica el desplazamiento
 * directamente al transform del enemy. Sin física — teleport puro.
 *
 * ── Invulnerabilidad temporal ────────────────────────────────────────────
 * Al esquivar, Sans activa el flag "sans.invincible" durante algunos frames.
 * BoneBarragePattern respeta ese flag y no dispara mientras está activo.
 */
public final class SansDodgeBehavior implements EnemyComport {

    private static final double PERSONAL_SPACE    = 120.0;  // distancia mínima al jugador
    private static final int    INVINCIBLE_FRAMES = 30;     // frames de invulnerabilidad post-dodge

    private final EnemyAction idle       = new IdleCommand();
    private final SansTeleportAction teleport = new SansTeleportAction(INVINCIBLE_FRAMES);

    @Override
    public EnemyAction decideAction(Enemy enemy, EnemyContext ctx) {
        Vector2D playerPos = ctx.getPosition();
        Vector2D sansPos   = enemy.getTransform().getPosition();

        double dx = playerPos.getX() - sansPos.getX();
        double dy = playerPos.getY() - sansPos.getY();
        double distSq = dx * dx + dy * dy;

        double minDist = enemy.getVariables()
            .getDouble(SansVariables.TELEPORT_RANGE, PERSONAL_SPACE);

        // Si el jugador está demasiado cerca → teleportarse
        if (distSq < minDist * minDist) {
            return teleport;
        }

        return idle;
    }
}
