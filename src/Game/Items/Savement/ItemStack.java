package Game.Items.Savement;

import Game.Items.Creation.ItemDefinition;

/**
 * Instancia de un ítem en un inventario: una definición + una cantidad.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * ItemStack trabaja con ItemDefinition ABSTRACTO como puente a las
 * definiciones concretas (WeaponDefinition, BulletDefinition, AmuletDefinition).
 *
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   ItemDefinition → "qué cosa es" (datos estáticos, compartidos)
 *   ItemStack      → "cuántos tengo" (estado por instancia de inventario)
 *
 * Para ítems no apilables (armas, armadura), usar count=1.
 * Para munición/recursos, count puede ser mayor.
 *
 * Uso:
 *   ItemStack potions = new ItemStack(potionDef, 3);
 *   ItemStack sword   = new ItemStack(swordDef);  // count = 1
 *   potions.add(2);    // potions.count = 5
 *   potions.remove(1); // potions.count = 4
 *   potions.isEmpty(); // false
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
        this.count = count;
    }

    // ── Acceso ────────────────────────────────────────────────────────────

    public ItemDefinition getDefinition() { return definition; }
    public int getCount()                 { return count; }
    public boolean isEmpty()              { return count <= 0; }
    
    // ── Operaciones ───────────────────────────────────────────────────────

    /**
     * Añade `amount` unidades.
     * @return cuántas unidades NO se pudieron añadir (overflow si hay límite).
     */
    public int add(int amount) {
        count += amount;
        return 0; // Sin límite por ahora
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
        if (!other.definition.equals(this.definition)) {
            throw new IllegalArgumentException("No se puede transferir entre distintas definiciones");
        }
        int toMove = other.count;
        other.remove(toMove);
        add(toMove);
        return toMove;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    @Override
    public String toString() {
        return definition.getDisplayName() + " x" + count;
    }
}
