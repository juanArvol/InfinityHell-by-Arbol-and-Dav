package Game.Enemys.Bosses.Sans.Patterns;

import Game.Enemys.AI.EnemyContext;
import Game.Enemys.Bosses.Sans.Variables.SansVariables;
import Game.Enemys.Core.Contracts.AttackPattern;
import Game.Enemys.Core.Enemy;
import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.GameEvents;

/**
 * Patrón de ataque de Sans — lluvia de huesos.
 *
 * ── Descripción ──────────────────────────────────────────────────────────
 * Sans lanza una serie de huesos en dirección al jugador con un cooldown
 * configurable. El patrón lee el cooldown actual desde EnemyVariables,
 * lo que permite que las fases ajusten la cadencia sin modificar el patrón.
 *
 * ── Reutilización ────────────────────────────────────────────────────────
 * BoneBarragePattern es stateful solo en su cooldown interno.
 * Si otra entidad necesitara el mismo patrón, puede instanciarlo directamente.
 *
 * ── Integración con proyectiles ──────────────────────────────────────────
 * execute() emite un GameEvents.OnWeaponFireEvent("sans.bone").
 * El sistema de proyectiles del juego escucha este evento y genera los huesos.
 * Esto evita que el módulo Boss conozca BulletFactory directamente.
 * Cuando ProjectileFactory esté disponible, se integrará aquí usando
 * enemy.getCenter() y ctx.getCenter() como origen y destino.
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
        // No atacar si Sans está en modo invulnerable (dodge)
        return !enemy.getVariables().getBoolean(SansVariables.INVINCIBLE);
    }

    @Override
    public void execute(Enemy enemy, EnemyContext ctx) {
        int cooldown = enemy.getVariables().getInt(SansVariables.ATK_COOLDOWN, 120);
        cooldownTimer = cooldown;

        // Emitir evento de disparo — el sistema de proyectiles lo procesa.
        // "sans.bone" identifica el tipo de proyectil a instanciar.
        // origin: enemy.getCenter()  /  target: ctx.getCenter()
        // disponibles para integración futura con ProjectileFactory.
        GameEventBus.GLOBAL.post(new GameEvents.OnWeaponFireEvent(null, "sans.bone"));
    }
}
