package Game.World.Spawn.Conditions;

import Game.World.Core.World;
import Game.World.Spawn.SpawnCondition;

/**
 * Condición de spawn activada por tiempo real.
 *
 * ── HRFC — Unified DeltaTime Migration & Temporal Model Completion ────────
 * ── Mini-HRFC — Final Temporal Normalization ──────────────────────────────
 *
 * MIGRACIÓN TEMPORAL:
 *   TimedSpawnCondition ahora usa tiempo real en segundos en lugar de ticks.
 *   Esto garantiza que el intervalo de spawn es independiente del framerate.
 *
 *   ANTES (frame-based @ 30 FPS):
 *     ticksSinceLastActivation++ cada tick
 *     intervalTicks = 120 ticks
 *     A 30 FPS: 120 ticks = 4.00 segundos
 *     A 60 FPS: 120 ticks = 2.00 segundos
 *     A 120 FPS: 120 ticks = 1.00 segundos
 *
 *   AHORA (time-based):
 *     elapsedSinceLastActivation += deltaTime
 *     intervalSeconds = 4.0 segundos
 *     A cualquier FPS: intervalo = 4.00 segundos
 *
 * ── COMPORTAMIENTO ────────────────────────────────────────────────────────
 *
 * Se activa cada intervalSeconds segundos.
 *
 * ── Uso ──────────────────────────────────────────────────────────────────
 *
 *   // Spawn cada 4 segundos
 *   SpawnRequest.withCondition(desc, TimedSpawnCondition.every(4.0))
 *
 *   // Spawn cada 4 segundos, máximo 5 veces
 *   SpawnRequest.withCondition(desc, TimedSpawnCondition.every(4.0).times(5))
 *
 * ── Migración desde código legacy ─────────────────────────────────────────
 *   CORRECCIÓN: El sistema legacy operaba a 30 FPS, no 60 FPS.
 *   
 *   ANTES: TimedSpawnCondition.every(120)  // 120 ticks @ 30 FPS
 *   AHORA: TimedSpawnCondition.every(4.0)  // 120/30 = 4.0 segundos
 *
 *   O usar factory (especificando 30 como targetFps):
 *   TimedSpawnCondition.fromTicks(120, 30)  // convierte 120 ticks @ 30 FPS a segundos
 */
public final class TimedSpawnCondition implements SpawnCondition {

    private final double intervalSeconds;
    private final int maxActivations; // 0 = infinito

    private double elapsedSinceLastActivation;
    private int activationCount;

    private TimedSpawnCondition(double intervalSeconds, int maxActivations) {
        this.intervalSeconds                = intervalSeconds;
        this.maxActivations                 = maxActivations;
        this.elapsedSinceLastActivation     = intervalSeconds; // ready on first check
        this.activationCount                = 0;
    }

    /**
     * Se activa cada intervalSeconds segundos, sin límite de veces.
     *
     * @param intervalSeconds intervalo en segundos reales
     */
    public static TimedSpawnCondition every(double intervalSeconds) {
        return new TimedSpawnCondition(intervalSeconds, 0);
    }

    /**
     * Factory method para compatibilidad con código legacy que usaba ticks.
     *
     * CORRECCIÓN: Especificar targetFps=30 para código legacy, no 60.
     *
     * @param intervalTicks cantidad de ticks entre spawns
     * @param targetFps framerate asumido por el código legacy (típicamente 30)
     * @return TimedSpawnCondition configurado con tiempo equivalente en segundos
     *
     * @deprecated Usar every() con segundos directamente
     */
    @Deprecated
    public static TimedSpawnCondition fromTicks(int intervalTicks, double targetFps) {
        return new TimedSpawnCondition(intervalTicks / targetFps, 0);
    }

    /**
     * Limita el número de activaciones.
     *
     * @param max cantidad máxima de spawns
     * @return nueva instancia con límite configurado
     */
    public TimedSpawnCondition times(int max) {
        return new TimedSpawnCondition(intervalSeconds, max);
    }

    /**
     * Evalúa si se debe activar el spawn.
     *
     * ── HRFC — Unified DeltaTime Migration ───────────────────────────────
     *
     * Acumula tiempo real transcurrido en lugar de contar ticks.
     * Al alcanzar intervalSeconds, activa el spawn y resetea el contador.
     *
     * @param world el mundo activo (no usado, pero requerido por interfaz)
     * @param deltaTime tiempo real del simulation step en segundos
     * @return true si el tiempo transcurrido alcanzó el intervalo configurado
     */
    @Override
    public boolean isMet(World world, double deltaTime) {
        if (maxActivations > 0 && activationCount >= maxActivations) return false;

        elapsedSinceLastActivation += deltaTime;
        
        if (elapsedSinceLastActivation >= intervalSeconds) {
            elapsedSinceLastActivation = 0.0;
            activationCount++;
            return true;
        }
        
        return false;
    }

    /**
     * Consulta el tiempo transcurrido desde la última activación.
     *
     * @return tiempo acumulado en segundos
     */
    public double getElapsedSinceLastActivation() {
        return elapsedSinceLastActivation;
    }

    /**
     * Consulta el intervalo configurado.
     *
     * @return intervalo en segundos
     */
    public double getIntervalSeconds() {
        return intervalSeconds;
    }

    /**
     * Consulta cuántas veces se ha activado.
     *
     * @return contador de activaciones
     */
    public int getActivationCount() {
        return activationCount;
    }

    /**
     * Consulta el progreso hacia la siguiente activación [0.0, 1.0].
     *
     * @return 0.0 = recién activado, 1.0 = punto de activación
     */
    public double getProgress() {
        return Math.min(1.0, elapsedSinceLastActivation / intervalSeconds);
    }

    /**
     * Resetea manualmente el timer y el contador de activaciones.
     */
    public void reset() {
        elapsedSinceLastActivation = 0.0;
        activationCount = 0;
    }
}
