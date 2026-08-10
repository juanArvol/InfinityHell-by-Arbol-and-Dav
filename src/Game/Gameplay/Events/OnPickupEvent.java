package Game.Gameplay.Events;

import Game.Items.Creation.ItemDefinition;
import Game.Player.Player;

/**
 * Evento emitido cuando el jugador recoge un ítem del mundo.
 *
 * Vive en Game.Items porque encapsula tipos concretos del Game
 * (Player, ItemDefinition). El Engine no debe conocer esos tipos.
 *
 * MIGRADO DESDE: Game.Engine.Events.OnPickupEvent
 * RAZÓN: los campos Player y ItemDefinition son tipos del Game.
 * Tenerlos en el Engine creaba dependencias Engine → Game.Player y
 * Engine → Game.Items.
 *
 * Emisores: PickupSystem
 * Listeners típicos: InventoryHUD, AudioSystem, QuestSystem
 */
public record OnPickupEvent(Player player, ItemDefinition definition, int amount) {}
