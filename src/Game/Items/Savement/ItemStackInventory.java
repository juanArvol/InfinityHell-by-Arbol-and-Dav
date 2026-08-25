package Game.Items.Savement;

/**
 * Inventario especializado para ItemStack con lógica de apilamiento automático.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO:
 *   Implementación específica de Inventory<ItemStack> que maneja automáticamente
 *   el apilamiento (stacking) de items de la misma definición.
 *
 * COMPORTAMIENTO:
 *   - Si se añade un ItemStack de una definición que ya existe, incrementa el count
 *   - Si no existe, crea un nuevo slot
 *   - Respeta el límite maxSlots del inventario base
 *
 * USO:
 *   ItemStackInventory inv = new ItemStackInventory(20); // 20 slots
 *   inv.add(new ItemStack(potionDef, 3));  // Slot 0: Potion x3
 *   inv.add(new ItemStack(potionDef, 2));  // Slot 0: Potion x5 (apilado)
 *   inv.add(new ItemStack(swordDef));      // Slot 1: Sword x1
 *
 * @see Inventory clase base genérica
 * @see ItemStack item apilable
 */
public class ItemStackInventory extends Inventory<ItemStack> {

    /**
     * Constructor sin límite de slots.
     */
    public ItemStackInventory() {
        super();
    }

    /**
     * Constructor con límite de slots.
     *
     * @param maxSlots máximo de slots permitidos
     */
    public ItemStackInventory(int maxSlots) {
        super(maxSlots);
    }

    // ── Override add() con lógica de stacking ─────────────────────────────

    /**
     * Añade un ItemStack al inventario con apilamiento automático.
     * 
     * COMPORTAMIENTO:
     *   1. Si el stack está vacío, no hace nada (retorna false)
     *   2. Busca un stack existente de la misma definición
     *   3. Si existe, añade la cantidad al stack existente (stacking)
     *   4. Si no existe, crea un nuevo slot (respetando maxSlots)
     *
     * @param stack ItemStack a añadir. No puede ser null.
     * @return true si se añadió completamente, false si el stack estaba vacío
     *         o no había espacio para un nuevo slot
     * @throws IllegalArgumentException si stack es null
     */
    @Override
    public boolean addItem(ItemStack stack) {
        if (stack == null)
            throw new IllegalArgumentException("stack no puede ser null");
        
        if (stack.isEmpty())
            return false;

        // Fase 1: Buscar stack existente de la misma definición
        for (ItemStack existing : inventoryItem) {
            if (existing.getDefinition().equals(stack.getDefinition())) {
                // Stacking: añadir cantidad al stack existente
                existing.add(stack.getCount());
                return true;
            }
        }

        // Fase 2: No existe, crear nuevo slot (si hay espacio)
        if (inventoryItem.size() >= maxSlots)
            return false;
        
        inventoryItem.add(stack);
        return true;
    }

    // ── Métodos de conveniencia específicos para ItemStack ───────────────

    /**
     * Añade items directamente desde una definición.
     * Shorthand para: add(new ItemStack(definition, count))
     *
     * @param definition definición del item
     * @param count cantidad a añadir
     * @return true si se añadió
     */
    public boolean addFromDefinition(Game.Items.Creation.ItemDefinition definition, int count) {
        return addItem(new ItemStack(definition, count));
    }

    /**
     * Elimina una cantidad específica de un item por su definición.
     * Busca el stack correspondiente y reduce su count.
     *
     * @param definition definición del item
     * @param count cantidad a eliminar
     * @return cantidad realmente eliminada
     */
    public int removeByDefinition(Game.Items.Creation.ItemDefinition definition, int count) {
        for (ItemStack stack : inventoryItem) {
            if (stack.getDefinition().equals(definition)) {
                int removed = stack.remove(count);
                
                // Si el stack quedó vacío, eliminarlo del inventario
                if (stack.isEmpty()) {
                    inventoryItem.remove(stack);
                }
                
                return removed;
            }
        }
        return 0; // No se encontró
    }

    /**
     * Obtiene la cantidad total de un item por su definición.
     * Suma todos los stacks de la misma definición (normalmente uno solo).
     *
     * @param definition definición del item
     * @return cantidad total
     */
    public int getCountOf(Game.Items.Creation.ItemDefinition definition) {
        int total = 0;
        for (ItemStack stack : inventoryItem) {
            if (stack.getDefinition().equals(definition)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Verifica si el inventario contiene al menos la cantidad indicada de un item.
     *
     * @param definition definición del item
     * @param minCount cantidad mínima requerida
     * @return true si se posee al menos minCount unidades
     */
    public boolean hasAtLeast(Game.Items.Creation.ItemDefinition definition, int minCount) {
        return getCountOf(definition) >= minCount;
    }

    @Override
    public String toString() {
        return "ItemStackInventory{slots=" + inventoryItem.size() + "/" + maxSlots + "}";
    }
}
