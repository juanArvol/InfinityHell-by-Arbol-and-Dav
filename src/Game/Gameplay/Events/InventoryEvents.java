package Game.Gameplay.Events;

/**
 * Catálogo de eventos relacionados con inventarios.
 *
 * ── Inventory System ───────────────────────────────────────────────────────
 *
 * Los eventos de este archivo representan comportamientos genéricos de los
 * inventarios, independientemente del tipo de objeto almacenado.
 */
public final class InventoryEvents {

    private InventoryEvents() {
    }

    /**
     * Emitido cuando cambia el elemento actualmente seleccionado.
     *
     * @param <T> tipo de elemento almacenado en el inventario
     *
     * @param previous elemento anteriormente seleccionado
     * @param current  nuevo elemento seleccionado
     */
    public record OnSelectionChange<T>(
            T previous,
            T current
    ) {
    }
}