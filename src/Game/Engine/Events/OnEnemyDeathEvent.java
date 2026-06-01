package Game.Engine.Events;

import Game.Enemys.Enemy;
import Game.Engine.GameMath.SpaceLogic.Logic2D.Vector2D;

/**
 * Evento emitido cuando un enemigo llega a 0 HP y muere.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * PROBLEMA DETECTADO — CONTRATO DIVERGENTE (igual que OnPickupEvent)
 *
 * Existían dos definiciones:
 *
 *   (A) Game.Engine.Events.OnEnemyDeathEvent  (este archivo — standalone)
 *       Campos: Enemy enemy, Vector2D position
 *
 *   (B) Game.Engine.Events.GameEvents.OnEnemyDeathEvent  (record interno)
 *       Campos: Enemy enemy, Vector2D position
 *
 * LootSystem importaba la clase interna de GameEvents:
 *   import Game.Engine.Events.GameEvents.OnEnemyDeathEvent;
 *   GameEventBus.subscribe(OnEnemyDeathEvent.class, ...)
 *
 * Si otro sistema emitía usando la clase standalone (A), LootSystem
 * nunca recibía el evento → loot system completamente inoperativo
 * sin ningún error de compilación visible.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAUSA RAÍZ
 *
 * Mismo problema que OnPickupEvent: refactor incompleto que dejó ambas
 * definiciones activas. La divergencia puede ocurrir silenciosamente porque:
 *   - Ambas clases compilan correctamente.
 *   - El bus no produce error al suscribir una clase que nadie emite.
 *   - El bug solo se manifiesta en runtime cuando el loot no aparece.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * SOLUCIÓN APLICADA
 *
 * Se establece Game.Engine.Events.OnEnemyDeathEvent (este archivo) como
 * ÚNICA fuente de verdad.
 *
 * Acción requerida:
 *   1. En GameEvents.java: ELIMINAR el record interno OnEnemyDeathEvent.
 *   2. En LootSystem.java: cambiar el import de:
 *        import Game.Engine.Events.GameEvents.OnEnemyDeathEvent;
 *      a:
 *        import Game.Engine.Events.OnEnemyDeathEvent;
 *      (o eliminarlo si ya estaba importado correctamente)
 *   3. En cualquier emisor (ej: Enemy, EnemyManager): verificar qué clase importan.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * LISTENERS RECOMENDADOS
 *
 *   - LootSystem    → spawnear WorldItems en position
 *   - AudioSystem   → reproducir sonido de muerte
 *   - FXSystem      → partículas en position
 *   - WaveSpawner   → decrementar contador de vivos
 *   - ScoreSystem   → sumar puntos
 */
public record OnEnemyDeathEvent(Enemy enemy, Vector2D position) {}
