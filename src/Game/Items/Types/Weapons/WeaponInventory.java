package Game.Items.Types.Weapons;

import Game.Engine.Events.GameEventBus;
import Game.Gameplay.Events.WeaponEvents;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventario de armas del jugador.
 *
 * ── HRFC — Weapon & Projectile System ────────────────────────────────────
 *
 * Refactorizado para trabajar directamente con {@link ModifiedWeapon},
 * eliminando la capa de indirección de WeaponSelected que no añadía valor.
 *
 * Responsabilidades:
 *   - Mantener la lista de armas equipadas en la run actual.
 *   - Gestionar el índice de arma activa.
 *   - Emitir OnWeaponSwitch al cambiar de arma (suscriptores opcionales).
 *
 * Nunca null: getCurrentWeapon() retorna null solo si el inventario está vacío.
 */
public class WeaponInventory {

    private final List<ModifiedWeapon> weapons = new ArrayList<>();
    private int currentIndex = 0;

    /** Bus de eventos para emitir OnWeaponSwitch. */
    private final GameEventBus eventBus;

    /**
     * @param eventBus bus de eventos para emitir OnWeaponSwitch al cambiar de arma.
     */
    public WeaponInventory(GameEventBus eventBus) {
        if (eventBus == null) throw new IllegalArgumentException("WeaponInventory: eventBus is required");
        this.eventBus = eventBus;
    }

    // ── Gestión de armas ──────────────────────────────────────────────────

    /**
     * Añade un arma al inventario.
     * Si es la primera arma añadida, pasa a ser el arma activa automáticamente.
     */
    public void addWeapon(ModifiedWeapon weapon) {
        if (weapon == null) throw new IllegalArgumentException("weapon no puede ser null");
        weapons.add(weapon);
    }

    /**
     * Retorna el arma actualmente equipada.
     * Retorna null si el inventario está vacío.
     */
    public ModifiedWeapon getCurrentWeapon() {
        if (weapons.isEmpty()) return null;
        return weapons.get(currentIndex);
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

    // ── Consultas ─────────────────────────────────────────────────────────

    /** Número total de armas en el inventario. */
    public int size() { return weapons.size(); }

    /** True si no hay ninguna arma. */
    public boolean isEmpty() { return weapons.isEmpty(); }

    /** Vista no modificable de la lista de armas (para UI de inventario). */
    public List<ModifiedWeapon> getAll() {
        return Collections.unmodifiableList(weapons);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void emitSwitch(ModifiedWeapon previous, ModifiedWeapon current) {
        if (eventBus.hasListeners(WeaponEvents.OnWeaponSwitch.class)) {
            eventBus.post(new WeaponEvents.OnWeaponSwitch(previous, current));
        }
    }
}
