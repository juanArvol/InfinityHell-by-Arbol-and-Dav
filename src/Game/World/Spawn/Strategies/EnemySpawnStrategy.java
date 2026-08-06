package Game.World.Spawn.Strategies;

import Game.Enemys.Core.Enemy;
import Game.Enemys.Core.EnemyAssembler;
import Game.Enemys.Core.EnemyFactory;
import Game.Engine.GameMath.Logic2D.Vector2D;
import Game.World.Spawn.SpawnStrategy;

/**
 * Estrategia de spawn para enemigos tipados.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Conecta el SpawnSystem genérico con el sistema de enemies concreto.
 * EnemySpawnStrategy sabe construir un Enemy dado un EnemyId o un Assembler.
 *
 * ── EJEMPLOS ──────────────────────────────────────────────────────────────
 *   // Por enum tipado:
 *   SpawnStrategy zombieStrategy = EnemySpawnStrategy.of(EnemyFactory.EnemyId.ZOMBIE);
 *
 *   // Por assembler custom (boss, variante especial):
 *   SpawnStrategy sansStrategy = EnemySpawnStrategy.of(new SansAssembler());
 *
 *   // Aleatorio entre los tipos base:
 *   SpawnStrategy random = EnemySpawnStrategy.random();
 */
public final class EnemySpawnStrategy implements SpawnStrategy {

    private final Supplier strategy;

    @FunctionalInterface
    private interface Supplier {
        Enemy create(Vector2D position);
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /**
     * Estrategia que spawnea un tipo concreto de enemy.
     */
    public static EnemySpawnStrategy of(EnemyFactory.EnemyId id) {
        return new EnemySpawnStrategy(pos -> EnemyFactory.create(id, pos));
    }

    /**
     * Estrategia que usa un assembler custom (Boss, variante especial).
     */
    public static EnemySpawnStrategy of(EnemyAssembler assembler) {
        return new EnemySpawnStrategy(pos -> EnemyFactory.create(assembler, pos));
    }

    /**
     * Estrategia que elige aleatoriamente entre los tipos base del juego.
     */
    public static EnemySpawnStrategy random() {
        EnemyFactory.EnemyId[] types = EnemyFactory.EnemyId.values();
        return new EnemySpawnStrategy(pos -> {
            EnemyFactory.EnemyId type = types[(int)(Math.random() * types.length)];
            return EnemyFactory.create(type, pos);
        });
    }

    private EnemySpawnStrategy(Supplier strategy) {
        this.strategy = strategy;
    }

    @Override
    public Game.Engine.GameObjects create(Vector2D position) {
        return strategy.create(position);
    }
}
