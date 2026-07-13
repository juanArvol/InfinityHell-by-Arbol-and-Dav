package Game.Enemys;

import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Evento emitido cuando un enemigo llega a 0 HP y muere.
 *
 * Vive en Game.Enemys porque encapsula un tipo concreto del Game (Enemy).
 * El Engine no debe conocer Enemy — por eso este evento no pertenece al Engine.
 *
 * MIGRADO DESDE: Game.Engine.Events.OnEnemyDeathEvent
 * RAZÓN: el campo Enemy enemy es un tipo del Game. Tenerlo en el Engine
 * creaba una dependencia Engine → Game.Enemys.
 *
 * Listeners típicos:
 *   - LootSystem  → spawnear WorldItems en position
 *   - AudioSystem → reproducir sonido de muerte
 *   - WaveSpawner → decrementar contador de vivos
 */
public record OnEnemyDeathEvent(Enemy enemy, Vector2D position) {}
