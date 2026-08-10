package Game.World.Spawn.Strategies;

import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyFactory;
import Game.Engine.Events.GameEventBus;
import Game.Engine.GameMath.Logic2D.Vector2D;
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

    public static EnemySpawnStrategy of(EnemyFactory.EnemyId id, GameEventBus eventBus) {
        return new EnemySpawnStrategy(pos -> EnemyFactory.create(id, pos, eventBus));
    }

    public static EnemySpawnStrategy of(EnemyAssembler assembler, GameEventBus eventBus) {
        return new EnemySpawnStrategy(pos -> EnemyFactory.create(assembler, pos, eventBus));
    }

    public static EnemySpawnStrategy random(GameEventBus eventBus) {
        EnemyFactory.EnemyId[] types = EnemyFactory.EnemyId.values();
        return new EnemySpawnStrategy(pos -> {
            EnemyFactory.EnemyId type = types[(int)(Math.random() * types.length)];
            return EnemyFactory.create(type, pos, eventBus);
        });
    }

    /** Sobrecarga de conveniencia sin bus explícito — para uso en tests sin eventos. */
    public static EnemySpawnStrategy of(EnemyFactory.EnemyId id) {
        return of(id, null);
    }

    public static EnemySpawnStrategy of(EnemyAssembler assembler) {
        return of(assembler, null);
    }

    public static EnemySpawnStrategy random() {
        return random(null);
    }

    private EnemySpawnStrategy(Supplier strategy) {
        this.strategy = strategy;
    }

    @Override
    public Game.Engine.GameObjects create(Vector2D position) {
        return strategy.create(position);
    }
}
