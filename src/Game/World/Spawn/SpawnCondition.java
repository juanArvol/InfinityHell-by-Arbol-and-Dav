package Game.World.Spawn;

import Game.World.Core.World;

/**
 * Condición de activación de un spawn.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Una SpawnCondition decide CUÁNDO un spawn debe ocurrir.
 * Se evalúa en cada tick o bajo demanda desde SpawnSystem.
 *
 * ── EJEMPLOS DE IMPLEMENTACIÓN ────────────────────────────────────────────
 *   SpawnCondition.always()              → siempre activa (spawn manual)
 *   SpawnCondition.never()               → nunca activa (deshabilitado)
 *   TimedSpawnCondition                  → cada N ticks
 *   ZoneEnterCondition                   → cuando una entidad entra en zona
 *   EnemyDeathCondition                  → cuando muere un enemy concreto
 *   EntityCountCondition                 → cuando hay menos de N entidades
 *   EventCondition                       → cuando se dispara un evento
 *   MissionCondition                     → cuando una misión llega a etapa X
 *   CompositeSpawnCondition              → AND/OR de condiciones
 *   OnceThenDisableCondition             → solo una vez, luego se desactiva
 *
 * ── COMPOSICIÓN ───────────────────────────────────────────────────────────
 * Las condiciones se componen con and() y or():
 *
 *   SpawnCondition cond =
 *       TimedSpawnCondition.every(60)
 *           .and(EntityCountCondition.lessThan(5))
 *           .and(SpawnCondition.never().or(EventCondition.on("boss_phase_2")));
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * isMet(world) se llama cada tick por SpawnSystem. Debe ser barato.
 * Si la evaluación requiere estado persistente, el implementador lo mantiene.
 */
@FunctionalInterface
public interface SpawnCondition {

    /**
     * Evalúa si la condición está satisfecha en el tick actual.
     *
     * @param world el mundo activo en este momento.
     * @return true si el spawn debe ocurrir.
     */
    boolean isMet(World world);

    // ── Condiciones base ──────────────────────────────────────────────────

    /** Condición siempre activa. Útil para spawns manuales puntuales. */
    static SpawnCondition always() { return world -> true; }

    /** Condición nunca activa. Útil para desactivar un spawn sin eliminarlo. */
    static SpawnCondition never()  { return world -> false; }

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Retorna una condición que es verdadera solo cuando AMBAS son verdaderas.
     */
    default SpawnCondition and(SpawnCondition other) {
        return world -> this.isMet(world) && other.isMet(world);
    }

    /**
     * Retorna una condición que es verdadera cuando CUALQUIERA es verdadera.
     */
    default SpawnCondition or(SpawnCondition other) {
        return world -> this.isMet(world) || other.isMet(world);
    }

    /**
     * Retorna la condición negada.
     */
    default SpawnCondition not() {
        return world -> !this.isMet(world);
    }
}
