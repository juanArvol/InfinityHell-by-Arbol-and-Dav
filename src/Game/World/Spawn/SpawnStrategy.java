package Game.World.Spawn;

import Game.Engine.GameObjects;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Estrategia de construcción de objetos a spawnear.
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 * Una SpawnStrategy sabe CÓMO construir el objeto que va a aparecer en el mundo.
 * No decide cuándo ni dónde — eso es SpawnCondition y SpawnPoint.
 *
 * ── EJEMPLOS DE IMPLEMENTACIÓN ────────────────────────────────────────────
 *   EnemySpawnStrategy         → construye un Enemy via EnemyAssembler
 *   RandomEnemySpawnStrategy   → construye un Enemy aleatorio
 *   BossSpawnStrategy          → construye un Boss con parámetros de fase
 *   ItemDropSpawnStrategy      → construye un WorldItem
 *   ProjectileSpawnStrategy    → construye un Bullet
 *   ScriptedSpawnStrategy      → ejecuta lógica custom antes de construir
 *
 * ── CONTRATO ──────────────────────────────────────────────────────────────
 * create(position) recibe la posición ya resuelta por SpawnPoint.samplePosition()
 * y retorna el GameObjects listo para ser añadido al mundo.
 *
 * Retornar null indica que la estrategia no pudo construir el objeto
 * (recurso no disponible, pool agotado, etc.). SpawnSystem omitirá ese spawn
 * sin lanzar excepción.
 */
@FunctionalInterface
public interface SpawnStrategy {

    /**
     * Construye el objeto a spawnear en la posición dada.
     *
     * @param position posición resuelta por SpawnPoint — en coordenadas de mundo.
     * @return el GameObjects a añadir al mundo, o null si no se puede construir.
     */
    GameObjects create(Vector2D position);
}
