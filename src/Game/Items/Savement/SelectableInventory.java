package Game.Items.Savement;

public class SelectableInventory<T> extends Inventory<T> {

    protected int currentIndex = 0;

    public SelectableInventory() {
        super();
    }

    public SelectableInventory(int maxSlots) {
        super(maxSlots);
    }

    /**
     * Obtiene el elemento actualmente seleccionado.
     *
     * @return elemento seleccionado, o null si está vacío.
     */
    public T getCurrent() {

        if (inventoryItem.isEmpty()) {
            return null;
        }

        return inventoryItem.get(currentIndex);
    }

    /**
     * Avanza al siguiente elemento en ciclo circular.
     */
    public void next() {

        if (inventoryItem.size() <= 1) {
            return;
        }

        currentIndex =
                (currentIndex + 1) % inventoryItem.size();
    }

    /**
     * Retrocede al elemento anterior en ciclo circular.
     */
    public void previous() {

        if (inventoryItem.size() <= 1) {
            return;
        }

        currentIndex =
                (currentIndex - 1 + inventoryItem.size())
                        % inventoryItem.size();
    }

    /**
     * Selecciona directamente el elemento indicado.
     */
    public void selectAt(int index) {

        if (index < 0 || index >= inventoryItem.size()) {
            throw new IndexOutOfBoundsException(
                    "índice fuera de rango: " + index
            );
        }

        currentIndex = index;
    }

    /**
     * Índice del elemento actualmente seleccionado.
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Mantiene currentIndex dentro del rango válido.
     */
    protected void clampIndex() {

        if (inventoryItem.isEmpty()) {
            currentIndex = 0;
        } else if (currentIndex < 0) {
            currentIndex = 0;
        } else if (currentIndex >= inventoryItem.size()) {
            currentIndex = inventoryItem.size() - 1;
        }
    }

    @Override
    public boolean remove(T item) {

        boolean removed = super.remove(item);

        if (removed) {
            clampIndex();
        }

        return removed;
    }

    @Override
    public T removeAt(int index) {

        T removed = super.removeAt(index);

        clampIndex();

        return removed;
    }

    @Override
    public void clear() {

        super.clear();

        currentIndex = 0;
    }
}