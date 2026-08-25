package Game.Items.Types.Weapons;

import Game.Engine.GameEventBus;
import Game.Gameplay.Events.WeaponEvents;
import Game.Items.Savement.Inventory;
import Game.Items.Types.Weapons.WeaponType.WeaponType;

/**
 * Inventario de armas — autoridad del dominio Weapons.
 *
 * ── ARQUITECTURA — Items Module ──────────────────────────────────────────
 *
 * HERENCIA:
 *   WeaponInventory extends Inventory<ModifiedWeapon>
 *   Añade cycling (next/prev/current) y eventos de cambio de arma.
 *
 * RESPONSABILIDADES:
 *   • Almacenar armas poseídas (ModifiedWeapon instances)
 *   • Gestionar adquisición y remoción con unicidad
 *   • Emitir OnWeaponSwitch cuando el arma activa cambia
 *   • Mantener el índice de arma activa y cycling
 *
 * UNICIDAD:
 *   Las armas se obtienen una sola vez por partida. add() verifica duplicados.
 */
public class WeaponInventory extends Inventory<ModifiedWeapon> {

    private int currentIndex = 0;

    /** Bus de eventos para emitir OnWeaponSwitch. Puede ser null. */
    private final GameEventBus eventBus;

    /**
     * Constructor con bus de eventos.
     *
     * @param eventBus bus para emitir OnWeaponSwitch al cambiar de arma.
     *                 Null = no se emiten eventos de cambio.
     */
    public WeaponInventory(GameEventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Constructor sin bus de eventos — sin notificaciones de cambio.
     */
    public WeaponInventory() {
        this.eventBus = null;
    }

    // ── Gestión de armas ──────────────────────────────────────────────────

    /**
     * Añade un arma al inventario.
     * Implementa UNICIDAD: si ya se posee, la operación es no-op.
     * Si es la primera arma, pasa a ser el arma activa automáticamente.
     *
     * @param weapon arma a añadir. No puede ser null.
     * @return true si se añadió (nueva adquisición), false si ya se poseía
     * @throws IllegalArgumentException si weapon es null
     */
    public boolean addWeapon(ModifiedWeapon weapon) {
        return addItem(weapon);
    }

    /**
     * Elimina un arma específica del inventario.
     * Si era el arma activa, currentIndex se ajusta al rango válido.
     *
     * @param weapon arma a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean removeWeapon(ModifiedWeapon weapon) {
        boolean removed = inventoryItem.remove(weapon);
        if (removed) {
            clampIndex();
        }
        return removed;
    }

    /**
     * Elimina el arma en el índice especificado.
     */
    public ModifiedWeapon removeAt(int index) {
        ModifiedWeapon removed = inventoryItem.remove(index);
        clampIndex();
        return removed;
    }

    /**
     * True si el portador posee un arma del tipo indicado.
     */
    public boolean hasWeapon(WeaponType weaponType) {
        if (weaponType == null) return false;
        
        for (ModifiedWeapon weapon : inventoryItem) {
            if (weapon.getWeaponType() == weaponType) {
                return true;
            }
        }
        return false;
    }

    // ── Cycling — selección activa ─────────────────────────────────────────

    /**
     * Retorna el arma actualmente equipada.
     *
     * @return arma activa, o null si el inventario está vacío
     */
    public ModifiedWeapon getCurrentWeapon() {
        if (inventoryItem.isEmpty()) return null;
        return inventoryItem.get(currentIndex);
    }

    /**
     * Avanza al siguiente arma en el ciclo circular.
     * Emite OnWeaponSwitch si el arma cambia.
     */
    public void nextWeapon() {
        if (inventoryItem.size() <= 1) return;
        ModifiedWeapon previous = getCurrentWeapon();
        currentIndex = (currentIndex + 1) % inventoryItem.size();
        emitSwitch(previous, getCurrentWeapon());
    }

    /**
     * Retrocede al arma anterior en el ciclo circular.
     * Emite OnWeaponSwitch si el arma cambia.
     */
    public void previousWeapon() {
        if (inventoryItem.size() <= 1) return;
        ModifiedWeapon previous = getCurrentWeapon();
        currentIndex = (currentIndex - 1 + inventoryItem.size()) % inventoryItem.size();
        emitSwitch(previous, getCurrentWeapon());
    }

    /**
     * Selecciona el arma en el índice indicado directamente.
     * Emite OnWeaponSwitch si el arma cambia.
     */
    public void selectAt(int index) {
        if (index < 0 || index >= inventoryItem.size())
            throw new IndexOutOfBoundsException("índice fuera de rango: " + index);
        if (index == currentIndex) return;
        ModifiedWeapon previous = getCurrentWeapon();
        currentIndex = index;
        emitSwitch(previous, getCurrentWeapon());
    }

    /**
     * Índice del arma actualmente seleccionada.
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    // ── Override clear ────────────────────────────────────────────────────

    public void clearWeaponsInventory() {
        clear();
        currentIndex = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void clampIndex() {
        if (inventoryItem.isEmpty()) {
            currentIndex = 0;
        } else if (currentIndex >= inventoryItem.size()) {
            currentIndex = inventoryItem.size() - 1;
        }
    }

    private void emitSwitch(ModifiedWeapon previous, ModifiedWeapon current) {
        if (eventBus == null) return;
        if (eventBus.hasListeners(WeaponEvents.OnWeaponSwitch.class)) {
            eventBus.post(new WeaponEvents.OnWeaponSwitch(previous, current));
        }
    }
}