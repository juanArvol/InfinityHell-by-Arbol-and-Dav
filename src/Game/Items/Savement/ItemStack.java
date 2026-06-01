package Game.Items.Savement;

import Game.Items.Creation.ItemDefinition;

/**
 * Instancia de un ítem en un inventario: una definición + una cantidad.
 *
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   ItemDefinition → "qué cosa es" (datos estáticos, compartidos)
 *   ItemStack       → "cuántos tengo" (estado por instancia de inventario)
 *
 * Para ítems no apilables (armas, armadura), maxStack=1 garantiza que
 * count siempre sea 1.
 *
 * Para munición (ammo_9mm, maxStack=50), count puede ser 1..50.
 *
 * Uso:
 *   ItemStack bullets = new ItemStack(ammo9mmDef, 30);
 *   ItemStack pistol  = new ItemStack(pistol9mmDef);  // count = 1
 *   bullets.add(10);    // bullets.count = 40
 *   bullets.remove(5);  // bullets.count = 35
 *   bullets.isEmpty();  // false
 */
public class ItemStack {

    private final ItemDefinition definition;
    private int count;

    /** Constructor con count = 1. */
    public ItemStack(ItemDefinition definition) {
        this(definition, 1);
    }

    /** Constructor con count explícito. */
    public ItemStack(ItemDefinition definition, int count) {
        if (definition == null) throw new IllegalArgumentException("definition no puede ser null");
        if (count < 0) throw new IllegalArgumentException("count no puede ser negativo");
        this.definition = definition;
        this.count = Math.min(count, definition.maxStack);
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public ItemDefinition getDefinition() { return definition; }
    public int getCount()                 { return count; }
    public boolean isEmpty()              { return count <= 0; }
    public boolean isFull()              { return count >= definition.maxStack; }
    public int spaceLeft()               { return definition.maxStack - count; }

    // ── Operaciones ───────────────────────────────────────────────────────

    /**
     * Añade hasta `amount` unidades respetando maxStack.
     * @return cuántas unidades NO se pudieron añadir (overflow).
     */
    public int add(int amount) {
        int space = spaceLeft();
        int added = Math.min(amount, space);
        count += added;
        return amount - added;
    }

    /**
     * Elimina hasta `amount` unidades sin pasar de 0.
     * @return cuántas unidades se eliminaron realmente.
     */
    public int remove(int amount) {
        int removed = Math.min(amount, count);
        count -= removed;
        return removed;
    }

    /**
     * Transfiere unidades desde otro stack compatible (misma definición).
     * @return cuántas se transfirieron.
     */
    public int transferFrom(ItemStack other) {
        if (other.definition != this.definition) {
            throw new IllegalArgumentException("No se puede transferir entre distintas definiciones");
        }
        int toMove = Math.min(other.count, spaceLeft());
        other.remove(toMove);
        add(toMove);
        return toMove;
    }

    @Override
    public String toString() {
        return definition.displayName + " x" + count;
    }
}
