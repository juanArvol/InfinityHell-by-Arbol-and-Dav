package Game.Events;

import Game.Enemys.Enemy;
import GameMath.Vector2D;

/**
 * Evento emitido cuando un enemigo muere.
 *
 * Listeners típicos:
 *   - AudioSystem     → reproducir sonido de muerte
 *   - FXSystem        → partículas/sangre en position
 *   - LootSystem      → spawnear WorldItems en position
 *   - WaveSpawner     → decrementar contador de enemigos vivos
 *   - ScoreSystem     → sumar puntos
 *
 * Ninguno de esos sistemas necesita conocer Enemy directamente —
 * con la posición y referencia opcional es suficiente para la mayoría.
 */
public record OnEnemyDeathEvent(Enemy enemy, Vector2D position) {}
