package Game.Items.Savement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import Game.Items.Creation.ItemType;

/**
 * Inventario de ítems — contenedor de ItemStacks.
 *
 * DISEÑO SIMPLE (suficiente para empezar):
 *   - Lista de slots. Cada slot puede tener un ItemStack o estar vacío (null).
 *   - addItem() primero intenta apilar en stacks existentes del mismo tipo,
 *     luego busca un slot vacío.
 *   - Extensible: se puede subclasificar (PlayerInventory, ContainerInventory)
 *     sin cambiar esta clase base.
 *
 * No implementa "drag & drop" ni lógica de UI — eso es responsabilidad
 * de la capa de presentación.
 *
 * Uso:
 *   Inventory inv = new Inventory(20); // 20 slots
 *   inv.addItem(new ItemStack(pistolDef));
 *   inv.addItem(new ItemStack(ammo9mmDef, 15));
 *   Optional<ItemStack> found = inv.findByType(ItemType.FIREARM);
 */
public class Inventory {

    private final List<ItemStack> slots;
    private final int capacity;

    public Inventory(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity debe ser > 0");
        this.capacity = capacity;
        this.slots = new ArrayList<>(Collections.nCopies(capacity, null));
    }

    // ── Añadir ────────────────────────────────────────────────────────────

    /**
     * Añade un ItemStack al inventario.
     * Primero intenta apilar en stacks existentes del mismo tipo.
     * Luego busca slots vacíos.
     *
     * @param stack ítem a añadir
     * @return true si se añadió todo, false si el inventario está lleno
     *         y quedó overflow (el stack queda reducido con el sobrante)
     */
    public boolean addItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return true;

        // Paso 1: intentar apilar en stacks existentes del mismo tipo
        for (ItemStack existing : slots) {
            if (existing != null
                    && existing.getDefinition() == stack.getDefinition()
                    && !existing.isFull()) {
                int overflow = existing.transferFrom(stack) == 0
                        ? stack.getCount() : 0;
                // transferFrom modifica `stack` directamente
                if (stack.isEmpty()) return true;
            }
        }

        // Paso 2: buscar slot vacío
        for (int i = 0; i < capacity; i++) {
            if (slots.get(i) == null) {
                slots.set(i, stack);
                return true;
            }
        }

        // Inventario lleno — hay overflow
        return false;
    }

    // ── Buscar ────────────────────────────────────────────────────────────

    /** Primer stack de cierto tipo de ítem, o empty si no hay. */
    public Optional<ItemStack> findByType(ItemType type) {
        for (ItemStack s : slots) {
            if (s != null && s.getDefinition().type == type) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /** Primer stack con cierto ID de definición, o empty si no hay. */
    public Optional<ItemStack> findById(String id) {
        for (ItemStack s : slots) {
            if (s != null && s.getDefinition().id.equals(id)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    // ── Eliminar ──────────────────────────────────────────────────────────

    /**
     * Elimina `amount` unidades de un ítem identificado por ID.
     * Si el stack queda vacío, libera el slot.
     * @return cuántas unidades se eliminaron.
     */
    public int removeItem(String id, int amount) {
        int remaining = amount;
        for (int i = 0; i < capacity && remaining > 0; i++) {
            ItemStack s = slots.get(i);
            if (s != null && s.getDefinition().id.equals(id)) {
                remaining -= s.remove(remaining);
                if (s.isEmpty()) slots.set(i, null);
            }
        }
        return amount - remaining;
    }

    // ── Estado ────────────────────────────────────────────────────────────

    public int getCapacity()    { return capacity; }
    public ItemStack getSlot(int i) { return slots.get(i); }

    /** Slots ocupados actualmente. */
    public int usedSlots() {
        int used = 0;
        for (ItemStack s : slots) if (s != null) used++;
        return used;
    }

    public boolean isFull()  { return usedSlots() >= capacity; }
    public boolean isEmpty() { return usedSlots() == 0; }

    /** Vista inmutable de los slots (para UI/render). */
    public List<ItemStack> getSlots() {
        return Collections.unmodifiableList(slots);
    }

    // ── Peso total ────────────────────────────────────────────────────────

    /** Peso total del inventario en kg. */
    public double getTotalWeight() {
        double total = 0;
        for (ItemStack s : slots) {
            if (s != null) {
                total += s.getDefinition().weight * s.getCount();
            }
        }
        return total;
    }
}
