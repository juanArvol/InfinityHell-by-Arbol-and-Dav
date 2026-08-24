package Game.Items.Savement;

import Game.Items.ItemDefinition;
import Game.Items.ItemType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Inventario genérico — almacena ItemStacks (ItemDefinition + cantidad).
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * Inventory trabaja con ItemDefinition ABSTRACTO como puente.
 * Las definiciones concretas (WeaponDefinition, BulletDefinition, etc.)
 * se almacenan polimórficamente.
 *
 * PATRÓN: Slots dinámicos — crece según se añaden items.
 *
 * Auto-stacking: si se añade un item ya existente, incrementa count
 * en lugar de crear nuevo slot.
 *
 * Uso:
 *   Inventory inv = new Inventory();
 *   inv.addItem(new ItemStack(weaponDef, 1));
 *   inv.addItem(new ItemStack(bulletDef, 50));
 *   Optional<ItemStack> weapon = inv.findByType(ItemType.WEAPON);
 */
public class Inventory {

    private final List<ItemStack> slots;
    private final int maxSlots;

    public Inventory() {
        this(Integer.MAX_VALUE); // Sin límite por defecto
    }

    public Inventory(int maxSlots) {
        this.slots = new ArrayList<>();
        this.maxSlots = maxSlots;
    }

    // ── Añadir items ──────────────────────────────────────────────────────

    /**
     * Añade un ItemStack al inventario.
     * Si ya existe un stack de la misma definición, incrementa su count.
     * Si no, crea un nuevo slot.
     *
     * @return true si se añadió completamente, false si hubo overflow
     */
    public boolean addItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        // Buscar stack existente de la misma definición
        for (ItemStack existing : slots) {
            if (existing.getDefinition().equals(stack.getDefinition())) {
                existing.add(stack.getCount());
                return true;
            }
        }

        // No existe, crear nuevo slot
        slots.add(new ItemStack(stack.getDefinition(), stack.getCount()));
        return true;
    }

    // ── Búsqueda ──────────────────────────────────────────────────────────

    /**
     * Encuentra el primer ItemStack del tipo especificado.
     */
    public Optional<ItemStack> findByType(ItemType type) {
        return slots.stream()
                .filter(stack -> stack.getDefinition() instanceof ItemDefinition)
                .findFirst();
    }

    /**
     * Encuentra un ItemStack por ID de definición.
     */
    public Optional<ItemStack> findById(String id) {
        return slots.stream()
                .filter(stack -> stack.getDefinition().getId().equals(id))
                .findFirst();
    }

    // ── Eliminación ───────────────────────────────────────────────────────

    /**
     * Elimina un ItemStack específico del inventario.
     */
    public boolean removeItem(ItemStack stack) {
        return slots.remove(stack);
    }

    /**
     * Elimina cantidad de un item por ID.
     * @return true si se eliminó completamente, false si no había suficiente
     */
    public boolean removeById(String id, int amount) {
        Optional<ItemStack> found = findById(id);
        if (found.isEmpty()) return false;

        ItemStack stack = found.get();
        int removed = stack.remove(amount);
        
        if (stack.isEmpty()) {
            slots.remove(stack);
        }
        
        return removed == amount;
    }

    // ── Consulta ──────────────────────────────────────────────────────────

    public int size() { return slots.size(); }
    public boolean isEmpty() { return slots.isEmpty(); }
    public void clear() { slots.clear(); }
    
    public ItemStack getSlot(int i) { return slots.get(i); }

    /**
     * Cuenta cuántos items de un tipo específico hay en total.
     */
    public int countByType(ItemType type) {
        return slots.stream()
                .filter(stack -> stack.getDefinition() instanceof ItemDefinition)
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    public List<ItemStack> getSlots() {
        return new ArrayList<>(slots);
    }

    @Override
    public String toString() {
        return "Inventory{slots=" + slots.size() + "}";
    }
}
