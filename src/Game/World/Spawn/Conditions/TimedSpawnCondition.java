package Game.World.Spawn.Conditions;

import Game.World.Core.World;
import Game.World.Spawn.SpawnCondition;

/**
 * Condición de spawn activada por tiempo.
 *
 * Se activa cada {@code intervalTicks} ticks.
 *
 * Uso:
 *   SpawnRequest.withCondition(desc, TimedSpawnCondition.every(120))
 *
 * También acepta un número máximo de activaciones:
 *   TimedSpawnCondition.every(60).times(5)  → máximo 5 spawns
 */
public final class TimedSpawnCondition implements SpawnCondition {

    private final int intervalTicks;
    private final int maxActivations; // 0 = infinito

    private int ticksSinceLastActivation;
    private int activationCount;

    private TimedSpawnCondition(int intervalTicks, int maxActivations) {
        this.intervalTicks          = intervalTicks;
        this.maxActivations         = maxActivations;
        this.ticksSinceLastActivation = intervalTicks; // ready on first check
        this.activationCount        = 0;
    }

    /**
     * Se activa cada {@code ticks} ticks, sin límite de veces.
     */
    public static TimedSpawnCondition every(int ticks) {
        return new TimedSpawnCondition(ticks, 0);
    }

    /**
     * Limita el número de activaciones.
     */
    public TimedSpawnCondition times(int max) {
        return new TimedSpawnCondition(intervalTicks, max);
    }

    @Override
    public boolean isMet(World world) {
        if (maxActivations > 0 && activationCount >= maxActivations) return false;

        ticksSinceLastActivation++;
        if (ticksSinceLastActivation >= intervalTicks) {
            ticksSinceLastActivation = 0;
            activationCount++;
            return true;
        }
        return false;
    }
}
