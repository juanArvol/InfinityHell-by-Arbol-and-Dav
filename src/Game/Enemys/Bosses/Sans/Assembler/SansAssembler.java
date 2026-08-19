package Game.Enemys.Bosses.Sans.Assembler;

import Game.Enemys.Bosses.Sans.Components.SansInvincibilityComponent;
import Game.Enemys.Bosses.Sans.Phases.SansPhase1;
import Game.Enemys.Bosses.Sans.Phases.SansPhase2;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyDefinition;
import Game.Enemys.Core.Transitions.TimedTransition;
import Game.Enemys.EnemyPhysicsConfig;
import Game.Engine.Entity.Attributes.EntityAttributes;
import Game.Engine.Entity.Combat.AttackSource;
import Game.Engine.Entity.Combat.AttackSources;
import Game.Engine.Entity.Components.Visuals.AnimationControllerComponent;
import Game.Engine.Entity.Flags.EntityFlags;
import Game.Engine.Entity.Stats.EntityStats;
import Sprites.Entity.Enemys.noBoss.Zombie.EnemyAssets;

/**
 * Ensamblador de Sans.
 *
 * ── HRFC-007 — Living Entity Core ────────────────────────────────────────
 * Migrado a los tipos genéricos del Living Entity Core:
 *   EnemyStats       → EntityStats
 *   EnemyFlags       → EntityFlags
 *   EnemyAttributes  → EntityAttributes
 *   AttackSource/s   → Game.Living.Combat
 *
 * ── Sin BossCore ─────────────────────────────────────────────────────────
 * Sans extiende EnemyAssembler exactamente igual que ZombieAssembler.
 *
 * ── Estructura del módulo Sans ───────────────────────────────────────────
 *   Bosses/Sans/
 *     Assembler/  → SansAssembler (este archivo)
 *     AI/         → SansDodgeBehavior, SansTeleportAction
 *     Movement/   → SansMovement
 *     Phases/     → SansPhase1, SansPhase2
 *     Patterns/   → BoneBarragePattern
 *     Components/ → SansInvincibilityComponent
 *     Variables/  → SansVariables
 */
public final class SansAssembler extends EnemyAssembler {

    // HRFC Phase 2: Migrado a tiempo real en segundos
    /** Duración de la Fase 1 en segundos. */
    private static final double PHASE1_DURATION_SECONDS = 10.0;  // 10 segundos (antes: 600 frames @ 60 fps)

    @Override
    protected EnemyDefinition definition() {
        return EnemyDefinition.builder()
            .sprite(EnemyAssets.flyingHandle)     // placeholder — reemplazar con SansAssets
            .health(SansVariables.PHASE1_HP)
            .physics(EnemyPhysicsConfig.flyingStandard())
            .collider(32, 48)
            .animation("idle")
            .animation("attack")
            .lootTable("sans_drops")
            .build();
    }

    @Override
    protected void configureStats(EntityStats stats) {
        // Valores de Fase 1 como base. Las fases los sobreescriben en onEnter().
        stats.setSpeed(SansVariables.PHASE1_SPEED)
             .setDamage(SansVariables.PHASE1_DAMAGE)
             .setAttackCooldown(SansVariables.PHASE1_ATK_COOLDOWN)
             .setTeleportRange(SansVariables.PHASE1_TELEPORT_RANGE);
    }

    @Override
    protected void configureFlags(EntityFlags flags) {
        // Sans empieza vulnerable; la invulnerabilidad se activa solo al teleportarse.
        flags.setInvincible(false)
             .setFlying(true);
    }

    @Override
    protected void configureAttributes(EntityAttributes attributes) {
        attributes.setFaction(EntityAttributes.Faction.BOSS)
                  .setElement(EntityAttributes.Element.DARK)
                  .setEntityClass(EntityAttributes.EntityClass.BOSS)
                  .setDifficultyTier(EntityAttributes.DifficultyTier.EXTREME);
    }

    @Override
    protected void configureAttackSources(AttackSources sources) {
        // Sans ataca con magia de alma Y con su cuerpo cuando es necesario.
        sources.add(AttackSource.MAGIC)
               .add(AttackSource.NATURAL);
    }

    @Override
    protected void configureComponents(Enemy enemy) {
        // SansInvincibilityComponent DEBE registrarse antes de iniciar las fases,
        // ya que SansTeleportAction lo busca por tipo en el registry.
        enemy.getComponentRegistry().add(new SansInvincibilityComponent(), enemy);
    }

    @Override
    protected void configureVisual(Enemy enemy) {
        // Placeholder hasta que SansAssets esté disponible.
        enemy.addComponent(new AnimationControllerComponent(EnemyAssets.flyingHandle, "idle"));

    }

    @Override
    protected void configurePhases(Enemy enemy) {
        // Phase1 → [10 segundos] → Phase2 (fase final)
        enemy.getPhaseController()
            .addPhase(new SansPhase1(), new TimedTransition(PHASE1_DURATION_SECONDS));
        enemy.getPhaseController()
            .addPhase(new SansPhase2(), null);

        // Activa Phase1 inmediatamente: llama SansPhase1.onEnter() que sobreescribe
        // los stats base con los valores de fase 1 y configura movimiento + IA.
        enemy.getPhaseController().start(enemy);
    }
}
