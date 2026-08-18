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
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 *
 * MIGRACIÓN: timer ahora es double (segundos) en lugar de int (frames).
 * activateTimer() ahora recibe segundos en lugar de frames.
 *
 * ── Responsabilidad ──────────────────────────────────────────────────────
 * Decrementa cada frame el timer post-teleporte. Cuando llega a 0,
 * llama enemy.getFlags().setInvincible(false) para restaurar la
 * vulnerabilidad de Sans.
 *
 * SansTeleportAction activa la invulnerabilidad (setInvincible(true)) y
 * llama activateTimer(seconds) en este componente para iniciar el conteo.
 *
 * ── Integración con daño ─────────────────────────────────────────────────
 * Enemy.damage() comprueba flags.isInvincible() antes de aplicar daño.
 * No se necesita ninguna verificación adicional en el lado de la bala.
 */
public final class SansInvincibilityComponent implements EnemyComponent {

    private double timer = 0.0;

    /**
     * Inicia el timer de invulnerabilidad.
     * Llamado por SansTeleportAction inmediatamente después de activar el flag.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe segundos en lugar de frames.
     *
     * @param seconds duración de la invulnerabilidad en segundos.
     */
    public void activateTimer(double seconds) {
        this.timer = Math.max(seconds, 0.0);
    }

    /**
     * Devuelve el tiempo restante de invulnerabilidad.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Retorna double (segundos) en lugar de int (frames).
     */
    public double getRemainingTime() {
        return timer;
    }

    @Override
    public void update(Enemy enemy, double deltaTime) {
        if (!enemy.getFlags().isInvincible()) return;

        if (timer > 0) {
            timer -= deltaTime;
            if (timer < 0) timer = 0;
        } else {
            // Timer agotado — desactivar invulnerabilidad
            enemy.getFlags().setInvincible(false);
        }
    }
}
