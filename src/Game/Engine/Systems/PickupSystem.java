package Game.Engine.Systems;

import Game.Engine.Events.GameEventBus;
import Game.Engine.Events.OnPickupEvent;                // ← standalone, fuente de verdad
import Game.Items.Savement.ItemStack;
import Game.Player.Player;
import Game.World.WorldObjects.WorldItem;

/**
 * Sistema de pickup — gestiona la lógica de recoger ítems del mundo.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * CORRECCIÓN DE CONTRATO DE EVENTO
 *
 * PROBLEMA ANTERIOR:
 *   PickupSystem emitía:
 *     GameEventBus.post(new OnPickupEvent(...))
 *   importando Game.Engine.Events.OnPickupEvent (standalone — clase A).
 *
 *   Si algún listener suscribía GameEvents.OnPickupEvent (clase B — internal),
 *   el evento NUNCA llegaba. Compilación correcta, bug silencioso en runtime.
 *
 * CAUSA RAÍZ:
 *   Dos clases distintas para el mismo evento. Ver GameEvents.java.
 *
 * SOLUCIÓN:
 *   Fuente de verdad única: Game.Engine.Events.OnPickupEvent (standalone).
 *   GameEvents.OnPickupEvent fue eliminado.
 *   Este archivo importa el standalone → sin cambio funcional.
 *
 * Se cambia GameEventBus.post() estático por GameEventBus.GLOBAL.post()
 * para hacer explícito que se usa la instancia global, facilitando
 * la migración futura a instancias por escena.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * ARQUITECTURA (sin cambios)
 *
 * Player NO debe saber sobre WorldItem. WorldItem NO debe saber sobre Inventory.
 * PickupSystem es el mediador entre los dos.
 *
 * FLUJO:
 *   1. Player colisiona con WorldItem → WorldItem.onCollisionWith(player)
 *   2. WorldItem llama PickupSystem.handlePickup(player, worldItem)
 *   3. PickupSystem intenta añadir el ItemStack al inventario del Player
 *   4. Si el inventario acepta todo → WorldItem.markForRemoval()
 *   5. Si acepta parcial → WorldItem queda con el sobrante
 *   6. En todos los casos que haya pickup se dispara OnPickupEvent
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

        int before = stack.getCount();

        boolean fullyAdded = inventory.addItem(stack);

        int after    = stack.getCount();
        int pickedUp = before - after;

        if (pickedUp > 0) {
            // Emitir usando el STANDALONE OnPickupEvent — fuente de verdad única.
            GameEventBus.GLOBAL.post(new OnPickupEvent(player, stack.getDefinition(), pickedUp));
        }

        if (fullyAdded || stack.isEmpty()) {
            worldItem.markForRemoval();
        }
        // Si no: el WorldItem sigue en el mundo con el sobrante
    }
}
