package Game.Engine.Events;

import Game.Items.Creation.ItemDefinition;
import Game.Player.Player;

/**
 * Evento emitido cuando el jugador recoge un ítem del mundo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * PROBLEMA DETECTADO — CONTRATO DIVERGENTE
 *
 * Existían dos definiciones del mismo evento semántico:
 *
 *   (A) Game.Engine.Events.OnPickupEvent  (este archivo — standalone record)
 *       Campos: Player player, ItemDefinition definition, int amount
 *
 *   (B) Game.Engine.Events.GameEvents.OnPickupEvent  (record interno)
 *       Campos: Player player, ItemDefinition definition, int amount
 *
 * Aunque los campos son idénticos, son CLASES DISTINTAS en tiempo de ejecución.
 * Consecuencia:
 *   - PickupSystem.java emite: GameEventBus.post(new OnPickupEvent(...))
 *     importando Game.Engine.Events.OnPickupEvent  (clase A).
 *   - Si un listener suscribe GameEvents.OnPickupEvent.class (clase B),
 *     NUNCA recibirá el evento → bug silencioso.
 *   - Viceversa: si suscribe OnPickupEvent.class (A) pero el emitter
 *     usa GameEvents.OnPickupEvent (B), mismo problema.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CAUSA RAÍZ
 *
 * Refactor incompleto: se añadieron los records internos en GameEvents
 * como "catálogo centralizado", pero los archivos standalone preexistentes
 * (OnPickupEvent.java, OnEnemyDeathEvent.java) no fueron eliminados.
 * Resultado: dos contratos para el mismo concepto.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * SOLUCIÓN APLICADA
 *
 * Se establece UNA SOLA clase como fuente de verdad:
 *   → Game.Engine.Events.OnPickupEvent  (este archivo, standalone)
 *
 * Razón de elegir standalone sobre interno:
 *   - PickupSystem ya importa este paquete directamente.
 *   - Los imports standalone son más explícitos y menos propensos a ambigüedad.
 *   - GameEvents puede reexportarlo como alias si se desea consistencia visual.
 *
 * Acción requerida en GameEvents.java:
 *   Cambiar:
 *     public record OnPickupEvent(Player player, ItemDefinition definition, int amount) {}
 *   Por:
 *     // Usar Game.Engine.Events.OnPickupEvent directamente (ver esa clase)
 *     // Alias para conveniencia de import:
 *     // import static o simplemente eliminar el record interno duplicado.
 *
 * Todos los emisores y suscriptores deben usar:
 *   import Game.Engine.Events.OnPickupEvent;
 *
 * ──────────────────────────────────────────────────────────────────────────
 * IMPACTO
 *
 *   - PickupSystem.java: sin cambios (ya importa esta clase).
 *   - Cualquier listener de GameEvents.OnPickupEvent: actualizar import.
 *   - GameEvents.java: eliminar el record interno OnPickupEvent.
 */
public record OnPickupEvent(Player player, ItemDefinition definition, int amount) {}
