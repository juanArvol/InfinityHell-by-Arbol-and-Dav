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
 * ── LÍMITE DE SLOTS ──────────────────────────────────────────────────────
 *
 * maxSlots controla el número máximo de SLOTS (posiciones) en el inventario,
 * NO la cantidad total de items.
 *
 * EJEMPLO:
 *   Inventory<ItemStack> inv = new Inventory<>(3);  // 3 slots
 *   inv.add(new ItemStack(potion, 50));  // Slot 1: 50 pociones
 *   inv.add(new ItemStack(sword, 1));    // Slot 2: 1 espada
 *   inv.add(new ItemStack(arrow, 999));  // Slot 3: 999 flechas
 *   inv.add(new ItemStack(shield, 1));   // ❌ RECHAZADO: no hay slots
 *
 * NOTA: La implementación base NO verifica maxSlots en add().
 *       Las subclases deben implementar esta validación si la necesitan.
 *
 * PATRÓN:
 *   Las subclases concretas (WeaponInventory, AmuletInventory, etc.) heredan
 *   de esta clase y especifican su tipo:
 *     - WeaponInventory extends Inventory<ModifiedWeapon>
 *     - AmuletInventory extends Inventory<ItemDefinition>
 *     - BulletInventory extends Inventory<BulletType>
 *   
 *   También puede usarse directamente:
 *     - Inventory<ItemStack> para items stackeables genéricos
 *     - ItemStackInventory para stacking automático
 *
 * @param <T> tipo de objeto almacenado en el inventario
 * @see ItemStackInventory para inventario con stacking automático
 */
public class Inventory<T> {

    /** Items almacenados. Protected para que subclases accedan directamente. */
    protected final List<T> inventoryItem = new ArrayList<>();
    
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
    public boolean addItem(T item) {
        if (item == null)
            throw new IllegalArgumentException(item + "no puede ser null");
        
        // Si el inventario está lleno, rechazar
        if (inventoryItem.size() >= maxSlots)
            return false;

        // Verificar duplicidad por identidad
        if (inventoryItem.contains(item))
            return false;
        
        return inventoryItem.add(item);
    }

    /**
     * Elimina un item específico del inventario.
     *
     * @param item item a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean remove(T item) {
        return inventoryItem.remove(item);
    }

    /**
     * Elimina el item en el índice especificado.
     *
     * @param index índice del item a eliminar (0-based)
     * @return el item eliminado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public T removeAt(int index) {
        return inventoryItem.remove(index);
    }

    /**
     * Verifica si el inventario contiene un item específico.
     *
     * @param item item a verificar
     * @return true si se posee
     */
    public boolean contains(T item) {
        return inventoryItem.contains(item);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    /**
     * Obtiene un item por su índice.
     *
     * @param index índice del item (0-based)
     * @return el item en el índice especificado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public T getItem(int index) {
        return inventoryItem.get(index);
    }

    /**
     * Lista inmutable de todos los items.
     *
     * @return lista de items. Nunca null, puede estar vacía.
     */
    public List<T> getAll() {
        return Collections.unmodifiableList(inventoryItem);
    }

    // ── Consultas ─────────────────────────────────────────────────────────

    /** Número total de items almacenados. */
    public int size() {
        return inventoryItem.size();
    }

    /** True si no hay items. */
    public boolean isEmpty() {
        return inventoryItem.isEmpty();
    }

    /* private void clampIndex() {
        if (inventoryItem.isEmpty()) {
            currentIndex = 0;
        } else if (currentIndex >= inventoryItem.size()) {
            currentIndex = inventoryItem.size() - 1;
        }
    } */
    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia todos los items — útil para testing o reinicios de run.
     */
    public void clear() {
        inventoryItem.clear();
    }

    // ── Object identity ───────────────────────────────────────────────────

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{items=" + inventoryItem.size() + "}";
    }
}
