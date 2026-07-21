package Game.World.Systems;

import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.OnPickupEvent;
import Game.Items.Savement.ItemStack;
import Game.Player.Player;
import Game.World.WorldObjects.WorldItem;

/**
 * Sistema de pickup — gestiona la lógica de recoger ítems del mundo.
 *
 * Vive en Game.World.Systems porque orquesta Player, WorldItem e Inventory —
 * todos conceptos del Game. No es infraestructura reutilizable del Engine.
 *
 * MIGRADO DESDE: Game.Engine.Systems.PickupSystem
 * RAZÓN: PickupSystem importa Player, WorldItem, ItemStack — tipos del Game.
 * Tenerlo en el Engine creaba dependencias Engine → Game.
 *
 * FLUJO:
 *   1. WorldItem.onCollisionWith(GameObjects) detecta instanceof Player
 *   2. Llama PickupSystem.handlePickup(player, worldItem)
 *   3. PickupSystem intenta añadir el ItemStack al inventario del Player
 *   4. Si el inventario acepta todo → WorldItem.markForRemoval()
 *   5. En todos los casos que haya pickup se dispara OnPickupEvent
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

        int before = stack.getCount();

        boolean fullyAdded = inventory.addItem(stack);

        int after    = stack.getCount();
        int pickedUp = before - after;

        if (pickedUp > 0) {
            GameEventBus.GLOBAL.post(new OnPickupEvent(player, stack.getDefinition(), pickedUp));
        }

        if (fullyAdded || stack.isEmpty()) {
            worldItem.markForRemoval();
        }
    }
}
