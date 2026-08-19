package Game.World.Spawn;

import Game.World.Core.World;

/**
 * Condición de activación de un spawn.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Una SpawnCondition decide CUÁNDO un spawn debe ocurrir.
 * Se evalúa en cada tick o bajo demanda desde SpawnSystem.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 *
 * MIGRACIÓN TEMPORAL:
 *   El método isMet() ahora recibe deltaTime para permitir que condiciones
 *   temporales (TimedSpawnCondition) gestionen su estado basándose en
 *   tiempo real en lugar de ticks.
 *
 *   Condiciones no temporales (ZoneEnterCondition, EntityCountCondition)
 *   ignoran el parámetro deltaTime.
 *
 * ── EJEMPLOS DE IMPLEMENTACIÓN ────────────────────────────────────────────
 *   SpawnCondition.always()              → siempre activa (spawn manual)
 *   SpawnCondition.never()               → nunca activa (deshabilitado)
 *   TimedSpawnCondition                  → cada N segundos
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
 *       TimedSpawnCondition.every(2.0)  // cada 2 segundos
 *           .and(EntityCountCondition.lessThan(5))
 *           .and(SpawnCondition.never().or(EventCondition.on("boss_phase_2")));
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * isMet(world, deltaTime) se llama cada tick por SpawnSystem. Debe ser barato.
 * Si la evaluación requiere estado persistente, el implementador lo mantiene.
 */
@FunctionalInterface
public interface SpawnCondition {

    /**
     * Evalúa si la condición está satisfecha en el tick actual.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * Recibe deltaTime para que condiciones temporales puedan acumular
     * tiempo de forma independiente del framerate.
     *
     * Condiciones basadas en estado (EntityCount, ZoneEnter, etc.) ignoran deltaTime.
     *
     * @param world el mundo activo en este momento.
     * @param deltaTime tiempo real del simulation step en segundos
     * @return true si el spawn debe ocurrir.
     */
    boolean isMet(World world, double deltaTime);

    // ── Condiciones base ──────────────────────────────────────────────────

    /** Condición siempre activa. Útil para spawns manuales puntuales. */
    static SpawnCondition always() { return (world, dt) -> true; }

    /** Condición nunca activa. Útil para desactivar un spawn sin eliminarlo. */
    static SpawnCondition never()  { return (world, dt) -> false; }

    // ── Composición ───────────────────────────────────────────────────────

    /**
     * Retorna una condición que es verdadera solo cuando AMBAS son verdaderas.
     */
    default SpawnCondition and(SpawnCondition other) {
        return (world, dt) -> this.isMet(world, dt) && other.isMet(world, dt);
    }

    /**
     * Retorna una condición que es verdadera cuando CUALQUIERA es verdadera.
     */
    default SpawnCondition or(SpawnCondition other) {
        return (world, dt) -> this.isMet(world, dt) || other.isMet(world, dt);
    }

    /**
     * Retorna la condición negada.
     */
    default SpawnCondition not() {
        return (world, dt) -> !this.isMet(world, dt);
    }
}
