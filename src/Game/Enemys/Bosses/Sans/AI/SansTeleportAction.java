package Game.Enemys.Bosses.Sans.AI;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Enemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Acción de teletransporte de Sans.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * Teleporta a Sans a una posición aleatoria dentro de su rango de movimiento.
 * La física no interviene — es un desplazamiento directo del transform.
 *
 * ── Invulnerabilidad ─────────────────────────────────────────────────────
 * Al ejecutarse, activa el flag "sans.invincible" durante invincibleFrames.
 * El flag se decrementa cada frame en SansInvincibilityComponent
 * (que el Assembler registra en el EnemyComponentRegistry).
 *
 * ── Reutilización ────────────────────────────────────────────────────────
 * Esta acción puede reutilizarse en cualquier enemy que necesite teleporte,
 * siempre que tenga SansVariables.TELEPORT_RANGE configurado.
 */
public final class SansTeleportAction implements EnemyAction {

    private final int invincibleFrames;

    public SansTeleportAction(int invincibleFrames) {
        this.invincibleFrames = invincibleFrames;
    }

    @Override
    public void execute(Enemy enemy) {
        double range = enemy.getVariables()
            .getDouble(SansVariables.TELEPORT_RANGE, 300.0);

        // Calcular posición destino aleatoria dentro del rango
        double angle  = Math.random() * Math.PI * 2;
        double radius = range * 0.5 + Math.random() * range * 0.5;

        double newX = enemy.getTransform().getPosition().getX() + Math.cos(angle) * radius;
        double newY = enemy.getTransform().getPosition().getY() + Math.sin(angle) * radius;

        // Teleporte: desplazamiento directo al transform (sin física)
        enemy.getTransform().getPosition().setX(newX);
        enemy.getTransform().getPosition().setY(newY);

        // Resetear velocidad para evitar que la física acumulada desvíe al enemy
        if (enemy.getPhysics() != null) {
            enemy.getPhysics().getVelocity().setX(0);
            enemy.getPhysics().getVelocity().setY(0);
        }

        // Activar invulnerabilidad temporal post-teleporte
        enemy.getVariables().set(SansVariables.INVINCIBLE, true);
        enemy.getVariables().set("sans.invincible_timer", invincibleFrames);
    }
}
