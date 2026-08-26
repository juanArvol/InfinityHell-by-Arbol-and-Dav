package Game.Items.Savement;

import Game.Items.Creation.ItemDefinition;

/**
 * Instancia de un ítem en un inventario: una definición + una cantidad.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * ItemStack trabaja con ItemDefinition ABSTRACTO como puente a las
 * definiciones concretas (compartidas entre familias de Items).
 *
 * SEPARACIÓN DE RESPONSABILIDADES:
 *   ItemDefinition → "qué cosa es" (datos estáticos, compartidos)
 *   ItemStack      → "cuántos tengo" (estado por instancia de inventario)
 *
 * ── STACKING LIMITS ──────────────────────────────────────────────────────
 *
 * DISEÑO ACTUAL:
 *   ItemStack NO tiene límite máximo de apilamiento por diseño.
 *   Cada ItemStack puede tener count desde 0 hasta Integer.MAX_VALUE.
 *
 * RAZONES:
 *   1. Simplicidad: no todas las definiciones necesitan límites
 *   2. Flexibilidad: el límite puede aplicarse a nivel de inventario
 *   3. Performance: sin checks adicionales en operaciones básicas
 *
 * PARA ITEMS NO APILABLES:
 *   Usar count=1 y manejar unicidad en la capa de inventario.
 *   Ejemplo: WeaponInventory previene duplicados en add()
 *
 * PARA ITEMS APILABLES CON LÍMITE:
 *   Si en el futuro se necesitan límites por definición (ej: "max 99 por stack"),
 *   agregar un campo maxStackSize a ItemDefinition y validar en add().
 *
 * USO:
 *   ItemStack potions = new ItemStack(potionDef, 3);
 *   ItemStack sword   = new ItemStack(swordDef);  // count = 1
 *   potions.add(2);    // potions.count = 5
 *   potions.remove(1); // potions.count = 4
 *   potions.isEmpty(); // false
 */
public class ItemStack {

    private final ItemDefinition definition;
    private int count;

    /** Constructor con count explícito. */
    public ItemStack(ItemDefinition definition, int count) {
        if (definition == null) throw new IllegalArgumentException("definition no puede ser null");
        if (count < 0){this.count = 1;} 
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
    /**
     * Añade unidades.
     *
     * Actualmente no existe límite de stack.
     *
     * @return unidades que no pudieron añadirse.
     */
    public int add(int amount) {

        if (amount < 0) {
            throw new IllegalArgumentException(
                    "amount no puede ser negativo"
            );
        }

        int newCount =
                count + amount;

        if (newCount > Integer.MAX_VALUE) {

            int added =
                    Integer.MAX_VALUE - count;

            count = Integer.MAX_VALUE;

            return amount - added;
        }

        count = (int) newCount;

        return 0;
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
     * Transfiere todas las unidades desde otro stack
     * compatible.
     *
     * @return cantidad realmente transferida.
     */
    public int transferFrom(
            ItemStack other
    ) {

        if (other == null) {
            throw new IllegalArgumentException(
                    "other no puede ser null"
            );
        }

        if (other == this) {
            return 0;
        }

        if (!definition.equals(
                other.definition
        )) {

            throw new IllegalArgumentException(
                    "No se puede transferir entre " +
                    "distintas definiciones"
            );
        }

        int available =
                other.count;

        int overflow =
                add(available);

        int transferred =
                available - overflow;

        other.remove(
                transferred
        );

        return transferred;
    }

    public void setCount(int count) {
        this.count = Math.max(0, count);
    }

    @Override
    public String toString() {
        return definition.getDisplayName() + " x" + count;
    }
}
