package Game.Items.Types.Weapons;

import Game.Engine.GameEventBus;
import Game.Gameplay.Events.WeaponEvents;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventario de armas — autoridad del dominio Weapons.
 *
 * ── HRFC — Player Inventory & Domain Ownership Consolidation ──────────────
 *
 * ── OWNERSHIP ─────────────────────────────────────────────────────────────
 *
 * WeaponInventory pertenece al dominio Weapons y responde la pregunta:
 *   "¿Qué armas posee el portador?"
 *
 * PlayerRuntime responde: "¿Qué arma está equipada actualmente?"
 * PlayerCombat  responde: "¿Cómo se ejecuta el combate?"
 *
 * ── RESPONSABILIDADES ─────────────────────────────────────────────────────
 *
 *   • Almacenar armas poseídas (ModifiedWeapon instances)
 *   • Gestionar adquisición y remoción de armas
 *   • Emitir OnWeaponSwitch cuando el arma activa cambia
 *   • Mantener el índice de arma activa
 *   • Exponer la API de consulta de armas
 *
 * ── SEPARACIÓN DE RESPONSABILIDADES ──────────────────────────────────────
 *
 *   WeaponInventory  → almacenamiento + selección activa
 *   PlayerRuntime    → coordina inventory y estado equipado
 *   PlayerCombat     → ejecución del pipeline de disparo
 *
 * ── UNICIDAD ──────────────────────────────────────────────────────────────
 *
 * Las armas se obtienen una sola vez por partida. WeaponInventory NO
 * implementa deduplicación defensiva — la garantía de que una recompensa
 * válida no produzca duplicados pertenece al sistema de adquisición.
 *
 * ── CYCLING ───────────────────────────────────────────────────────────────
 *
 * El cycling (next/prev/current) es responsabilidad de este inventario.
 * PlayerRuntime puede mantener un índice propio o delegar directamente
 * en los métodos de ciclo de este inventario.
 */
public class WeaponInventory {

    private final List<ModifiedWeapon> weapons = new ArrayList<>();
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
     * Útil en testing o cuando no se requiere reactividad.
     */
    public WeaponInventory() {
        this.eventBus = null;
    }

    // ── Gestión de armas ──────────────────────────────────────────────────

    /**
     * Añade un arma al inventario.
     * Si es la primera arma añadida, pasa a ser el arma activa automáticamente.
     *
     * @param weapon arma a añadir. No puede ser null.
     * @throws IllegalArgumentException si weapon es null
     */
    public void addWeapon(ModifiedWeapon weapon) {
        if (weapon == null) throw new IllegalArgumentException("weapon no puede ser null");
        weapons.add(weapon);
    }

    /**
     * Elimina un arma específica del inventario.
     * Si era el arma activa, currentIndex se ajusta al rango válido.
     *
     * @param weapon arma a eliminar
     * @return true si se eliminó, false si no se encontró
     */
    public boolean removeWeapon(ModifiedWeapon weapon) {
        boolean removed = weapons.remove(weapon);
        if (removed) clampIndex();
        return removed;
    }

    /**
     * Elimina el arma en el índice especificado.
     *
     * @param index índice del arma a eliminar (0-based)
     * @return el arma eliminada
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public ModifiedWeapon removeWeaponAt(int index) {
        ModifiedWeapon removed = weapons.remove(index);
        clampIndex();
        return removed;
    }

    /**
     * True si el portador posee un arma del tipo indicado.
     *
     * @param weaponType tipo de arma a verificar
     * @return true si se posee al menos una arma de ese tipo
     */
    public boolean hasWeapon(WeaponType weaponType) {
        // TODO: Implementar cuando ModifiedWeapon exponga WeaponType
        // Por ahora retornar false como placeholder para mantener compatibilidad
        return false;
    }

    // ── Cycling — selección activa ─────────────────────────────────────────

    /**
     * Retorna el arma actualmente equipada.
     *
     * @return arma activa, o null si el inventario está vacío
     */
    public ModifiedWeapon getCurrentWeapon() {
        if (weapons.isEmpty()) return null;
        return weapons.get(currentIndex);
    }

    /**
     * Obtiene un arma por su índice en el inventario.
     *
     * @param index índice del arma (0-based)
     * @return el arma en el índice especificado
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public ModifiedWeapon getWeapon(int index) {
        return weapons.get(index);
    }

    /**
     * Avanza al siguiente arma en el ciclo circular.
     * Emite {@link WeaponEvents.OnWeaponSwitch} si el arma cambia.
     */
    public void nextWeapon() {
        if (weapons.size() <= 1) return;
        ModifiedWeapon previous = getCurrentWeapon();
        currentIndex = (currentIndex + 1) % weapons.size();
        emitSwitch(previous, getCurrentWeapon());
    }

    /**
     * Retrocede al arma anterior en el ciclo circular.
     * Emite {@link WeaponEvents.OnWeaponSwitch} si el arma cambia.
     */
    public void previousWeapon() {
        if (weapons.size() <= 1) return;
        ModifiedWeapon previous = getCurrentWeapon();
        currentIndex = (currentIndex - 1 + weapons.size()) % weapons.size();
        emitSwitch(previous, getCurrentWeapon());
    }

    /**
     * Selecciona el arma en el índice indicado directamente.
     * Emite {@link WeaponEvents.OnWeaponSwitch} si el arma cambia.
     *
     * @param index índice a seleccionar (0-based)
     * @throws IndexOutOfBoundsException si el índice es inválido
     */
    public void selectAt(int index) {
        if (index < 0 || index >= weapons.size())
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

    // ── Consultas ─────────────────────────────────────────────────────────

    /**
     * Lista inmutable de armas poseídas (en orden de adquisición).
     *
     * @return lista de armas. Nunca null, puede estar vacía.
     */
    public List<ModifiedWeapon> getAll() {
        return Collections.unmodifiableList(weapons);
    }

    /** True si no se poseen armas. */
    public boolean isEmpty() {
        return weapons.isEmpty();
    }

    /** Número total de armas poseídas. */
    public int size() {
        return weapons.size();
    }

    // ── Limpieza ──────────────────────────────────────────────────────────

    /**
     * Limpia el inventario — útil para testing o reinicios de run.
     */
    public void clear() {
        weapons.clear();
        currentIndex = 0;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void clampIndex() {
        if (weapons.isEmpty()) {
            currentIndex = 0;
        } else if (currentIndex >= weapons.size()) {
            currentIndex = weapons.size() - 1;
        }
    }

    private void emitSwitch(ModifiedWeapon previous, ModifiedWeapon current) {
        if (eventBus == null) return;
        if (eventBus.hasListeners(WeaponEvents.OnWeaponSwitch.class)) {
            eventBus.post(new WeaponEvents.OnWeaponSwitch(previous, current));
        }
    }
}