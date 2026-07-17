package Game.Enemys.Bosses.Sans.Phases;

import Game.Enemys.Bosses.Sans.AI.SansDodgeBehavior;
import Game.Enemys.Bosses.Sans.Movement.SansMovement;
import Game.Enemys.Bosses.Sans.Patterns.BoneBarragePattern;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Contracts.EnemyPhase;
import Game.Enemys.Core.Enemy;

/**
 * Fase 1 de Sans — El perezoso.
 *
 * ── HRFC-006 ──────────────────────────────────────────────────────────────
 * Migrado de EnemyVariables a EnemyStats / EnemyFlags.
 *
 *   Velocidad          → enemy.getStats().setSpeed()
 *   Daño               → enemy.getStats().setDamage()
 *   Cooldown de ataque → enemy.getStats().setAttackCooldown()
 *   Rango de teleporte → enemy.getStats().setTeleportRange()
 *   Invulnerabilidad   → enemy.getFlags().setInvincible()
 *   Movimiento         → SansMovement (ya no FlyingMovement)
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * Sans está tranquilo. Ataca con cadencia lenta, se teleporta sin urgencia.
 *
 * ── Transición a Fase 2 ──────────────────────────────────────────────────
 * Condición definida en SansAssembler: TimedTransition(600 frames).
 */
public final class SansPhase1 implements EnemyPhase {

    @Override
    public String id() { return "sans.phase_1"; }

    @Override
    public void onEnter(Enemy enemy) {
        // ── Movimiento: SansMovement propio ───────────────────────────────
        // onActivate() activa EnemyFlags.isFlying y EnemyState.setFlying.
        enemy.getMovementController().setStrategy(new SansMovement(), enemy);

        // ── IA: esquiva perezosa ──────────────────────────────────────────
        enemy.getAIController().setBehavior(new SansDodgeBehavior());

        // ── Ataques: lluvia de huesos lenta ───────────────────────────────
        enemy.getAttackController().clearPatterns();
        enemy.getAttackController().addPattern(new BoneBarragePattern());

        // ── Stats: valores de fase 1 ──────────────────────────────────────
        enemy.getStats()
            .setSpeed(SansVariables.PHASE1_SPEED)
            .setDamage(SansVariables.PHASE1_DAMAGE)
            .setAttackCooldown(SansVariables.PHASE1_ATK_COOLDOWN)
            .setTeleportRange(SansVariables.PHASE1_TELEPORT_RANGE);

        // ── Flags: fase 1 ─────────────────────────────────────────────────
        enemy.getFlags().setInvincible(false);
    }
}
