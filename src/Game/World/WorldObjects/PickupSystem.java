package Game.World.WorldObjects;

import Game.Events.GameEventBus;
import Game.Events.OnPickupEvent;
import Game.Items.ItemStack;
import Game.Player.Player;
import Game.World.Core.World;
import Game.World.Core.WorldManager;

/**
 * Sistema de pickup — gestiona la lógica de recoger ítems del mundo.
 *
 * ── POR QUÉ SEPARADO ─────────────────────────────────────────────────────
 * Player NO debe saber sobre WorldItem. WorldItem NO debe saber sobre Inventory.
 * PickupSystem es el mediador entre los dos, igual que CollisionsSystem media
 * entre GameObjects.
 *
 * ── FLUJO ────────────────────────────────────────────────────────────────
 *   1. Player colisiona con WorldItem → WorldItem.onCollisionWith(player)
 *   2. WorldItem llama PickupSystem.handlePickup(player, worldItem)
 *   3. PickupSystem intenta añadir el ItemStack al inventario del Player
 *   4. Si el inventario acepta todo → WorldItem.markForRemoval()
 *   5. Si acepta parcial → WorldItem queda con el sobrante
 *   6. Si inventario lleno → no pasa nada
 *   7. En todos los casos que haya pickup se dispara OnPickupEvent
 *
 * La limpieza de WorldItems con pendingRemoval=true la hace World.update()
 * al final del frame (igual que las bullets muertas).
 */
public final class PickupSystem {

    private PickupSystem() {}

    /**
     * Intenta que el jugador recoja el item del world.
     * Modifica el ItemStack del WorldItem si el pickup es parcial.
     * Marca el WorldItem para remoción si el stack queda vacío.
     */
    public static void handlePickup(Player player, WorldItem worldItem) {
        if (player == null || worldItem == null) return;

        var inventory = player.getInventory();
        if (inventory == null) return;

        ItemStack stack = worldItem.getItemStack();
        if (stack == null || stack.isEmpty()) {
            worldItem.markForRemoval();
            return;
        }

        // Guardar cantidad antes de intentar añadir (para saber cuánto se tomó)
        int before = stack.getCount();

        boolean fullyAdded = inventory.addItem(stack);

        int after = stack.getCount();
        int pickedUp = before - after;

        if (pickedUp > 0) {
            // Algo se recogió — disparar evento
            GameEventBus.post(new OnPickupEvent(player, stack.getDefinition(), pickedUp));
        }

        if (fullyAdded || stack.isEmpty()) {
            worldItem.markForRemoval();
        }
        // Si no: el WorldItem sigue en el mundo con el sobrante
    }
}
