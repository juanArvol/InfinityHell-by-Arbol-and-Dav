package Game.World.Spawn.Strategies;

import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyFactory;
import Game.Engine.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.Engine.Simulation.Storage.EntityStore;
import Game.World.Spawn.SpawnStrategy;

/**
 * Estrategia de spawn para enemigos tipados.
 */
public final class EnemySpawnStrategy implements SpawnStrategy {

    private final Supplier strategy;

    @FunctionalInterface
    private interface Supplier {
        Enemy create(Vector2D position);
    }

    public static EnemySpawnStrategy of(EnemyFactory.EnemyId id, GameEventBus eventBus, EntityStore entityStore) {
        return new EnemySpawnStrategy(pos -> EnemyFactory.create(id, pos, eventBus, entityStore));
    }

    public static EnemySpawnStrategy of(EnemyAssembler assembler, GameEventBus eventBus, EntityStore entityStore) {
        return new EnemySpawnStrategy(pos -> EnemyFactory.create(assembler, pos, eventBus, entityStore));
    }

    public static EnemySpawnStrategy random(GameEventBus eventBus, EntityStore entityStore) {
        EnemyFactory.EnemyId[] types = EnemyFactory.EnemyId.values();
        return new EnemySpawnStrategy(pos -> {
            EnemyFactory.EnemyId type = types[(int)(Math.random() * types.length)];
            return EnemyFactory.create(type, pos, eventBus, entityStore);
        });
    }

    /** Sobrecarga de conveniencia sin bus explícito — para uso en tests sin eventos. */
    public static EnemySpawnStrategy of(EnemyFactory.EnemyId id, EntityStore entityStore) {
        return of(id, null, entityStore);
    }

    public static EnemySpawnStrategy of(EnemyAssembler assembler, EntityStore entityStore) {
        return of(assembler, null, entityStore);
    }

    public static EnemySpawnStrategy random(EntityStore entityStore) {
        return random(null, entityStore);
    }

    private EnemySpawnStrategy(Supplier strategy) {
        this.strategy = strategy;
    }

    @Override
    public Game.Engine.GameObjects create(Vector2D position) {
        return strategy.create(position);
    }
}
