package Game.Enemys.Bosses.Sans.Phases;

import Game.Enemys.Bosses.Sans.AI.SansDodgeBehavior;
import Game.Enemys.Bosses.Sans.Movement.SansMovement;
import Game.Enemys.Bosses.Sans.Patterns.BoneBarragePattern;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Contracts.EnemyPhase;
import Game.Enemys.Core.Enemy;

/**
 * Fase 2 de Sans — La determinación.
 *
 * ── HRFC-006 ──────────────────────────────────────────────────────────────
 * Migrado de EnemyVariables a EnemyStats / EnemyFlags.
 * Movimiento cambiado de FlyingMovement a SansMovement.
 *
 * ── Principio de composición en práctica ────────────────────────────────
 * Fase 2 reutiliza SansDodgeBehavior y BoneBarragePattern.
 * La variación de comportamiento surge de EnemyStats, no de herencia.
 *
 * ── Transición ───────────────────────────────────────────────────────────
 * Fase final — sin transición de salida.
 */
public final class SansPhase2 implements EnemyPhase {

    @Override
    public String id() { return "sans.phase_2"; }

    @Override
    public void onEnter(Enemy enemy) {
        // ── Movimiento: SansMovement — reinicia el estado para la fase 2 ──
        // setStrategy con un nuevo SansMovement llama onDeactivate + onActivate,
        // reseteando el ángulo de órbita para la fase más agresiva.
        enemy.getMovementController().setStrategy(new SansMovement(), enemy);

        // ── IA: misma esquiva, ahora con mayor rango (definido en stats) ──
        enemy.getAIController().setBehavior(new SansDodgeBehavior());

        // ── Ataques: mismo patrón, cooldown reducido → más agresivo ───────
        enemy.getAttackController().clearPatterns();
        enemy.getAttackController().addPattern(new BoneBarragePattern(enemy.getEventBus()));

        // ── Stats: valores de fase 2 ──────────────────────────────────────
        enemy.getStats()
            .setSpeed(SansVariables.PHASE2_SPEED)
            .setDamage(SansVariables.PHASE2_DAMAGE)
            .setAttackCooldown(SansVariables.PHASE2_ATK_COOLDOWN)
            .setTeleportRange(SansVariables.PHASE2_TELEPORT_RANGE);

        // ── Flags: reiniciar invulnerabilidad al entrar en fase 2 ─────────
        enemy.getFlags().setInvincible(false);
    }
}
