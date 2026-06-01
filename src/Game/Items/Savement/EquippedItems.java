package Game.Items.Savement;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ítems actualmente equipados en el jugador.
 *
 * SEPARACIÓN: EquippedItems es distinto del Inventory.
 *   Inventory → todos los ítems que el jugador carga.
 *   EquippedItems → lo que el jugador tiene "en mano" ahora mismo.
 *
 * Un ítem equipado sigue existiendo en el Inventory (no se consume al equipar).
 * La lógica de "quitarlo del inventario al equiparlo" es responsabilidad del
 * sistema de UI o del PlayerCombat si decide implementarlo.
 *
 * Uso:
 *   EquippedItems equipped = new EquippedItems();
 *   equipped.equip(EquipmentSlot.PRIMARY_WEAPON, new ItemStack(pistolDef));
 *   Optional<ItemStack> primary = equipped.getEquipped(EquipmentSlot.PRIMARY_WEAPON);
 */
public class EquippedItems {

    private final Map<EquipmentSlot, ItemStack> slots = new EnumMap<>(EquipmentSlot.class);

    /** Equipa un ítem en el slot dado. Reemplaza el anterior si había uno. */
    public void equip(EquipmentSlot slot, ItemStack item) {
        slots.put(slot, item);
    }

    /** Quita el ítem del slot. */
    public void unequip(EquipmentSlot slot) {
        slots.remove(slot);
    }

    /** Devuelve el ítem equipado en el slot, si existe. */
    public Optional<ItemStack> getEquipped(EquipmentSlot slot) {
        return Optional.ofNullable(slots.get(slot));
    }

    /** True si hay algo equipado en ese slot. */
    public boolean hasEquipped(EquipmentSlot slot) {
        return slots.containsKey(slot);
    }

    /** Intercambia dos slots (útil para swap de arma rápida). */
    public void swap(EquipmentSlot slotA, EquipmentSlot slotB) {
        ItemStack tmp = slots.get(slotA);
        if (slots.containsKey(slotB)) {
            slots.put(slotA, slots.get(slotB));
        } else {
            slots.remove(slotA);
        }
        if (tmp != null) {
            slots.put(slotB, tmp);
        } else {
            slots.remove(slotB);
        }
    }
}
