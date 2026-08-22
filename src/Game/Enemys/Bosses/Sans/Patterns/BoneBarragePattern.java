package Game.Enemys.Bosses.Sans.Patterns;

import Game.Enemys.Core.Contracts.AttackPattern;
import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.AI.EnemyContext;
import Game.Engine.GameEventBus;
import Game.Gameplay.Events.SpawnProjectileEvent;

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
 * ── HRFC — Unified DeltaTime Migration ───────────────────────────────────
 *
 * MIGRACIÓN: cooldownTimer ahora es double (segundos) en lugar de int (frames).
 *
 * ── HRFC — Instancias explícitas ─────────────────────────────────────────
 * El bus se inyecta en el constructor — no se accede a ninguna instancia global.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * BoneBarragePattern es stateful solo en su cooldown interno.
 * El cooldown actual lo lee de EnemyStats cada vez que se ejecuta,
 * lo que permite a las fases cambiar la cadencia sin modificar el patrón.
 */
public final class BoneBarragePattern implements AttackPattern {

    private final GameEventBus eventBus;
    private double cooldownTimer = 0.0;

    /**
     * @param eventBus bus de eventos donde publicar SpawnProjectileEvent.
     *                 Inyectado desde el Assembler que construye este patrón.
     */
    public BoneBarragePattern(GameEventBus eventBus) {
        if (eventBus == null) throw new IllegalArgumentException("BoneBarragePattern: eventBus is required");
        this.eventBus = eventBus;
    }

    @Override
    public String id() { return "sans.bone_barrage"; }

    /**
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * CAMBIO: Ahora recibe deltaTime y decrementa el cooldown en segundos.
     */
    @Override
    public void update(Enemy enemy, double deltaTime) {
        if (cooldownTimer > 0) {
            cooldownTimer -= deltaTime;
            if (cooldownTimer < 0) cooldownTimer = 0;
        }
    }

    @Override
    public boolean canExecute(Enemy enemy, EnemyContext ctx) {
        if (cooldownTimer > 0) return false;
        // No atacar mientras Sans está en modo invulnerable (post-teleporte)
        return !enemy.getFlags().isInvincible();
    }

    /**
     * ── HRFC Phase 2 — Unified Real-Time Simulation Stabilization ────────
     *
     * MIGRADO: attackCooldown ahora es double en segundos.
     * Ya no necesita conversión de frames a segundos.
     */
    @Override
    public void execute(Enemy enemy, EnemyContext ctx) {
        // Aplicar el cooldown configurado en EnemyStats para esta fase
        // MIGRADO: attackCooldown ahora es directamente en segundos
        cooldownTimer = enemy.getStats().getAttackCooldown();

        // Emitir SpawnProjectileEvent — el sistema de proyectiles lo procesa.
        // "sans.bone" identifica el tipo de proyectil a instanciar.
        // origin: centro del enemy / target: centro del jugador.
        eventBus.post(new SpawnProjectileEvent(
            "sans.bone",
            enemy.getTransform().getPosition(),
            ctx.getPosition(),
            enemy
        ));
    }
}
