package Game.World.Spawn.Conditions;

import Game.World.Core.World;
import Game.World.Spawn.SpawnCondition;

/**
 * Condición que se activa exactamente una vez, luego nunca más.
 *
 * Útil para spawns que deben ocurrir una sola vez sin usar SpawnRequest.immediate(),
 * por ejemplo cuando la condición inicial no está garantizada en el primer tick.
 *
 * Uso:
 *   // Spawn del boss una vez cuando el jugador entre en la sala:
 *   SpawnRequest.withCondition(bossDesc,
 *       new OnceThenDisableCondition(ZoneEnterCondition.forZone(bossRoom)));
 */
public final class OnceThenDisableCondition implements SpawnCondition {

    private final SpawnCondition delegate;
    private boolean activated = false;

    public OnceThenDisableCondition(SpawnCondition delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isMet(World world) {
        if (activated) return false;
        if (delegate.isMet(world)) {
            activated = true;
            return true;
        }
        return false;
    }
}
