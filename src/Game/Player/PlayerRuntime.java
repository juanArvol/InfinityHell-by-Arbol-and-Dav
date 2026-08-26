package Game.Player;

import Game.Items.Types.Ammulets.AmuletType;
import Game.Items.Types.Bullets.Definition.BulletType;
import Game.Items.Types.Weapons.ModifiedWeapon;
import Game.Items.Types.Weapons.WeaponType.WeaponType;

/**
 * Estado mutable de la run del Player.
 *
 * ── HRFC — Player Architecture Consolidation ──────────────────────────────
 *
 * PlayerRuntime representa el estado mutable adquirido durante la run.
 *
 * RESPONSABILIDADES:
 *
 *   • Progreso adquirido durante la run
 *   • Equipamiento adquirido
 *   • Coordinación entre inventario y gameplay
 *   • Acceso al estado actualmente equipado
 *
 * La selección NO pertenece a PlayerRuntime.
 *
 * La selección es responsabilidad de cada inventario seleccionable:
 *
 *   WeaponInventory
 *       └── SelectableInventory<ModifiedWeapon>
 *
 *   BulletInventory
 *       └── SelectableInventory<BulletType>
 *
 * Los amuletos no son seleccionables:
 *
 *   AmuletInventory
 *       └── Inventory<String>
 *
 * ── OWNERSHIP VS EQUIPMENT ───────────────────────────────────────────────
 *
 * PlayerInventory responde:
 *
 *   "¿Qué posee el Player?"
 *
 * PlayerRuntime responde:
 *
 *   "¿Qué estado runtime tiene el Player?"
 *
 * La selección concreta pertenece al inventario correspondiente.
 *
 * ── SELECCIÓN INDEPENDIENTE ───────────────────────────────────────────────
 *
 * Las armas y balas mantienen selecciones independientes:
 *
 *   WeaponInventory.current
 *          ≠
 *   BulletInventory.current
 *
 * Por lo tanto:
 *
 *   selectWeapon(...)
 *       → solamente modifica WeaponInventory
 *
 *   selectBullet(...)
 *       → solamente modifica BulletInventory
 *
 * ── AMULETOS ──────────────────────────────────────────────────────────────
 *
 * Los amuletos solamente representan posesión.
 *
 * No tienen selección mediante SelectableInventory.
 *
 * Su aplicación ocurre mediante el sistema de amuletos durante el disparo.
 */
public final class PlayerRuntime {

    /**
     * Inventario del Player.
     *
     * Contiene las posesiones runtime de:
     *
     *   • armas
     *   • balas
     *   • amuletos
     */
    private final PlayerInventory inventory;

    /**
     * Crea el estado runtime del Player.
     *
     * @param inventory inventario del Player
     */
    public PlayerRuntime(PlayerInventory inventory) {

        if (inventory == null) {
            throw new IllegalArgumentException(
                    "inventory es requerido"
            );
        }

        this.inventory = inventory;
    }

    // ── Acceso al inventario ──────────────────────────────────────────────

    /**
     * Obtiene el inventario runtime del Player.
     *
     * @return inventario del Player
     */
    public PlayerInventory getInventory() {
        return inventory;
    }

    // ── Armas ──────────────────────────────────────────────────────────────

    /**
     * Obtiene el arma actualmente seleccionada.
     *
     * La selección es propiedad de WeaponInventory.
     *
     * @return arma seleccionada, o null si no hay armas
     */
    public ModifiedWeapon getCurrentWeapon() {
        return inventory.weapons().getCurrentWeapon();
    }

    /**
     * Selecciona un arma por su WeaponType.
     *
     * La selección se delega al WeaponInventory.
     *
     * No modifica la selección de bala.
     *
     * @param weaponType tipo de arma
     * @return true si el arma existe y fue seleccionada
     */
    public boolean selectWeapon(ModifiedWeapon weapon) {

        if (weapon == null) {
            return false;
        }

        int index = inventory.weapons().indexOf(weapon);

        if (index < 0) {
            return false;
        }

        inventory.weapons().selectWeaponAt(index);

        return true;
    }

    /**
     * Avanza al siguiente arma.
     *
     * La selección es gestionada por WeaponInventory.
     */
    public void nextWeapon() {
        inventory.weapons().nextWeapon();
    }

    /**
     * Retrocede al arma anterior.
     *
     * La selección es gestionada por WeaponInventory.
     */
    public void previousWeapon() {
        inventory.weapons().previousWeapon();
    }

    /**
     * Obtiene el índice del arma actualmente seleccionada.
     *
     * Este valor pertenece al WeaponInventory.
     */
    public int getCurrentWeaponIndex() {
        return inventory.weapons().getCurrentWeaponIndex();
    }

    // ── Balas ─────────────────────────────────────────────────────────────

    /**
     * Obtiene el tipo de bala actualmente seleccionado.
     *
     * @return bala seleccionada, o null si no hay balas
     */
    public BulletType getCurrentBullet() {
        return inventory.bullets().getCurrentBullet();
    }

    /**
     * Selecciona una bala por tipo.
     *
     * La selección es propiedad de BulletInventory.
     *
     * No modifica la selección del arma.
     *
     * @param bulletType tipo de bala
     * @return true si la bala existe y fue seleccionada
     */
    public boolean selectBullet(BulletType bulletType) {

        if (bulletType == null) {
            return false;
        }

        int index =
                inventory.bullets().indexOf(bulletType);

        if (index < 0) {
            return false;
        }

        inventory.bullets().selectBulletAt(index);
        return true;
    }

    /**
     * Avanza a la siguiente bala.
     *
     * La selección es gestionada por BulletInventory.
     */
    public void nextBullet() {
        inventory.bullets().nextBullet();
    }

    /**
     * Retrocede a la bala anterior.
     *
     * La selección es gestionada por BulletInventory.
     */
    public void previousBullet() {
        inventory.bullets().previousBullet();
    }

    /**
     * Obtiene el índice de la bala actualmente seleccionada.
     *
     * Este valor pertenece al BulletInventory.
     */
    public int getCurrentBulletIndex() {
        return inventory.bullets().getCurrentBulletIndex();
    }

    // ── Adquisiciones runtime ─────────────────────────────────────────────

    /**
     * Añade un arma al inventario durante la run.
     *
     * La adquisición no fuerza una selección explícita.
     *
     * @param weapon arma a añadir
     * @return true si fue añadida
     */
    public boolean acquireWeapon(ModifiedWeapon weapon) {
        return inventory.addWeapon(weapon);
    }

    /**
     * Añade una bala al inventario durante la run.
     *
     * La adquisición no cambia explícitamente la selección.
     *
     * @param bulletType tipo de bala a añadir
     * @return true si fue añadida
     */
    public boolean acquireBullet(BulletType bulletType) {
        return inventory.addBullet(bulletType);
    }

    /**
     * Añade un amuleto al inventario durante la run.
     *
     * Los amuletos no son seleccionables.
     *
     * @param amuletId ID del amuleto
     * @return true si fue añadido
     */
    public boolean acquireAmulet(AmuletType amulet) {
        return inventory.addAmulet(amulet);
    }

    // ── Consultas de posesión ─────────────────────────────────────────────

    /**
     * Determina si el Player posee un tipo de arma.
     */
    public boolean hasWeapon(WeaponType weaponType) {
        return inventory.hasWeapon(weaponType);
    }

    /**
     * Determina si el Player posee un tipo de bala.
     */
    public boolean hasBullet(BulletType bulletType) {
        return inventory.hasBullet(bulletType);
    }

    // ── Consultas de cantidad ─────────────────────────────────────────────

    /**
     * Número total de armas poseídas.
     */
    public int getWeaponCount() {
        return inventory.getWeaponCount();
    }

    /**
     * Número total de balas poseídas.
     */
    public int getBulletCount() {
        return inventory.getBulletCount();
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Actualiza el estado runtime del Player.
     *
     * La validación de selección ya no se realiza aquí.
     *
     * Cada SelectableInventory mantiene internamente su propia selección
     * válida cuando se modifican sus elementos.
     */
    public void update() {
        // Actualmente no requiere lógica adicional.
    }
}