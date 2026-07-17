package Game.Enemys.Bosses.Sans.Patterns;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Core.Contracts.AttackPattern;
import Game.Enemys.Core.Enemy;
import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.SpawnProjectileEvent;

/**
 * Patrón de ataque de Sans — lluvia de huesos.
 *
 * ── HRFC-006 ──────────────────────────────────────────────────────────────
 * Migrado de EnemyVariables + OnWeaponFireEvent a EnemyStats + SpawnProjectileEvent.
 *
 *   Cooldown    → enemy.getStats().getAttackCooldownInt()
 *   Invencible  → enemy.getFlags().isInvincible()
 *   Evento      → SpawnProjectileEvent en lugar de OnWeaponFireEvent
 *
 * OnWeaponFireEvent está diseñado para el sistema de armas del jugador.
 * SpawnProjectileEvent es el canal genérico para cualquier entidad del mundo.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * BoneBarragePattern es stateful solo en su cooldown interno.
 * El cooldown actual lo lee de EnemyStats cada vez que se ejecuta,
 * lo que permite a las fases cambiar la cadencia sin modificar el patrón.
 */
public final class BoneBarragePattern implements AttackPattern {

    private int cooldownTimer = 0;

    @Override
    public String id() { return "sans.bone_barrage"; }

    @Override
    public void update(Enemy enemy) {
        if (cooldownTimer > 0) cooldownTimer--;
    }

    @Override
    public boolean canExecute(Enemy enemy, EnemyContext ctx) {
        if (cooldownTimer > 0) return false;
        // No atacar mientras Sans está en modo invulnerable (post-teleporte)
        return !enemy.getFlags().isInvincible();
    }

    @Override
    public void execute(Enemy enemy, EnemyContext ctx) {
        // Aplicar el cooldown configurado en EnemyStats para esta fase
        cooldownTimer = enemy.getStats().getAttackCooldownInt();

        // Emitir SpawnProjectileEvent — el sistema de proyectiles lo procesa.
        // "sans.bone" identifica el tipo de proyectil a instanciar.
        // origin: centro del enemy / target: centro del jugador.
        GameEventBus.GLOBAL.post(new SpawnProjectileEvent(
            "sans.bone",
            enemy.getTransform().getPosition(),
            ctx.getPosition(),
            enemy
        ));
    }
}
