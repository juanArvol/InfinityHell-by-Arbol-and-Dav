package Game.Enemys.Bosses.Sans.Phases;

import Game.Enemys.Bosses.Sans.AI.SansDodgeBehavior;
import Game.Enemys.Bosses.Sans.Patterns.BoneBarragePattern;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Contracts.EnemyPhase;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.Movement.FlyingMovement;
import Game.Enemys.Core.Variables.EnemyVariables;

/**
 * Fase 2 de Sans — La determinación.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * Sans deja de ser vago. Ataca más rápido, se teleporta con mayor rango
 * y el daño de sus huesos aumenta. Sigue usando el mismo patrón de ataque
 * y el mismo comportamiento de esquiva — solo cambian los valores.
 *
 * ── Principio de composición en práctica ────────────────────────────────
 * Fase 2 no crea nuevas clases de IA ni nuevos patrones.
 * Reutiliza SansDodgeBehavior y BoneBarragePattern con variables distintas.
 * La variación de comportamiento surge de EnemyVariables, no de herencia.
 *
 * ── Transición ───────────────────────────────────────────────────────────
 * Fase final — no hay transición de salida. Sans pelea hasta el final.
 */
public final class SansPhase2 implements EnemyPhase {

    @Override
    public String id() { return "sans.phase_2"; }

    @Override
    public void onEnter(Enemy enemy) {
        // ── Movimiento: sigue flotando ────────────────────────────────────
        // FlyingMovement ya está activo desde Fase 1. setStrategy() con el
        // mismo tipo activa onDeactivate + onActivate, limpiando el estado.
        enemy.getMovementController().setStrategy(new FlyingMovement(), enemy);

        // ── IA: misma esquiva, ahora más urgente (rango mayor en variables) ──
        enemy.getAIController().setBehavior(new SansDodgeBehavior());

        // ── Ataques: mismo patrón, cooldown reducido → más agresivo ───────
        enemy.getAttackController().clearPatterns();
        enemy.getAttackController().addPattern(new BoneBarragePattern());

        // ── Variables: fase 2 — más rápido y más daño ────────────────────
        enemy.getVariables()
            .set(SansVariables.ATK_COOLDOWN,         SansVariables.PHASE2_ATK_COOLDOWN)
            .set(SansVariables.TELEPORT_RANGE,       SansVariables.PHASE2_TELEPORT_RANGE)
            .set(SansVariables.INVINCIBLE,           false)
            .set(EnemyVariables.Keys.DAMAGE, SansVariables.PHASE2_DAMAGE);
    }
}
