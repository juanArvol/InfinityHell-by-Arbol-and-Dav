package Game.World.Spawn;

import Game.World.Core.World;

/**
 * Disparador de un spawn.
 *
 * ── DIFERENCIA CON SpawnCondition ─────────────────────────────────────────
 * SpawnCondition es una CONDICIÓN continua (se evalúa cada tick).
 * SpawnTrigger es un EVENTO discreto (se activa una vez por ocurrencia).
 *
 * Usar SpawnCondition para:
 *   - Spawn cada N segundos (timer continuo)
 *   - Spawn mientras hay menos de N enemies
 *   - Spawn mientras el jugador esté en zona
 *
 * Usar SpawnTrigger para:
 *   - Spawn al morir un boss (ocurre exactamente una vez)
 *   - Spawn al entrar a una sala (ocurre al entrar, no continuamente)
 *   - Spawn al completar un objetivo de misión
 *   - Spawn por input del jugador
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 * SpawnTrigger mantiene internamente si fue "disparado". SpawnSystem
 * llama isTriggered(world) cada tick. El trigger se autodesactiva
 * después de ser consumido si isOneShot() es true.
 *
 * Los triggers persistentes (re-activables) implementan isOneShot() = false
 * y gestionan su propia ventana de re-activación.
 *
 * ── IMPLEMENTACIONES PREVISTAS ────────────────────────────────────────────
 *   ImmediateSpawnTrigger    → se activa en el próximo tick, luego nunca más
 *   EventSpawnTrigger        → se activa cuando un GameEventBus emite un evento
 *   EnemyDeathTrigger        → se activa al morir un enemy concreto
 *   MissionTrigger           → se activa al alcanzar una etapa de misión
 *   DistanceTrigger          → se activa cuando el jugador llega a X distancia
 *   ManualTrigger            → activado explícitamente por código externo
 */
public interface SpawnTrigger {

    /**
     * Retorna true si este trigger está activo para el tick actual.
     * SpawnSystem llama este método antes de procesar el spawn.
     *
     * @param world el mundo activo.
     * @return true si se debe procesar el spawn asociado a este trigger.
     */
    boolean isTriggered(World world);

    /**
     * Notifica al trigger que su spawn fue procesado.
     * Los triggers de un solo uso (oneShot) deben desactivarse aquí.
     */
    void onConsumed();

    /**
     * True si este trigger se desactiva después del primer uso.
     * False si puede ser activado múltiples veces.
     */
    boolean isOneShot();

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Trigger que se activa exactamente una vez en el próximo tick.
     * Típicamente usado para spawns manuales inmediatos.
     */
    static SpawnTrigger immediate() {
        return new SpawnTrigger() {
            private boolean fired = false;

            @Override public boolean isTriggered(World world) { return !fired; }
            @Override public void onConsumed() { fired = true; }
            @Override public boolean isOneShot() { return true; }
        };
    }

    /**
     * Trigger controlado manualmente: se activa cuando se llama fire()
     * y se consume automáticamente tras ser procesado.
     */
    static ManualTrigger manual() {
        return new ManualTrigger();
    }

    // ── ManualTrigger ─────────────────────────────────────────────────────

    /**
     * Trigger de activación manual.
     * fire() lo activa; SpawnSystem lo consume en el próximo tick.
     *
     * Uso desde código de gameplay:
     *   ManualTrigger t = SpawnTrigger.manual();
     *   spawnRequest.setTrigger(t);
     *   ...
     *   t.fire(); // activa el spawn
     */
    final class ManualTrigger implements SpawnTrigger {

        private volatile boolean pending = false;

        /** Activa el trigger. El spawn ocurrirá en el próximo tick de SpawnSystem. */
        public void fire() { pending = true; }

        @Override public boolean isTriggered(World world) { return pending; }
        @Override public void onConsumed() { pending = false; }
        @Override public boolean isOneShot() { return false; } // re-activable
    }
}
