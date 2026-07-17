package Game.Enemys.Bosses.Sans.Components;

import Game.Enemys.Core.Contracts.EnemyComponent;
import Game.Enemys.Core.Enemy;

/**
 * Componente de invulnerabilidad temporal de Sans.
 *
 * ── HRFC-006 ──────────────────────────────────────────────────────────────
 * Migrado de EnemyVariables a EnemyFlags.
 *
 * El timer ya no se almacena en EnemyVariables como double bajo la clave
 * "sans.invincible_timer". Ahora es un campo tipado interno al componente.
 * La bandera de invulnerabilidad vive en enemy.getFlags().isInvincible().
 *
 * ── Responsabilidad ──────────────────────────────────────────────────────
 * Decrementa cada frame el timer post-teleporte. Cuando llega a 0,
 * llama enemy.getFlags().setInvincible(false) para restaurar la
 * vulnerabilidad de Sans.
 *
 * SansTeleportAction activa la invulnerabilidad (setInvincible(true)) y
 * llama activateTimer(frames) en este componente para iniciar el conteo.
 *
 * ── Integración con daño ─────────────────────────────────────────────────
 * Enemy.damage() comprueba flags.isInvincible() antes de aplicar daño.
 * No se necesita ninguna verificación adicional en el lado de la bala.
 */
public final class SansInvincibilityComponent implements EnemyComponent {

    private int timer = 0;

    /**
     * Inicia el timer de invulnerabilidad.
     * Llamado por SansTeleportAction inmediatamente después de activar el flag.
     *
     * @param frames número de frames que durará la invulnerabilidad.
     */
    public void activateTimer(int frames) {
        this.timer = Math.max(frames, 0);
    }

    /** Devuelve los frames restantes de invulnerabilidad. */
    public int getRemainingFrames() {
        return timer;
    }

    @Override
    public void update(Enemy enemy) {
        if (!enemy.getFlags().isInvincible()) return;

        if (timer > 0) {
            timer--;
        } else {
            // Timer agotado — desactivar invulnerabilidad
            enemy.getFlags().setInvincible(false);
        }
    }
}
