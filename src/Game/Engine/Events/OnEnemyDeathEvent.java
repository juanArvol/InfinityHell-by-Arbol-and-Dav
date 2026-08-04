package Game.Engine.Events;

import Game.Enemys.Core.Enemy;
import Game.Engine.GameMath.Logic2D.Vector2D;

/**
 * Evento emitido cuando un Enemy llega a 0 HP y muere.
 *
 * Migrado desde Game.Enemys.OnEnemyDeathEvent al Core del framework.
 *
 * ── Listeners típicos ────────────────────────────────────────────────────
 *   LootSystem   → spawnear WorldItems en position.
 *   AudioSystem  → reproducir sonido de muerte.
 *   WaveSpawner  → decrementar contador de vivos.
 *   QuestSystem  → registrar kill para objetivos de misión.
 */
public record OnEnemyDeathEvent(Enemy enemy, Vector2D position) {}
