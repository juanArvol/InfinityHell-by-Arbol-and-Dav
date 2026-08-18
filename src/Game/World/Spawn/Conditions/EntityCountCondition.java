package Game.World.Spawn.Conditions;

import Game.World.Core.World;
import Game.World.Spawn.SpawnCondition;

/**
 * Condición de spawn basada en el conteo de entidades del mundo activo.
 *
 * Activa el spawn cuando el número de entidades del tipo dado
 * es menor o igual que el umbral configurado.
 *
 * Uso:
 *   // Spawnear cuando haya menos de 5 GameObjects en total:
 *   EntityCountCondition.lessThan(5)
 *
 *   // Spawnear cuando haya menos de 3 instancias de Enemy:
 *   EntityCountCondition.lessThan(3, Enemy.class)
 *
 * No filtra por tipo si {@code type} es null o {@code GameObjects.class}.
 */
public final class EntityCountCondition implements SpawnCondition {

    private final int             threshold;
    private final Class<?>        type;
    private final Comparison      comparison;

    public enum Comparison { LESS_THAN, LESS_THAN_OR_EQUAL, GREATER_THAN, EQUAL }

    private EntityCountCondition(int threshold, Class<?> type, Comparison comparison) {
        this.threshold  = threshold;
        this.type       = type;
        this.comparison = comparison;
    }

    /**
     * Activa cuando hay MENOS DE {@code max} entidades del tipo dado.
     */
    public static EntityCountCondition lessThan(int max) {
        return new EntityCountCondition(max, null, Comparison.LESS_THAN);
    }

    public static EntityCountCondition lessThan(int max, Class<?> type) {
        return new EntityCountCondition(max, type, Comparison.LESS_THAN);
    }

    /**
     * Activa cuando hay EXACTAMENTE {@code count} entidades del tipo dado.
     */
    public static EntityCountCondition exactly(int count, Class<?> type) {
        return new EntityCountCondition(count, type, Comparison.EQUAL);
    }

    /**
     * Activa cuando no hay ninguna entidad del tipo dado.
     */
    public static EntityCountCondition none(Class<?> type) {
        return new EntityCountCondition(0, type, Comparison.EQUAL);
    }

    @Override
    public boolean isMet(World world) {
        // Consultar el DynamicEntityRegistry
        long count = world.getDynamicEntityRegistry().getAll().stream()
            .filter(obj -> type == null || type.isInstance(obj))
            .count();

        return switch (comparison) {
            case LESS_THAN           -> count < threshold;
            case LESS_THAN_OR_EQUAL  -> count <= threshold;
            case GREATER_THAN        -> count > threshold;
            case EQUAL               -> count == threshold;
        };
    }
}
