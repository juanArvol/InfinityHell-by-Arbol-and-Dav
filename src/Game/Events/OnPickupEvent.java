package Game.Events;

import Game.Items.ItemDefinition;
import Game.Player.Player;

/**
 * Alias limpio de GameEvents.OnPickupEvent para imports directos.
 * PickupSystem usa este import: import Game.Events.OnPickupEvent;
 */
public record OnPickupEvent(Player player, ItemDefinition definition, int amount) {}
