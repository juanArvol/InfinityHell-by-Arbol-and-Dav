package Game.Enemys.Bosses.Sans.AI;

import Game.Enemys.AI.EnemyAction;
import Game.Enemys.Bosses.Sans.Components.SansInvincibilityComponent;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Enemy;

/**
 * Acción de teletransporte de Sans.
 *
 * ── HRFC-006 ──────────────────────────────────────────────────────────────
 * Migrado de EnemyVariables a EnemyStats / EnemyFlags.
 *
 *   Rango de teleporte  → enemy.getStats().getTeleportRange()
 *   Flag de invulnerable → enemy.getFlags().setInvincible(true)
 *   Timer de inv.        → SansInvincibilityComponent.activateTimer(frames)
 *
 * La invulnerabilidad se activa a través del componente, que gestiona
 * el conteo y la desactivación automática. Enemy.damage() ya respeta
 * el flag flags.isInvincible(), sin necesidad de lógica adicional en la bala.
 */
public final class SansTeleportAction implements EnemyAction {

    @Override
    public void execute(Enemy enemy) {
        double range = enemy.getStats().getTeleportRange();
        if (range <= 0) range = 300.0; // fallback por seguridad

        // ── Calcular posición destino aleatoria dentro del rango ──────────
        double angle  = Math.random() * Math.PI * 2;
        double radius = range * 0.5 + Math.random() * range * 0.5;

        double newX = enemy.getTransform().getPosition().getX() + Math.cos(angle) * radius;
        double newY = enemy.getTransform().getPosition().getY() + Math.sin(angle) * radius;

        // ── Teleporte: desplazamiento directo al transform ────────────────
        enemy.getTransform().getPosition().setX(newX);
        enemy.getTransform().getPosition().setY(newY);

        // ── Resetear velocidad para evitar deriva post-teleporte ──────────
        if (enemy.getPhysics() != null) {
            enemy.getPhysics().getVelocity().setX(0);
            enemy.getPhysics().getVelocity().setY(0);
        }

        // ── Activar invulnerabilidad temporal ─────────────────────────────
        enemy.getFlags().setInvincible(true);

        // Delegar el conteo al componente registrado en el registry.
        // Si el componente no está registrado, la invulnerabilidad se activa
        // igualmente — simplemente no expirará automáticamente (graceful degradation).
        SansInvincibilityComponent invComp =
            enemy.getComponentRegistry().get(SansInvincibilityComponent.class);
        if (invComp != null) {
            invComp.activateTimer(SansVariables.INVINCIBLE_FRAMES);
        }
    }
}
