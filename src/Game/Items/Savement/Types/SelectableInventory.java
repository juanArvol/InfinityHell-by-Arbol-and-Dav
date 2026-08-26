package Game.Items.Savement.Types;

import Game.Engine.GameEventBus;
import Game.Gameplay.Events.InventoryEvents;

/**
 * Inventario que permite seleccionar uno de sus elementos.
 *
 * @param <T> tipo de elemento almacenado
 */
public class SelectableInventory<T> extends Inventory<T> {

    /**
     * Índice del elemento actualmente seleccionado.
     */
    protected int currentIndex = 0;

    /**
     * Bus de eventos opcional.
     */
    private final GameEventBus eventBus;

    public SelectableInventory() {
        this(null);
    }

    public SelectableInventory(GameEventBus eventBus) {
        super();
        this.eventBus = eventBus;
    }

    public SelectableInventory(int maxSlots) {
        this(maxSlots, null);
    }

    public SelectableInventory(
            int maxSlots,
            GameEventBus eventBus
    ) {
        super(maxSlots);
        this.eventBus = eventBus;
    }

    /**
     * Obtiene el elemento actualmente seleccionado.
     *
     * @return elemento seleccionado, o null si el inventario está vacío.
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

        T previous = getCurrent();

        currentIndex =
                (currentIndex + 1) % inventoryItem.size();

        emitSelectionChange(
                previous,
                getCurrent()
        );
    }

    /**
     * Retrocede al elemento anterior en ciclo circular.
     */
    public void previous() {

        if (inventoryItem.size() <= 1) {
            return;
        }

        T previous = getCurrent();

        currentIndex =
                (currentIndex - 1 + inventoryItem.size())
                        % inventoryItem.size();

        emitSelectionChange(
                previous,
                getCurrent()
        );
    }

    /**
     * Selecciona directamente el elemento indicado.
     *
     * @param index índice del elemento a seleccionar
     */
    public void selectAt(int index) {

        if (index < 0 || index >= inventoryItem.size()) {
            throw new IndexOutOfBoundsException(
                    "índice fuera de rango: " + index
            );
        }

        if (index == currentIndex) {
            return;
        }

        T previous = getCurrent();

        currentIndex = index;

        emitSelectionChange(
                previous,
                getCurrent()
        );
    }

    /**
     * Obtiene el índice actualmente seleccionado.
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

    /**
     * Elimina un elemento preservando, cuando es posible,
     * el elemento que estaba seleccionado.
     */
    @Override
    public boolean removeItem(T item) {

        int removedIndex = inventoryItem.indexOf(item);

        if (removedIndex < 0) {
            return false;
        }

        T previousSelection = getCurrent();

        boolean removed = super.removeItem(item);

        if (!removed) {
            return false;
        }

        /*
         * Si eliminamos un elemento anterior al seleccionado,
         * el índice debe desplazarse una posición para continuar
         * apuntando al mismo objeto.
         */
        if (!inventoryItem.isEmpty()
                && removedIndex < currentIndex) {

            currentIndex--;
        }

        clampIndex();

        T currentSelection = getCurrent();

        if (previousSelection != currentSelection) {
            emitSelectionChange(
                    previousSelection,
                    currentSelection
            );
        }

        return true;
    }

    /**
     * Elimina el elemento situado en el índice indicado.
     */
    @Override
    public T removeItemAt(int index) {

        if (index < 0 || index >= inventoryItem.size()) {
            throw new IndexOutOfBoundsException(
                    "índice fuera de rango: " + index
            );
        }

        T previousSelection = getCurrent();

        T removed = super.removeItemAt(index);

        /*
         * Si se eliminó una posición anterior al elemento seleccionado,
         * el índice seleccionado se desplaza una posición hacia atrás.
         */
        if (!inventoryItem.isEmpty()
                && index < currentIndex) {

            currentIndex--;
        }

        clampIndex();

        T currentSelection = getCurrent();

        if (previousSelection != currentSelection) {
            emitSelectionChange(
                    previousSelection,
                    currentSelection
            );
        }

        return removed;
    }

    /**
     * Vacía completamente el inventario.
     *
     * Si había un elemento seleccionado, se emite un cambio hacia null.
     */
    @Override
    public void clear() {

        T previousSelection = getCurrent();

        super.clear();

        currentIndex = 0;

        if (previousSelection != null) {
            emitSelectionChange(
                    previousSelection,
                    null
            );
        }
    }

    /**
     * Emite el evento genérico de cambio de selección.
     */
    protected void emitSelectionChange(
            T previous,
            T current
    ) {

        if (eventBus == null) {
            return;
        }

        if (!eventBus.hasListeners(
                InventoryEvents.OnSelectionChange.class
        )) {
            return;
        }

        eventBus.post(
                new InventoryEvents.OnSelectionChange<>(
                        previous,
                        current
                )
        );
    }
}