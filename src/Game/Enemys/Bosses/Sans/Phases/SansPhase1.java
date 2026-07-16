package Game.Enemys.Bosses.Sans.Phases;

import Game.Enemys.Bosses.Sans.AI.SansDodgeBehavior;
import Game.Enemys.Bosses.Sans.Patterns.BoneBarragePattern;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Contracts.EnemyPhase;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.Movement.FlyingMovement;
import Game.Enemys.Core.Variables.EnemyVariables;

/**
 * Fase 1 de Sans — El perezoso.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * Sans está tranquilo. Ataca con cadencia lenta, se teleporta sin urgencia,
 * y su rango de "espacio personal" es pequeño.
 *
 * ── Lo que configura al entrar ───────────────────────────────────────────
 *   Movimiento : FlyingMovement (sans flota, sin gravedad)
 *   IA         : SansDodgeBehavior (esquiva si el jugador se acerca)
 *   Ataques    : BoneBarragePattern con cooldown lento (120 frames)
 *   Variables  : cooldown=120, teleport_range=300, damage=4
 *
 * ── Transición a Fase 2 ──────────────────────────────────────────────────
 * La condición de transición la define el EnemyPhaseController del Assembler.
 * Sans tiene 1 HP — la transición a Fase 2 es por tiempo (TimedTransition),
 * no por HP. Fase 2 comienza cuando el jugador demuestra persistencia.
 */
public final class SansPhase1 implements EnemyPhase {

    @Override
    public String id() { return "sans.phase_1"; }

    @Override
    public void onEnter(Enemy enemy) {
        // ── Movimiento: flota sin gravedad ────────────────────────────────
        enemy.getMovementController().setStrategy(new FlyingMovement(), enemy);

        // ── IA: esquiva perezosa ──────────────────────────────────────────
        enemy.getAIController().setBehavior(new SansDodgeBehavior());

        // ── Ataques: lluvia de huesos lenta ───────────────────────────────
        enemy.getAttackController().clearPatterns();
        enemy.getAttackController().addPattern(new BoneBarragePattern());

        // ── Variables: fase 1 ─────────────────────────────────────────────
        enemy.getVariables()
            .set(SansVariables.ATK_COOLDOWN,         SansVariables.PHASE1_ATK_COOLDOWN)
            .set(SansVariables.TELEPORT_RANGE,       SansVariables.PHASE1_TELEPORT_RANGE)
            .set(SansVariables.INVINCIBLE,           false)
            .set(EnemyVariables.Keys.DAMAGE, SansVariables.PHASE1_DAMAGE);
    }
}
