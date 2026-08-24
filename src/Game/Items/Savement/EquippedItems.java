package Game.Items.Savement;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Ítems equipados del jugador — cada slot solo puede tener un ítem.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * EquippedItems trabaja con ItemStack (que usa ItemDefinition abstracto).
 * Las definiciones concretas se manejan polimórficamente.
 *
 * Uso:
 *   EquippedItems equipped = new EquippedItems();
 *   equipped.equip(EquipmentSlot.WEAPON, new ItemStack(weaponDef, 1));
 *   Optional<ItemStack> weapon = equipped.getEquipped(EquipmentSlot.WEAPON);
 */
public class EquippedItems {

    private final Map<EquipmentSlot, ItemStack> slots = new EnumMap<>(EquipmentSlot.class);

    /** Equipa un ítem en el slot especificado. Reemplaza el anterior si existe. */
    public void equip(EquipmentSlot slot, ItemStack item) {
        if (item == null) {
            slots.remove(slot);
        } else {
            slots.put(slot, item);
        }
    }

    /** Retorna el ítem equipado en el slot, si existe. */
    public Optional<ItemStack> getEquipped(EquipmentSlot slot) {
        return Optional.ofNullable(slots.get(slot));
    }

    /** Desequipa el ítem del slot especificado. */
    public Optional<ItemStack> unequip(EquipmentSlot slot) {
        return Optional.ofNullable(slots.remove(slot));
    }

    /** Verifica si hay algo equipado en el slot. */
    public boolean hasEquipped(EquipmentSlot slot) {
        return slots.containsKey(slot);
    }

    /** Limpia todos los slots equipados. */
    public void clear() {
        slots.clear();
    }

    @Override
    public String toString() {
        return "EquippedItems{slots=" + slots.size() + "}";
    }
}
