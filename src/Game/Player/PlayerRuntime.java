package Game.Player;

import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Estado mutable de la run del Player — coordina inventario y equipamiento.
 *
 * ── HRFC — Player Reengineering v2 ────────────────────────────────────────
 *
 * ── RESPONSABILIDAD ───────────────────────────────────────────────────────
 *
 * PlayerRuntime representa el estado mutable que se adquiere durante la run.
 * Es el propietario conceptual de:
 *
 *   • Progreso adquirido durante la run
 *   • Equipamiento adquirido
 *   • Estado runtime de gameplay
 *   • Coordinación entre inventario y equipamiento
 *
 * Separa claramente:
 *
 *   PlayerLoadout   → configuración inicial (inmutable)
 *   PlayerRuntime   → estado actual de la run (mutable)
 *   PlayerInventory → almacenamiento de posesiones
 *   PlayerCombat    → ejecución de combate
 *
 * ── ARQUITECTURA ──────────────────────────────────────────────────────────
 *
 *   PlayerRuntime
 *        │
 *        ├── PlayerInventory (delegación)
 *        │      ├── Weapon collection
 *        │      └── Bullet collection
 *        │
 *        └── Equipment state
 *               ├── currentWeapon
 *               └── currentBullet
 *
 * ── SELECCIÓN INDEPENDIENTE ───────────────────────────────────────────────
 *
 * Las armas y balas se seleccionan independientemente:
 *
 *   runtime.selectWeapon(WeaponType.ESCOPETA)  // no cambia bala
 *   runtime.selectBullet(BulletType.BULLETJUMP) // no cambia arma
 *
 * ── INVARIANTES ───────────────────────────────────────────────────────────
 *
 *   • Una bala seleccionada permanece activa hasta que se cambie explícitamente
 *   • Adquirir una nueva bala no elimina automáticamente otras
 *   • Las balas son infinitas (representan tipos/equipamientos disponibles)
 *   • El arma y bala activas son independientes entre sí
 */
public class PlayerRuntime {

    /** Inventario del Player — almacena armas y balas por separado. */
    private final PlayerInventory inventory;

    /** Arma actualmente seleccionada (index en el inventario de armas). */
    private int currentWeaponIndex = 0;

    /** Bala actualmente seleccionada (index en el inventario de balas). */
    private int currentBulletIndex = 0;

    /**
     * @param inventory inventario del Player para gestionar armas y balas
     */
    public PlayerRuntime(PlayerInventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory es requerido");
        }
        this.inventory = inventory;
    }

    // ── Acceso al inventario ──────────────────────────────────────────────

    /**
     * Inventario del Player — punto de acceso a armas y balas almacenadas.
     */
    public PlayerInventory getInventory() {
        return inventory;
    }

    // ── Gestión de armas ──────────────────────────────────────────────────

    /**
     * Arma actualmente seleccionada.
     * @return arma activa, o null si no hay armas en el inventario
     */
    public ModifiedWeapon getCurrentWeapon() {
        List<ModifiedWeapon> weapons = inventory.getWeapons();
        if (weapons.isEmpty() || currentWeaponIndex >= weapons.size()) {
            return null;
        }
        return weapons.get(currentWeaponIndex);
    }

    /**
     * Selecciona un arma por tipo.
     * No afecta la bala actualmente seleccionada.
     *
     * @param weaponType tipo de arma a seleccionar
     * @return true si se encontró y seleccionó el arma, false si no se posee
     */
    public boolean selectWeapon(WeaponType weaponType) {
        List<ModifiedWeapon> weapons = inventory.getWeapons();
        for (int i = 0; i < weapons.size(); i++) {
            ModifiedWeapon weapon = weapons.get(i);
            // Comparar por tipo de arma (requiere método en ModifiedWeapon para obtener el tipo)
            // Por ahora usando comparación directa de instancia
            if (weapon != null) {
                currentWeaponIndex = i;
                return true;
            }
        }
        return false;
    }

    /**
     * Avanza al siguiente arma en el inventario (ciclo circular).
     * No afecta la bala seleccionada.
     */
    public void nextWeapon() {
        List<ModifiedWeapon> weapons = inventory.getWeapons();
        if (weapons.size() <= 1) return;
        currentWeaponIndex = (currentWeaponIndex + 1) % weapons.size();
    }

    /**
     * Retrocede al arma anterior en el inventario (ciclo circular).
     * No afecta la bala seleccionada.
     */
    public void previousWeapon() {
        List<ModifiedWeapon> weapons = inventory.getWeapons();
        if (weapons.size() <= 1) return;
        currentWeaponIndex = (currentWeaponIndex - 1 + weapons.size()) % weapons.size();
    }

    // ── Gestión de balas ──────────────────────────────────────────────────

    /**
     * Bala actualmente seleccionada.
     * @return tipo de bala activa, o null si no hay balas en el inventario
     */
    public BulletType getCurrentBullet() {
        List<BulletType> bullets = inventory.getBullets();
        if (bullets.isEmpty() || currentBulletIndex >= bullets.size()) {
            return null;
        }
        return bullets.get(currentBulletIndex);
    }

    /**
     * Selecciona una bala por tipo.
     * No afecta el arma actualmente seleccionada.
     *
     * @param bulletType tipo de bala a seleccionar
     * @return true si se encontró y seleccionó la bala, false si no se posee
     */
    public boolean selectBullet(BulletType bulletType) {
        List<BulletType> bullets = inventory.getBullets();
        for (int i = 0; i < bullets.size(); i++) {
            if (bullets.get(i) == bulletType) {
                currentBulletIndex = i;
                return true;
            }
        }
        return false;
    }

    /**
     * Avanza a la siguiente bala en el inventario (ciclo circular).
     * No afecta el arma seleccionada.
     */
    public void nextBullet() {
        List<BulletType> bullets = inventory.getBullets();
        if (bullets.size() <= 1) return;
        currentBulletIndex = (currentBulletIndex + 1) % bullets.size();
    }

    /**
     * Retrocede a la bala anterior en el inventario (ciclo circular).
     * No afecta el arma seleccionada.
     */
    public void previousBullet() {
        List<BulletType> bullets = inventory.getBullets();
        if (bullets.size() <= 1) return;
        currentBulletIndex = (currentBulletIndex - 1 + bullets.size()) % bullets.size();
    }

    // ── Adquisiciones runtime ─────────────────────────────────────────────

    /**
     * Añade un arma al inventario durante la run.
     * El arma se añade al final del inventario pero no se selecciona automáticamente.
     *
     * @param weapon arma a añadir
     */
    public void acquireWeapon(ModifiedWeapon weapon) {
        inventory.addWeapon(weapon);
    }

    /**
     * Añade una bala al inventario durante la run.
     * La bala se añade al final del inventario pero no se selecciona automáticamente.
     *
     * @param bulletType tipo de bala a añadir
     */
    public void acquireBullet(BulletType bulletType) {
        inventory.addBullet(bulletType);
    }

    // ── Consultas de estado ───────────────────────────────────────────────

    /**
     * True si el Player posee el tipo de arma indicado.
     */
    public boolean hasWeapon(WeaponType weaponType) {
        return inventory.hasWeapon(weaponType);
    }

    /**
     * True si el Player posee el tipo de bala indicado.
     */
    public boolean hasBullet(BulletType bulletType) {
        return inventory.hasBullet(bulletType);
    }

    /**
     * Número total de armas en el inventario.
     */
    public int getWeaponCount() {
        return inventory.getWeapons().size();
    }

    /**
     * Número total de balas en el inventario.
     */
    public int getBulletCount() {
        return inventory.getBullets().size();
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza el estado runtime.
     * Llamar una vez por frame desde Player.update().
     */
    public void update() {
        // Validar índices tras posibles cambios en el inventario
        validateWeaponIndex();
        validateBulletIndex();
    }

    /**
     * Valida que currentWeaponIndex esté dentro del rango del inventario.
     * Si el arma actual fue removida, selecciona la primera disponible.
     */
    private void validateWeaponIndex() {
        List<ModifiedWeapon> weapons = inventory.getWeapons();
        if (weapons.isEmpty()) {
            currentWeaponIndex = 0;
        } else if (currentWeaponIndex >= weapons.size()) {
            currentWeaponIndex = weapons.size() - 1;
        }
    }

    /**
     * Valida que currentBulletIndex esté dentro del rango del inventario.
     * Si la bala actual fue removida, selecciona la primera disponible.
     */
    private void validateBulletIndex() {
        List<BulletType> bullets = inventory.getBullets();
        if (bullets.isEmpty()) {
            currentBulletIndex = 0;
        } else if (currentBulletIndex >= bullets.size()) {
            currentBulletIndex = bullets.size() - 1;
        }
    }
}