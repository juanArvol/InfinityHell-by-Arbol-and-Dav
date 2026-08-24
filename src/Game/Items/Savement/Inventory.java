package Game.Items.Savement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventario genérico — clase base para inventarios de Items.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * PROPÓSITO:
 *   Clase base genérica que unifica el concepto de inventario para todos
 *   los tipos de Items. También puede usarse directamente como inventario
 *   genérico para ItemStack.
 *
 * DISEÑO:
 *   Inventory<T> permite a las subclases definir:
 *     - El tipo de objeto que almacena (T)
 *     - Lógica específica de adición/remoción
 *     - Comportamiento de unicidad o acumulación
 *
 * PATRÓN:
 *   Las subclases concretas (WeaponInventory, AmuletInventory, etc.) heredan
 *   de esta clase y especifican su tipo:
 *     - WeaponInventory extends Inventory<ModifiedWeapon>
 *     - AmuletInventory extends Inventory<AmuletDefinition>
 *   
 *   También puede usarse directamente:
 *     - Inventory<ItemStack> para items stackeables genéricos
 *
 * @param <T> tipo de objeto almacenado en el inventario
 */
public class Inventory<T> {

    /** Items almacenados. Protected para que subclases accedan directamente. */
    protected final List<T> items = new ArrayList<>();
    
    /** Máximo de items permitidos. */
    protected final int maxSlots;

    /**
     * Constructor sin límite de slots.
     */
    public Inventory() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Constructor con límite de slots.
     *
     * @param maxSlots máximo de items permitidos
     */
    public Inventory(int maxSlots) {
        this.maxSlots = maxSlots;
    }

    // ── Operaciones básicas ───────────────────────────────────────────────

    /**
     * Añade un item al inventario.
     * Las subclases pueden sobrescribir para implementar unicidad o stacking.
     *
     * @param item item a añadir. No puede ser null.
     * @return true si se añadió, false si no
     * @throws IllegalArgumentException si item es null
     */
    public boolean add(T item) {
        if (item == null)
            throw new IllegalArgumentException("item no puede ser null");
        return items.add(item);
    }

    /**
     * Añade un ItemStack al inventario (para inventarios genéricos).
     * Si ya existe un stack de la misma definición, incrementa su count.
     * Si no, crea un nuevo slot.
     *
     * @param stack stack a añadir
     * @return true si se añadió completamente, false si hubo overflow
     */
    @SuppressWarnings("unchecked")
    public boolean addItem(Object stack) {
        if (!(stack instanceof Game.Items.Savement.ItemStack)) return false;
        Game.Items.Savement.ItemStack itemStack = (Game.Items.Savement.ItemStack) stack;
        
        if (itemStack.isEmpty()) return false;

        // Buscar stack existente de la misma definición
        for (T existing : items) {
            if (existing instanceof Game.Items.Savement.ItemStack) {
                Game.Items.Savement.ItemStack existingStack = (Game.Items.Savement.ItemStack) existing;
                if (existingStack.getDefinition().equals(itemStack.getDefinition())) {
                    existingStack.add(itemStack.getCount());
                    return true;
                }
            }
        }

        // No existe, crear nuevo slot
        items.add((T) itemStack);
        return true;
    }

    /**
     * Elimina un item específico del inventario.
     *
     * @param item item a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean remove(T item) {
        return items.remove(item);
    }

    /**
     * Elimina el item en el índice especificado.
     *
     * @param index índice del item a eliminar (0-based)
     * @return el item eliminado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public T removeAt(int index) {
        return items.remove(index);
    }

    /**
     * Verifica si el inventario contiene un item específico.
     *
     * @param item item a verificar
     * @return true si se posee
     */
    public boolean contains(T item) {
        return items.contains(item);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    /**
     * Obtiene un item por su índice.
     *
     * @param index índice del item (0-based)
     * @return el item en el índice especificado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public T get(int index) {
        return items.get(index);
    }

    /**
     * Lista inmutable de todos los items.
     *
     * @return lista de items. Nunca null, puede estar vacía.
     */
    public List<T> getAll() {
        return Collections.unmodifiableList(items);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** Número total de items almacenados. */
    public int size() {
        return items.size();
    }

    /** True si no hay items. */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia todos los items — útil para testing o reinicios de run.
     */
    public void clear() {
        items.clear();
    }

    // ── Object identity ───────────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{items=" + items.size() + "}";
    }
}
