package Game.Enemys.Bosses.Sans.Assembler;

import Game.Enemys.Bosses.Sans.Components.SansInvincibilityComponent;
import Game.Enemys.Bosses.Sans.Phases.SansPhase1;
import Game.Enemys.Bosses.Sans.Phases.SansPhase2;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Transitions.TimedTransition;
import Game.Enemys.Core.Variables.EnemyVariables;
import Game.Enemys.EnemyPhysicsConfig;
import Game.Engine.Components.Visuals.AnimationController;
import Sprites.Enemys.EnemyAssets;

/**
 * Ensamblador de Sans.
 *
 * ── HRFC-005 — Prueba de concepto del framework ──────────────────────────
 * Sans es el Boss de referencia que demuestra que el framework es correcto:
 *   - Usa el mismo Enemy.java que un Zombie.
 *   - Sus fases reconfiguran IA, movimiento y ataques en runtime.
 *   - El Core no sabe que Sans existe.
 *   - El módulo Sans no modifica nada del Core.
 *
 * ── Estructura del módulo Sans ───────────────────────────────────────────
 *   Bosses/Sans/
 *     Assembler/  → SansAssembler (este archivo)
 *     AI/         → SansDodgeBehavior, SansTeleportAction
 *     Phases/     → SansPhase1, SansPhase2
 *     Patterns/   → BoneBarragePattern
 *     Components/ → SansInvincibilityComponent
 *     Variables/  → SansVariables
 *
 * ── Configuración ────────────────────────────────────────────────────────
 *   HP      : 1 (Sans muere de un golpe si le alcanzas)
 *   Física  : flyingStandard (gravity=0, flota)
 *   Fases   : Phase1 → [TimedTransition 600 frames] → Phase2
 *   Componentes: SansInvincibilityComponent (gestiona el flag de esquiva)
 *
 * ── Sin BossCore ─────────────────────────────────────────────────────────
 * No existe herencia de ningún BossCore. Sans extiende EnemyAssembler,
 * exactamente igual que ZombieAssembler y FlyingEnemyAssembler.
 */
public final class SansAssembler extends EnemyAssembler {

    /** Duración de la Fase 1 en frames (~10 segundos a 60 fps). */
    private static final int PHASE1_DURATION_FRAMES = 600;

    @Override
    protected EnemyDefinition definition() {
        return EnemyDefinition.builder()
            .sprite(EnemyAssets.flyingHandle)      // placeholder — reemplazar con SansAssets
            .health(SansVariables.PHASE1_HP)
            .physics(EnemyPhysicsConfig.flyingStandard())
            .collider(32, 48)
            .build();
    }

    @Override
    protected void configure(Enemy enemy) {
        // ── Variables base ────────────────────────────────────────────────
        // Las fases sobreescriben estos valores al activarse.
        // Se definen aquí para garantizar que siempre existe un valor por defecto.
        enemy.getVariables()
            .setIfAbsent(EnemyVariables.Keys.SPEED,    0.0)
            .setIfAbsent(SansVariables.INVINCIBLE,     1)
            .setIfAbsent(SansVariables.ATK_COOLDOWN,   SansVariables.PHASE1_ATK_COOLDOWN)
            .setIfAbsent(SansVariables.TELEPORT_RANGE, SansVariables.PHASE1_TELEPORT_RANGE);

        // ── EnemyComponents opcionales ────────────────────────────────────
        // SansInvincibilityComponent gestiona el timer de invulnerabilidad
        // post-teleporte. Debe registrarse antes de iniciar las fases.
        enemy.getComponentRegistry().add(new SansInvincibilityComponent(), enemy);

        // ── Fases ─────────────────────────────────────────────────────────
        // Phase1 → [600 frames] → Phase2 (fase final)
        enemy.getPhaseController()
            .addPhase(new SansPhase1(), new TimedTransition(PHASE1_DURATION_FRAMES));
        enemy.getPhaseController()
            .addPhase(new SansPhase2(), null);

        // Inicia Fase 1 inmediatamente (llama SansPhase1.onEnter → configura
        // movimiento, IA, ataques y variables para la fase inicial)
        enemy.getPhaseController().start(enemy);

        // ── Visual ────────────────────────────────────────────────────────
        // Placeholder hasta que SansAssets esté disponible.
        enemy.addComponent(new AnimationController(EnemyAssets.flyingHandle, "idle"));
    }
}
