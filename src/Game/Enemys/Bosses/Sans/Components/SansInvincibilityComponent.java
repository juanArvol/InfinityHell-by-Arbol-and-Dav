package Game.Enemys.Bosses.Sans.Components;

import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Contracts.EnemyComponent;
import Game.Enemys.Core.Enemy;

/**
 * Componente de invulnerabilidad temporal de Sans.
 *
 * ── Responsabilidad ──────────────────────────────────────────────────────
 * Decrementa cada frame el timer de invulnerabilidad post-teleporte.
 * Cuando el timer llega a 0, desactiva el flag "sans.invincible".
 *
 * ── Por qué un EnemyComponent ────────────────────────────────────────────
 * La invulnerabilidad de Sans es una capacidad opcional específica de este
 * Boss. No pertenece al Core — ni a HealthComponent, ni a Enemy directamente.
 * Un EnemyComponent registrado en EnemyComponentRegistry es el lugar exacto
 * para esta lógica: vive con Sans, se actualiza cada frame, y no contamina
 * ningún sistema genérico.
 *
 * ── Integración con daño ─────────────────────────────────────────────────
 * El flag "sans.invincible" es leído por cualquier sistema que aplique daño
 * a Sans. Por ejemplo, BulletNormal.onHitEnemy() puede chequear:
 *
 *   if (enemy.getVariables().getBoolean(SansVariables.INVINCIBLE)) return;
 *
 * Esa verificación vive en el código de la bala, no en Enemy — correcto.
 */
public final class SansInvincibilityComponent implements EnemyComponent {

    @Override
    public void update(Enemy enemy) {
        if (!enemy.getVariables().getBoolean(SansVariables.INVINCIBLE)) return;

        double timer = enemy.getVariables().getDouble("sans.invincible_timer", 0);
        if (timer > 0) {
            enemy.getVariables().set("sans.invincible_timer", timer - 1);
        } else {
            // Timer agotado — desactivar invulnerabilidad
            enemy.getVariables().set(SansVariables.INVINCIBLE, false);
        }
    }
}
